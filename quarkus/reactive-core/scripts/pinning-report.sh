#!/usr/bin/env bash
# Builds the pinning audit table from the surefire reports of every PinningTest. Run the tests first.
set -euo pipefail
DEMO=$(cd "$(dirname "$0")/.." && pwd)

echo "| Module | Test | Outcome |"
echo "|---|---|---|"
find "$DEMO" -path '*/target/surefire-reports/TEST-*PinningTest.xml' | sort | while read -r report; do
    module=$(realpath --relative-to="$DEMO" "$report" | cut -d/ -f1-2)
    python3 -I - "$report" "$module" <<'PY'
import sys, xml.etree.ElementTree as ET
report, module = sys.argv[1], sys.argv[2]
for case in ET.parse(report).getroot().iter("testcase"):
    name = case.get("name")
    if name == "warmUp":
        continue
    failed = case.find("failure") is not None or case.find("error") is not None
    print(f"| `{module}` | {name} | {'failed' if failed else 'passed'} |")
PY
done
