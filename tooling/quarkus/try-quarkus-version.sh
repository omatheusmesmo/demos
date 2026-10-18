#!/usr/bin/env bash
# Runs a Maven Quarkus project on its own Quarkus version and on another one, then lists the tests whose outcome
# changed. Useful before an upgrade: the test suite says what the new version breaks or fixes.
#
# Usage: try-quarkus-version.sh <project-dir> <quarkus-version> [extra maven args...]
set -uo pipefail
PROJECT=$(cd "$1" && pwd) VERSION=$2
shift 2
WORK=$(mktemp -d)

collect() {
    python3 -I - "$PROJECT" <<'PARSE' | sort > "$1"
import pathlib, sys, xml.etree.ElementTree as ET
for report in pathlib.Path(sys.argv[1]).glob("**/target/*-reports/TEST-*.xml"):
    for case in ET.parse(report).getroot().iter("testcase"):
        if case.find("failure") is not None or case.find("error") is not None:
            outcome = "fail"
        elif case.find("skipped") is not None:
            outcome = "skip"
        else:
            outcome = "pass"
        print(case.get("classname") + "." + case.get("name") + "	" + outcome)
PARSE
}

run_suite() {
    local label=$1
    shift
    (cd "$PROJECT" && ./mvnw -B -q -fae verify -DskipITs=false "$@" > "$WORK/maven-$label.log" 2>&1)
}

require_results() {
    if [ ! -s "$WORK/$1.tsv" ]; then
        echo "No test results for $1; Maven failed before running tests:" >&2
        grep -m 5 "^\[ERROR\]" "$WORK/maven-$1.log" >&2
        exit 1
    fi
}

echo "baseline: current version"
(cd "$PROJECT" && ./mvnw -B -q clean > /dev/null 2>&1)
run_suite baseline "$@"; collect "$WORK/baseline.tsv"; require_results baseline

echo "candidate: Quarkus $VERSION"
(cd "$PROJECT" && ./mvnw -B -q clean > /dev/null 2>&1)
run_suite candidate "-Dquarkus.platform.version=$VERSION" "$@"; collect "$WORK/candidate.tsv"
[ -s "$WORK/candidate.tsv" ] || echo "No test results on $VERSION: the build failed before running tests (see compilation errors below)."

echo
echo "| Test | Baseline | Quarkus $VERSION |"
echo "|---|---|---|"
join -t $'\t' -a 1 -a 2 -e missing -o 0,1.2,2.2 "$WORK/baseline.tsv" "$WORK/candidate.tsv" \
    | awk -F'\t' '$2 != $3 { printf "| %s | %s | %s |\n", $1, $2, $3 }'
grep -q "COMPILATION ERROR" "$WORK/maven-candidate.log" && echo && echo "Compilation errors on $VERSION:" \
    && grep -A2 "COMPILATION ERROR" "$WORK/maven-candidate.log" | grep "\.java" | sort -u
echo
echo "Maven logs: $WORK"
