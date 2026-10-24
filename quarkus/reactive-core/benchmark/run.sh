#!/usr/bin/env bash
# Benchmarks the three models with Hyperfoil in an open load model, following the Quarkus performance guide (see
# tooling/bench/run.sh), and writes a Markdown summary under benchmark/results.
#
# Scenarios, each swept over arrival rates chosen around the capacity Little's law predicts:
#   fan-out            two calls to a 100 ms upstream with the default REST Client pool (50 connections)
#   fan-out-wide-pool  the same with a pool of 1000 connections, so the pool stops being the limit
#   database           one 200 ms query with the default pools (50 JDBC connections, 20 reactive connections)
#   cpu                a fixed amount of SHA-256 hashing, no I/O (about 1600 requests per second on two CPUs)
# SCENARIOS selects which ones run, QUARKUS_VERSION which Quarkus builds the images (3.40.1 by default), and
# <SCENARIO>_RATES (for example CPU_RATES) overrides a sweep, and STAMP (today by default) names the results, so a
# session interrupted on one day can be completed on another. The variables of tooling/bench/run.sh pass through.
set -euo pipefail
DEMO=$(cd "$(dirname "$0")/.." && pwd)
BENCH=$DEMO/benchmark
TOOLING=$(cd "$DEMO/../../tooling/bench" && pwd)
VERSION=${QUARKUS_VERSION:-3.40.1}
STAMP=${STAMP:-$(date +%Y-%m-%d)}
RAW=$BENCH/results/raw/$STAMP-quarkus-$VERSION
SUMMARY=$BENCH/results/$STAMP-quarkus-$VERSION.md
DB_ENV="-e QUARKUS_DATASOURCE_USERNAME=quarkus -e QUARKUS_DATASOURCE_PASSWORD=quarkus \
-e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://postgres:5432/quarkus \
-e QUARKUS_DATASOURCE_REACTIVE_URL=postgresql://postgres:5432/quarkus"
FAN_OUT_RATES=${FAN_OUT_RATES:-100 200 300 400}
FAN_OUT_WIDE_POOL_RATES=${FAN_OUT_WIDE_POOL_RATES:-500 1000 1500 2500 4000}
DATABASE_RATES=${DATABASE_RATES:-50 100 200 300}
CPU_RATES=${CPU_RATES:-500 1000 1500 2000}

(cd "$DEMO" && ./mvnw -B -q clean package -DskipTests -Dquarkus.platform.version="$VERSION" \
    -pl labs/threading,integrations/rest-client,integrations/postgres -am)
for module in labs/threading integrations/rest-client integrations/postgres; do
    docker build -q -f "$DEMO/$module/src/main/docker/Dockerfile.jvm" \
        -t "reactive-core/$(basename "$module"):$VERSION" "$DEMO/$module" >/dev/null
done
docker compose -f "$BENCH/compose.yaml" up -d --wait
trap 'docker compose -f "$BENCH/compose.yaml" down >/dev/null 2>&1' EXIT

scenario() {
    local name=$1 image=$2 base=$3 rates=$4 app_env=$5
    for variant in worker reactive virtual; do
        IMAGE="$image:$VERSION" TARGET_PATH="$base/$variant" RATES="$rates" \
            OUT_DIR="$RAW/$name/$variant" NETWORK=reactive-core-bench APP_ENV="$app_env" "$TOOLING/run.sh"
    done
}

SCENARIOS=${SCENARIOS:-fan-out fan-out-wide-pool database cpu}
for name in $SCENARIOS; do
    case $name in
        fan-out) scenario fan-out reactive-core/rest-client /fan-out "$FAN_OUT_RATES" \
            "-e UPSTREAM_URL=http://upstream:8080" ;;
        fan-out-wide-pool) scenario fan-out-wide-pool reactive-core/rest-client /fan-out "$FAN_OUT_WIDE_POOL_RATES" \
            "-e UPSTREAM_URL=http://upstream:8080 -e QUARKUS_REST_CLIENT_UPSTREAM_CONNECTION_POOL_SIZE=1000" ;;
        database) scenario database reactive-core/postgres /books "$DATABASE_RATES" "$DB_ENV" ;;
        cpu) scenario cpu reactive-core/threading /cpu "$CPU_RATES" "" ;;
        *) echo "unknown scenario: $name" >&2; exit 1 ;;
    esac
done

{
    echo "# Benchmark $STAMP, Quarkus $VERSION"
    echo
    echo "Machine: $(lscpu | sed -n 's/^Model name: *//p'), $(nproc) logical CPUs, $(uname -sr)."
    echo "Application: CPUs ${APP_CPUS:-2,3} (-XX:ActiveProcessorCount to match), ${MEMORY:-1g} memory," \
        "heap ${HEAP:-512m} (Xms = Xmx), ${GC:--XX:+UseParallelGC}."
    echo "Load generator: Hyperfoil ${HYPERFOIL_VERSION:-0.29.3} on CPUs ${LOAD_CPUS:-4,5,10,11}, open model (constant" \
        "arrival rate), ${TIMEOUT_SECONDS:-10} s request timeout. Dependencies on CPUs 0,1,6,7."
    echo "Each variant: ${ITERATIONS:-3} iterations, each a fresh JVM with a ${WARMUP:-2m} warmup and a" \
        "${COOLDOWN:-30} s cooldown, then every rate for ${DURATION:-30s} in ascending order, ${PAUSE:-15} s apart."
    echo
    echo "Values are the median across iterations, with the range in parentheses when they differ. Achieved counts"
    echo "2xx responses per second over the whole phase, drain included. Errors are started requests that did not end"
    echo "in a 2xx (timeouts included). The load generator keeps at most min(rate x timeout, ${MAX_SESSIONS:-5000})"
    echo "requests in flight; Unsent are scheduled requests it could not start because all of them were busy, and past"
    echo "that point latency describes its own queue, not an open stream of requests. CPU is the"
    echo "mean of the application container (200% is both CPUs), RSS the peak resident set of the JVM, GC the pause"
    echo "time inside the measured window. Other CPU is time other host processes took on the application CPUs."
    for name in fan-out fan-out-wide-pool database cpu; do
        [ -d "$RAW/$name" ] || continue
        echo
        echo "## $name"
        echo
        echo "| Variant | Target/s | Achieved/s | p50 ms | p99 ms | Errors % | Unsent | CPU % | RSS MiB | GC ms | Other CPU % |"
        echo "|---|---|---|---|---|---|---|---|---|---|---|"
        for variant in worker reactive virtual; do
            python3 -I "$TOOLING/summarize.py" rows "$variant" "$RAW/$name/$variant"
        done
    done
} > "$SUMMARY"
echo "Summary written to $SUMMARY"
