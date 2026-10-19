#!/usr/bin/env bash
# Runs one k6 script against an application container at several arrival rates, under fixed and isolated resources.
#
# The application, the load generator and the dependencies each get their own CPU set, so they do not compete for
# cores. The garbage collector and the heap are fixed instead of left to JVM ergonomics. While each run lasts, the
# container's CPU and memory are sampled every second and the host load average is recorded, so a noisy run can be
# recognised afterwards.
#
# Required environment:
#   IMAGE        container image of the application under test
#   SCRIPT       k6 script to run (it should import k6-lib.js)
#   TARGET_PATH  path the script calls, appended to the application URL
#   RATES        space-separated arrival rates, in requests per second
#   OUT_DIR      directory that receives, per rate, one k6 summary and one resource sample file per run
# Optional environment:
#   APP_CPUS (2,3)  K6_CPUS (4,5,10,11)  MEMORY (1g)  HEAP (512m)  GC (-XX:+UseG1GC)
#   RUNS (3)  WARMUP (30s)  WARMUP_RATE (first rate)  DURATION (30s)  MAX_VUS (5000)  TIMEOUT (10s)
#   NETWORK   docker network to join, for applications that need other containers
#   APP_ENV   extra "-e KEY=VALUE" flags for the application container
#   PORT      host port for the application (8080)
set -euo pipefail

: "${IMAGE:?IMAGE is required}" "${SCRIPT:?SCRIPT is required}" "${TARGET_PATH:?TARGET_PATH is required}"
: "${RATES:?RATES is required}" "${OUT_DIR:?OUT_DIR is required}"
APP_CPUS=${APP_CPUS:-2,3} K6_CPUS=${K6_CPUS:-4,5,10,11} MEMORY=${MEMORY:-1g} HEAP=${HEAP:-512m}
GC=${GC:--XX:+UseG1GC} RUNS=${RUNS:-3} WARMUP=${WARMUP:-30s} DURATION=${DURATION:-30s}
MAX_VUS=${MAX_VUS:-5000} TIMEOUT=${TIMEOUT:-10s} PORT=${PORT:-8080}
read -r -a RATE_LIST <<< "$RATES"
WARMUP_RATE=${WARMUP_RATE:-${RATE_LIST[0]}}
NETWORK_FLAG=${NETWORK:+--network $NETWORK}
NAME="bench-$(date +%s)-$$"
SAMPLER=

cleanup() {
    [ -n "$SAMPLER" ] && kill "$SAMPLER" 2>/dev/null || true
    docker rm -f "$NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

mkdir -p "$OUT_DIR/gc"
chmod 777 "$OUT_DIR/gc"
# shellcheck disable=SC2086
docker run -d --name "$NAME" --cpuset-cpus "$APP_CPUS" --memory "$MEMORY" -p "$PORT:8080" $NETWORK_FLAG ${APP_ENV:-} \
    -v "$OUT_DIR/gc:/gc" \
    -e GC_CONTAINER_OPTIONS="$GC" \
    -e JAVA_OPTS_APPEND="-Xms$HEAP -Xmx$HEAP -Xlog:gc:file=/gc/gc.log -Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager" \
    "$IMAGE" >/dev/null

for _ in $(seq 1 120); do
    curl -sf -o /dev/null "http://localhost:$PORT$TARGET_PATH" && break
    sleep 1
done
curl -sf -o /dev/null "http://localhost:$PORT$TARGET_PATH" || { docker logs "$NAME" | tail -20; exit 1; }

sample_resources() {
    while true; do
        docker stats --no-stream --format '{{json .}}' "$NAME" 2>/dev/null || true
        sleep 1
    done > "$1"
}

run_k6() {
    BASE_URL="http://localhost:$PORT" TARGET_PATH="$TARGET_PATH" RATE="$1" DURATION="$2" MAX_VUS="$MAX_VUS" \
        TIMEOUT="$TIMEOUT" taskset -c "$K6_CPUS" k6 run --quiet --summary-export "$3" "$SCRIPT" >/dev/null 2>&1 || true
}

echo "warmup $WARMUP at $WARMUP_RATE/s against $TARGET_PATH (discarded)"
run_k6 "$WARMUP_RATE" "$WARMUP" /dev/null
for rate in "${RATE_LIST[@]}"; do
    mkdir -p "$OUT_DIR/rate-$rate"
    for run in $(seq 1 "$RUNS"); do
        echo "rate $rate/s, run $run/$RUNS for $DURATION"
        cut -d' ' -f1 /proc/loadavg > "$OUT_DIR/rate-$rate/run-$run.load-before"
        sample_resources "$OUT_DIR/rate-$rate/run-$run.stats.jsonl" &
        SAMPLER=$!
        run_k6 "$rate" "$DURATION" "$OUT_DIR/rate-$rate/run-$run.json"
        kill "$SAMPLER" 2>/dev/null || true
        wait "$SAMPLER" 2>/dev/null || true
        SAMPLER=
        cut -d' ' -f1 /proc/loadavg > "$OUT_DIR/rate-$rate/run-$run.load-after"
    done
done
