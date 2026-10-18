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
    find "$PROJECT" -path '*/target/*-reports/TEST-*.xml' -print0 | xargs -0 -r python3 -I -c '
import sys, xml.etree.ElementTree as ET
for path in sys.argv[1:]:
    for case in ET.parse(path).getroot().iter("testcase"):
        outcome = "fail" if case.find("failure") is not None or case.find("error") is not None else (
            "skip" if case.find("skipped") is not None else "pass")
        print(f"{case.get(\"classname\")}.{case.get(\"name\")}\t{outcome}")
' "$@" | sort > "$1"
}

run_suite() {
    (cd "$PROJECT" && ./mvnw -B -q -fae verify -DskipITs=false "$@" > "$WORK/maven-$1.log" 2>&1)
}

echo "baseline: current version"
(cd "$PROJECT" && ./mvnw -B -q clean > /dev/null 2>&1)
run_suite baseline "$@"; collect "$WORK/baseline.tsv"

echo "candidate: Quarkus $VERSION"
(cd "$PROJECT" && ./mvnw -B -q clean > /dev/null 2>&1)
run_suite candidate "-Dquarkus.platform.version=$VERSION" "$@"; collect "$WORK/candidate.tsv"

echo
echo "| Test | Baseline | Quarkus $VERSION |"
echo "|---|---|---|"
join -t $'\t' -a 1 -a 2 -e missing -o 0,1.2,2.2 "$WORK/baseline.tsv" "$WORK/candidate.tsv" \
    | awk -F'\t' '$2 != $3 { printf "| %s | %s | %s |\n", $1, $2, $3 }'
grep -q "COMPILATION ERROR" "$WORK/maven-candidate.log" && echo && echo "Compilation errors on $VERSION:" \
    && grep -A2 "COMPILATION ERROR" "$WORK/maven-candidate.log" | grep "\.java" | sort -u
echo
echo "Maven logs: $WORK"
