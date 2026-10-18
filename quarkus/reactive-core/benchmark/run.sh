#!/usr/bin/env bash
# Benchmarks the three models and writes a Markdown summary under benchmark/results. Scenarios: fan-out (HTTP calls
# with default connection pools), fan-out-wide-pool (the REST Client pool raised to 1000, so the worker pool becomes
# the limit), database (pool-limited) and cpu. SCENARIOS selects which ones run; the summary covers every scenario
# measured on the same day. Environment variables of tooling/bench/run.sh (RUNS, WARMUP, DURATION, VUS, CPUS,
# MEMORY, HEAP) are passed through.
set -euo pipefail
DEMO=$(cd "$(dirname "$0")/.." && pwd)
BENCH=$DEMO/benchmark
TOOLING=$(cd "$DEMO/../../tooling/bench" && pwd)
STAMP=$(date +%Y-%m-%d)
RAW=$BENCH/results/raw/$STAMP
SUMMARY=$BENCH/results/$STAMP.md
DB_ENV="-e QUARKUS_DATASOURCE_USERNAME=quarkus -e QUARKUS_DATASOURCE_PASSWORD=quarkus \
-e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://postgres:5432/quarkus \
-e QUARKUS_DATASOURCE_REACTIVE_URL=postgresql://postgres:5432/quarkus"

(cd "$DEMO" && ./mvnw -B -q package -DskipTests -pl labs/threading,integrations/rest-client,integrations/postgres -am)
for module in labs/threading integrations/rest-client integrations/postgres; do
    docker build -q -f "$DEMO/$module/src/main/docker/Dockerfile.jvm" -t "reactive-core/$(basename "$module")" "$DEMO/$module" >/dev/null
done
docker compose -f "$BENCH/compose.yaml" up -d --wait
trap 'docker compose -f "$BENCH/compose.yaml" down >/dev/null 2>&1' EXIT

scenario() {
    local name=$1 image=$2 base=$3 app_env=$4
    for variant in worker reactive virtual; do
        IMAGE=$image SCRIPT=$BENCH/scenario.js TARGET_PATH="$base/$variant" OUT_DIR="$RAW/$name/$variant" \
            NETWORK=reactive-core-bench APP_ENV="$app_env" "$TOOLING/run.sh"
    done
}

SCENARIOS=${SCENARIOS:-fan-out fan-out-wide-pool database cpu}
for name in $SCENARIOS; do
    case $name in
        fan-out) scenario fan-out reactive-core/rest-client /quotes "-e UPSTREAM_URL=http://upstream:8080" ;;
        fan-out-wide-pool) scenario fan-out-wide-pool reactive-core/rest-client /quotes \
            "-e UPSTREAM_URL=http://upstream:8080 -e QUARKUS_REST_CLIENT_UPSTREAM_CONNECTION_POOL_SIZE=1000" ;;
        database) scenario database reactive-core/postgres /books "$DB_ENV" ;;
        cpu) scenario cpu reactive-core/threading /cpu "" ;;
        *) echo "unknown scenario: $name" >&2; exit 1 ;;
    esac
done

{
    echo "# Benchmark $STAMP"
    echo
    echo "Machine: $(nproc) CPUs, $(uname -sr). Containers: ${CPUS:-2} CPUs, ${MEMORY:-512m} memory, heap ${HEAP:-256m}."
    echo "Load: ${VUS:-100} virtual users, ${WARMUP:-30s} warmup discarded, ${RUNS:-5} runs of ${DURATION:-60s}."
    echo "Values are median (min to max) across runs. This is one machine and one workload, not a general benchmark."
    for name in fan-out fan-out-wide-pool database cpu; do
        [ -d "$RAW/$name" ] || continue
        echo
        echo "## $name"
        echo
        echo "| Variant | Requests/s | Median ms | p95 ms | p99 ms | Failed checks |"
        echo "|---|---|---|---|---|---|"
        for variant in worker reactive virtual; do
            "$TOOLING/summarize.sh" "$variant" "$RAW/$name/$variant"
        done
    done
} > "$SUMMARY"
echo "Summary written to $SUMMARY"
