#!/usr/bin/env bash
# Aggregates the k6 summaries in one directory into median and range per metric, as a Markdown table row.
#
# Usage: summarize.sh <label> <dir-with-run-*.json>
# Averages hide outliers, so every value is reported as "median (min to max)" across runs.
set -euo pipefail
LABEL=$1 DIR=$2

jq -s -r --arg label "$LABEL" '
  def stats(f): (map(f) | sort) as $v
    | ($v | length) as $n
    | (if $n % 2 == 1 then $v[($n - 1) / 2] else ($v[$n / 2 - 1] + $v[$n / 2]) / 2 end) as $median
    | "\($median | floor) (\($v[0] | floor) to \($v[-1] | floor))";
  "| \($label) | \(stats(.metrics.http_reqs.rate)) | \(stats(.metrics.http_req_duration.med)) | \(stats(.metrics.http_req_duration["p(95)"])) | \(stats(.metrics.http_req_duration["p(99)"])) | \(map(.metrics.checks.fails // 0) | add) |"
' "$DIR"/run-*.json
