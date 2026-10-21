#!/usr/bin/env bash
# Measures one application endpoint at several arrival rates with Hyperfoil, following the methodology of
# https://quarkus.io/guides/performance-measure and its reference lab, quarkusio/spring-quarkus-perf-comparison:
#
#   - every iteration starts a fresh JVM, so the iterations also capture start-to-start variance (JIT, heap layout)
#   - each JVM gets a warmup (2 minutes) and a cooldown (30 seconds) before anything is measured
#   - the heap is fixed (Xms = Xmx), -XX:ActiveProcessorCount matches the CPU set, and the application, the load
#     generator and the dependencies are pinned to separate CPUs
#   - memory is the RSS of the JVM process read with docker top, never docker stats (see the guide)
#   - Hyperfoil runs an open model (tooling/bench/open-rate.hf.yml), which avoids coordinated omission
#
# Inside each iteration the rates run in ascending order, each one after a pause that lets queued work drain. While a
# rate runs, a sampler records every second the CPU time of the container (from its cgroup), the busy time of the
# application CPUs (from /proc/stat, so time taken by other processes on those CPUs shows up) and the JVM RSS.
#
# Required environment:
#   IMAGE        container image of the application under test
#   TARGET_PATH  path to call
#   RATES        space-separated arrival rates, in requests per second
#   OUT_DIR      directory that receives iteration-N/rate-R/ with all.json (Hyperfoil) and samples.txt (one line per
#                second: epoch ms, container CPU usec, application CPUs busy and total jiffies, JVM RSS kB), plus
#                iteration-N/gc.log
# Optional environment:
#   APP_CPUS (2,3: a comma-separated list)  LOAD_CPUS (4,5,10,11)  MEMORY (1g)  HEAP (512m)  GC (-XX:+UseParallelGC)
#   ITERATIONS (3)  WARMUP (2m)  WARMUP_RATE (first rate)  COOLDOWN (30)  PAUSE (15)  DURATION (30s)
#   TIMEOUT_SECONDS (10)  MAX_SESSIONS (5000)  HYPERFOIL_VERSION (0.29.3)  LOAD_JAVA_HOME (JAVA_HOME)
#   NETWORK   docker network to join, for applications that need other containers
#   APP_ENV   extra "-e KEY=VALUE" flags for the application container
#   PORT      host port for the application (8080)
# An iteration that already finished (iteration-N/done exists) is skipped, so an interrupted session can resume.
set -euo pipefail

: "${IMAGE:?IMAGE is required}" "${TARGET_PATH:?TARGET_PATH is required}" "${RATES:?RATES is required}"
: "${OUT_DIR:?OUT_DIR is required}"
APP_CPUS=${APP_CPUS:-2,3} LOAD_CPUS=${LOAD_CPUS:-4,5,10,11} MEMORY=${MEMORY:-1g} HEAP=${HEAP:-512m}
GC=${GC:--XX:+UseParallelGC} ITERATIONS=${ITERATIONS:-3} WARMUP=${WARMUP:-2m} COOLDOWN=${COOLDOWN:-30}
PAUSE=${PAUSE:-15} DURATION=${DURATION:-30s} TIMEOUT_SECONDS=${TIMEOUT_SECONDS:-10} MAX_SESSIONS=${MAX_SESSIONS:-5000}
HYPERFOIL_VERSION=${HYPERFOIL_VERSION:-0.29.3} LOAD_JAVA_HOME=${LOAD_JAVA_HOME:-${JAVA_HOME:-}} PORT=${PORT:-8080}
read -r -a RATE_LIST <<< "$RATES"
WARMUP_RATE=${WARMUP_RATE:-${RATE_LIST[0]}}
PROCESSORS=$(tr ',' '\n' <<< "$APP_CPUS" | wc -l)
NETWORK_FLAG=${NETWORK:+--network $NETWORK}
BENCHMARK=$(cd "$(dirname "$0")" && pwd)/open-rate.hf.yml
NAME=
SAMPLERS=()

stop_samplers() {
    for pid in "${SAMPLERS[@]}"; do kill "$pid" 2>/dev/null || true; wait "$pid" 2>/dev/null || true; done
    SAMPLERS=()
}

cleanup() {
    stop_samplers
    [ -n "$NAME" ] && docker rm -f "$NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

start_application() {
    NAME="bench-$(date +%s)-$$"
    # shellcheck disable=SC2086
    docker run -d --name "$NAME" --cpuset-cpus "$APP_CPUS" --memory "$MEMORY" -p "$PORT:8080" $NETWORK_FLAG \
        ${APP_ENV:-} -v "$1:/gc" -e GC_CONTAINER_OPTIONS="$GC" \
        -e JAVA_OPTS_APPEND="-XX:ActiveProcessorCount=$PROCESSORS -Xms$HEAP -Xmx$HEAP -Xlog:gc:file=/gc/gc.log:time \
-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager" \
        "$IMAGE" >/dev/null
    for _ in $(seq 1 120); do
        curl -sf -o /dev/null "http://localhost:$PORT$TARGET_PATH" && return
        sleep 1
    done
    docker logs "$NAME" | tail -20
    exit 1
}

stop_application() {
    docker stop -t 30 "$NAME" >/dev/null
    docker rm "$NAME" >/dev/null
    NAME=
}

to_seconds() {
    case $1 in
        *m) echo $(( ${1%m} * 60 )) ;;
        *s) echo "${1%s}" ;;
    esac
}

hyperfoil() {
    local rate=$1 duration=$2 out=$3 sessions=$(( $1 * TIMEOUT_SECONDS ))
    local root
    root=$(mktemp -d)
    (( sessions > MAX_SESSIONS )) && sessions=$MAX_SESSIONS
    (cd "$root" && JAVA_HOME=$LOAD_JAVA_HOME taskset -c "$LOAD_CPUS" jbang -R -Dio.hyperfoil.rootdir="$root" \
        -R -Dio.hyperfoil.cpu.watchdog.idle.threshold=0.0 \
        --deps "io.hyperfoil:hyperfoil-all:$HYPERFOIL_VERSION@pom,io.hyperfoil:hyperfoil-api,io.hyperfoil:hyperfoil-clustering,io.hyperfoil:hyperfoil-controller-api,io.hyperfoil:hyperfoil-core,io.hyperfoil:hyperfoil-http" \
        --main io.hyperfoil.cli.commands.LoadAndRun "io.hyperfoil:hyperfoil-cli:$HYPERFOIL_VERSION" \
        -o "$root" -PPORT="$PORT" -PPATH="$TARGET_PATH" -PRATE="$rate" -PDURATION="$duration" \
        -PMAX_DURATION="$(( $(to_seconds "$duration") + TIMEOUT_SECONDS + 5 ))s" \
        -PTIMEOUT="${TIMEOUT_SECONDS}s" -PSESSIONS="$sessions" -PCONNECTIONS="$sessions" "$BENCHMARK" \
        > "$root/hyperfoil.log" 2>&1) || { tail -20 "$root/hyperfoil.log"; exit 1; }
    [ -n "$out" ] && cp "$root/run/0000/all.json" "$out"
    rm -rf "$root"
}

sample() {
    local cgroup cores
    cgroup=/sys/fs/cgroup$(cut -d: -f3 "/proc/$(docker inspect -f '{{.State.Pid}}' "$NAME")/cgroup")
    cores="^cpu($(tr ',' '|' <<< "$APP_CPUS")) "
    while true; do
        echo "$(date +%s%3N) $(awk '/^usage_usec/ {print $2}' "$cgroup/cpu.stat")" \
            "$(grep -E "$cores" /proc/stat | awk '{b += $2+$3+$4+$7+$8+$9; t += $2+$3+$4+$5+$6+$7+$8+$9} END {print b, t}')" \
            "$(docker top "$NAME" -o pid,rss,args 2>/dev/null | awk '/java/ {print $2}')"
        sleep 1
    done > "$1"
}

for iteration in $(seq 1 "$ITERATIONS"); do
    dir=$OUT_DIR/iteration-$iteration
    [ -f "$dir/done" ] && { echo "iteration $iteration already done, skipped"; continue; }
    rm -rf "$dir"
    mkdir -p "$dir"
    chmod 777 "$dir"
    start_application "$dir"
    echo "iteration $iteration/$ITERATIONS: warmup $WARMUP at $WARMUP_RATE/s against $TARGET_PATH, cooldown ${COOLDOWN}s"
    hyperfoil "$WARMUP_RATE" "$WARMUP" ""
    sleep "$COOLDOWN"
    for rate in "${RATE_LIST[@]}"; do
        echo "iteration $iteration/$ITERATIONS: rate $rate/s for $DURATION"
        mkdir -p "$dir/rate-$rate"
        sample "$dir/rate-$rate/samples.txt" &
        SAMPLERS+=($!)
        hyperfoil "$rate" "$DURATION" "$dir/rate-$rate/all.json"
        stop_samplers
        sleep "$PAUSE"
    done
    stop_application
    touch "$dir/done"
done
