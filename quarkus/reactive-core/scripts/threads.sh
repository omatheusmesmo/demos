#!/usr/bin/env bash
# Calls the worker, reactive and virtual thread variants of a module running in dev mode and prints which thread
# served each one. Usage: threads.sh <module> [base-url]
set -euo pipefail
MODULE=${1:?usage: threads.sh <module> [base-url]}
BASE=${2:-http://localhost:8080}

case "$MODULE" in
    labs/threading) paths="/dispatch/sync /dispatch/uni /dispatch/virtual" ;;
    integrations/rest-client) paths="/quotes/worker /quotes/reactive /quotes/virtual" ;;
    integrations/postgres) paths="/books/worker /books/reactive /books/virtual" ;;
    integrations/redis) paths="/counters/worker /counters/reactive /counters/virtual" ;;
    integrations/scheduler) paths="/jobs/worker /jobs/event-loop /jobs/virtual" ;;
    integrations/kafka) paths="/consumers/blocking /consumers/event-loop /consumers/virtual" ;;
    *) echo "unknown module: $MODULE" >&2; exit 1 ;;
esac

printf '%-24s %-36s %-8s %-10s %s\n' PATH THREAD VIRTUAL EVENTLOOP DUPLICATED
for path in $paths; do
    curl -sf "$BASE$path" | jq -r --arg path "$path" '
        (.thread // .lastThread // (.processed[-1].thread)) as $t
        | if $t == null then "\($path)\t(nothing recorded yet)"
          else "\($path)\t\($t.name)\t\($t.virtual)\t\($t.eventLoop)\t\($t.duplicatedContext)" end' \
        | awk -F'\t' '{ printf "%-24s %-36s %-8s %-10s %s\n", $1, $2, $3, $4, $5 }'
done
