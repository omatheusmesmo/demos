#!/usr/bin/env bash
# Checks one roadmap card against its acceptance criteria, with no AI involved.
#
# Usage: scripts/card.sh MIG-02 [--quick]
#   --quick   only the card's own checks, without the regression gates (unit tests and smoke)
#
# A card's checks live in roadmap/checks/<ID>.sh and define two functions:
#   card_checks   assertions that are specific to the card; they fail on the legacy baseline
#   card_gates    regression gates that must stay green, usually gate_tests and gate_smoke on a JDK
set -uo pipefail
source "$(dirname "$0")/lib.sh"
cd "$DEMO_DIR"

CARD="${1:?usage: scripts/card.sh <card id> [--quick]}"
QUICK=false
[[ "${2:-}" == "--quick" ]] && QUICK=true
CHECKS="roadmap/checks/$CARD.sh"
[[ -f "$CHECKS" ]] || { echo "No checks for $CARD ($CHECKS)"; exit 2; }

FAILED=0
LOG_DIR="target/card/$CARD"
mkdir -p "$LOG_DIR"

pass() { printf '  \033[32mPASS\033[0m %s\n' "$1"; }
fail() { printf '  \033[31mFAIL\033[0m %s\n' "$1"; FAILED=$((FAILED + 1)); }

# expect_absent <description> <extended regex> <paths...>: no file under the paths matches
expect_absent() {
    local description="$1" pattern="$2"
    shift 2
    local hits
    hits=$(grep -rnE "$pattern" "$@" 2>/dev/null | head -5)
    if [[ -z "$hits" ]]; then pass "$description"; else fail "$description"; sed 's/^/         /' <<<"$hits"; fi
}

# expect_present <description> <extended regex> <paths...>: at least one file under the paths matches
expect_present() {
    local description="$1" pattern="$2"
    shift 2
    if grep -rqE "$pattern" "$@" 2>/dev/null; then pass "$description"; else fail "$description"; fi
}

# expect_cmd <description> <command...>: the command succeeds; its output goes to the card log
expect_cmd() {
    local description="$1"
    shift
    local log="$LOG_DIR/$(tr -c 'a-zA-Z0-9' '-' <<<"$description" | cut -c1-60).log"
    if "$@" >"$log" 2>&1; then pass "$description"; else fail "$description (log: $log)"; tail -15 "$log" | sed 's/^/         /'; fi
}

# expect_plugin_at_least <artifactId> <minimum version>: the plugin is declared in pom.xml at that version or newer
expect_plugin_at_least() {
    local plugin="$1" minimum="$2" version
    version=$(xmllint --xpath "string(//*[local-name()='plugin'][*[local-name()='artifactId']='$plugin']/*[local-name()='version'])" pom.xml 2>/dev/null)
    if [[ -n "$version" && "$(printf '%s\n%s\n' "$minimum" "$version" | sort -V | head -1)" == "$minimum" ]]; then
        pass "$plugin is $version (at least $minimum)"
    else
        fail "$plugin is ${version:-undeclared}, needs at least $minimum"
    fi
}

gate_tests() {
    clean_build_outputs
    expect_cmd "unit tests pass on JDK $1" with_jdk "$1" ./mvnw -B verify
}

gate_smoke() {
    expect_cmd "smoke matches the golden files on JDK $1" env JDK="$1" scripts/smoke.sh
}

gate_golden_untouched() {
    expect_cmd "golden files are untouched" bash -c "cd smoke && sha256sum --check --quiet golden.sha256"
}

source "$CHECKS"

echo "$CARD: card checks"
card_checks
if ! $QUICK; then
    echo "$CARD: regression gates"
    gate_golden_untouched
    card_gates
fi

if [[ $FAILED -eq 0 ]]; then
    echo "$CARD: DONE"
else
    echo "$CARD: NOT DONE ($FAILED failing)"
    exit 1
fi
