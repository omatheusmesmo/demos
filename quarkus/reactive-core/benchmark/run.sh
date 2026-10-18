#!/usr/bin/env bash
# Benchmarks the three models in three scenarios (HTTP fan-out, pool-limited database, CPU-bound) and writes a
# Markdown summary under benchmark/results. Environment variables of tooling/bench/run.sh (RUNS, WARMUP, DURATION,
# VUS, CPUS, MEMORY, HEAP) are passed through.
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

scenario fan-out reactive-core/rest-client /quotes "-e UPSTREAM_URL=http://upstream:8080"
scenario database reactive-core/postgres /books "$DB_ENV"
scenario cpu reactive-core/threading /cpu ""

{
    echo "# Benchmark $STAMP"
    echo
    echo "Machine: $(nproc) CPUs, $(uname -sr). Containers: ${CPUS:-2} CPUs, ${MEMORY:-512m} memory, heap ${HEAP:-256m}."
    echo "Load: ${VUS:-100} virtual users, ${WARMUP:-30s} warmup discarded, ${RUNS:-5} runs of ${DURATION:-60s}."
    echo "Values are median (min to max) across runs. This is one machine and one workload, not a general benchmark."
    for name in fan-out database cpu; do
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
