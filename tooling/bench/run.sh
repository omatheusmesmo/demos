#!/usr/bin/env bash
# Runs one k6 scenario against an application container, several times, under fixed resource limits.
#
# Required environment:
#   IMAGE        container image of the application under test
#   SCRIPT       k6 script to run (it should import k6-lib.js)
#   TARGET_PATH  path the script calls, appended to the application URL
#   OUT_DIR      directory that receives one k6 summary JSON per measured run
# Optional environment:
#   CPUS (2)  MEMORY (512m)  HEAP (256m)  RUNS (5)  WARMUP (30s)  DURATION (60s)  VUS (100)
#   NETWORK   docker network to join, for applications that need other containers
#   APP_ENV   extra "-e KEY=VALUE" flags for the application container
#   PORT      host port for the application (8080)
set -euo pipefail

: "${IMAGE:?IMAGE is required}" "${SCRIPT:?SCRIPT is required}" "${TARGET_PATH:?TARGET_PATH is required}" "${OUT_DIR:?OUT_DIR is required}"
CPUS=${CPUS:-2} MEMORY=${MEMORY:-512m} HEAP=${HEAP:-256m} RUNS=${RUNS:-5}
WARMUP=${WARMUP:-30s} DURATION=${DURATION:-60s} VUS=${VUS:-100} PORT=${PORT:-8080}
NETWORK_FLAG=${NETWORK:+--network $NETWORK}
NAME="bench-$(date +%s)-$$"

cleanup() { docker rm -f "$NAME" >/dev/null 2>&1 || true; }
trap cleanup EXIT

mkdir -p "$OUT_DIR"
# shellcheck disable=SC2086
docker run -d --name "$NAME" --cpus "$CPUS" --memory "$MEMORY" -p "$PORT:8080" $NETWORK_FLAG ${APP_ENV:-} \
    -e JAVA_OPTS_APPEND="-Xms$HEAP -Xmx$HEAP -Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager" \
    "$IMAGE" >/dev/null

for _ in $(seq 1 120); do
    curl -sf -o /dev/null "http://localhost:$PORT$TARGET_PATH" && break
    sleep 1
done
curl -sf -o /dev/null "http://localhost:$PORT$TARGET_PATH" || { docker logs "$NAME" | tail -20; exit 1; }

run_k6() {
    BASE_URL="http://localhost:$PORT" TARGET_PATH="$TARGET_PATH" VUS="$VUS" DURATION="$1" \
        k6 run --quiet --summary-export "$2" "$SCRIPT" >/dev/null
}

echo "warmup $WARMUP against $TARGET_PATH (discarded)"
run_k6 "$WARMUP" /dev/null
for run in $(seq 1 "$RUNS"); do
    echo "run $run/$RUNS for $DURATION"
    run_k6 "$DURATION" "$OUT_DIR/run-$run.json"
done
docker stats --no-stream --format '{{json .}}' "$NAME" > "$OUT_DIR/container-stats.json"
