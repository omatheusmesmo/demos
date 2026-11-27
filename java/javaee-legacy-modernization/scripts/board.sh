#!/usr/bin/env bash
# Prints the migration board: every card with its priority and whether its own checks pass right now.
# The status comes from the code (scripts/card.sh --quick), never from a field someone has to remember to update.
set -uo pipefail
source "$(dirname "$0")/lib.sh"
cd "$DEMO_DIR"

field() { sed -n "s/^$2: *//p" "$1" | head -1 | tr -d '"'; }

printf '%-7s %-9s %-8s %-11s %s\n' CARD STATUS PRIO BOBCOINS TITLE
for card in roadmap/MIG-*.md; do
    id=$(field "$card" id)
    if [[ "$(field "$card" priority)" == blocked ]]; then
        status=BLOCKED
    elif scripts/card.sh "$id" --quick >/dev/null 2>&1; then
        status=DONE
    else
        status=TODO
    fi
    printf '%-7s %-9s %-8s %-11s %s\n' "$id" "$status" "$(field "$card" priority)" \
        "$(field "$card" estimate_bobcoins)/$(field "$card" cap_bobcoins)" "$(field "$card" title)"
done
