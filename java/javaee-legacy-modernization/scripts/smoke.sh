#!/usr/bin/env bash
# Starts the application on Open Liberty, drives the API and the JSF page, and compares every response with
# smoke/golden. The golden files were recorded on the legacy baseline (Java 8, Java EE 8): a migration passes
# only if the application still answers exactly the same.
#
# Usage: scripts/smoke.sh [--record]   --record rewrites the golden files instead of comparing.
set -euo pipefail
source "$(dirname "$0")/lib.sh"
cd "$DEMO_DIR"

JDK="${JDK:-$(declared_jdk)}"
PORT="${HTTP_PORT:-9080}"
BASE="http://localhost:$PORT"
GOLDEN="$DEMO_DIR/smoke/golden"
OUT="$DEMO_DIR/target/smoke"
RECORD=false
[[ "${1:-}" == "--record" ]] && RECORD=true

export JAVA_HOME
JAVA_HOME=$(jdk_home "$JDK")
export PATH="$JAVA_HOME/bin:$PATH"
MVN=(./mvnw -B -q -Dhttp.port="$PORT")

stop_server() {
    "${MVN[@]}" liberty:stop >/dev/null 2>&1 || true
}
trap stop_server EXIT

echo "smoke: JDK $JDK ($JAVA_HOME), port $PORT"
rm -rf "$OUT" && mkdir -p "$OUT"
stop_server
clean_build_outputs
"${MVN[@]}" -DskipTests package liberty:create liberty:deploy liberty:start -Dclean=true

for _ in $(seq 1 90); do
    curl -fsS "$BASE/api/customers" >/dev/null 2>&1 && break
    sleep 1
done
curl -fsS "$BASE/api/customers" >/dev/null || {
    echo "smoke: the application did not come up; last lines of messages.log:"
    tail -40 target/liberty/wlp/usr/servers/invoicing/logs/messages.log
    exit 1
}

# call <name> <expected status> <curl args...>: saves the normalized body as $OUT/<name>
call() {
    local name="$1" expected="$2"
    shift 2
    local status
    status=$(curl -sS -o "$OUT/$name.raw" -w '%{http_code}' "$@")
    if [[ "$status" != "$expected" ]]; then
        echo "smoke: $name answered HTTP $status, expected $expected"
        cat "$OUT/$name.raw"
        exit 1
    fi
    case "$name" in
        *.json) jq -S . "$OUT/$name.raw" >"$OUT/$name" ;;
        *.xml) xmllint --format "$OUT/$name.raw" >"$OUT/$name" ;;
    esac
    rm "$OUT/$name.raw"
}

post_invoice() {
    call "$1" "$2" -X POST -H 'Content-Type: application/json' --data "$3" "$BASE/api/invoices"
}

call customers.json 200 "$BASE/api/customers"
post_invoice invoice-1-gold.json 201 \
    '{"customerCode":"ACME","issueDate":"2026-12-01","lines":[{"sku":"WIDGET","quantity":3,"unitPrice":10.50},{"sku":"GADGET","quantity":1,"unitPrice":1203.00}]}'
post_invoice invoice-2-silver-half-cent.json 201 \
    '{"customerCode":"GLOBEX","issueDate":"2026-12-02","lines":[{"sku":"WIDGET","quantity":10,"unitPrice":51.63}]}'
post_invoice invoice-3-standard-large.json 201 \
    '{"customerCode":"INITECH","issueDate":"2026-12-03","lines":[{"sku":"SERVER","quantity":2,"unitPrice":600.00}]}'
post_invoice error-unknown-customer.json 400 \
    '{"customerCode":"NOBODY","issueDate":"2026-12-04","lines":[{"sku":"WIDGET","quantity":1,"unitPrice":1.00}]}'
call invoice-1.json 200 "$BASE/api/invoices/1"
call error-missing-invoice.json 404 "$BASE/api/invoices/99"
call invoice-1-export.xml 200 "$BASE/api/invoices/1/export"
call invoices.json 200 "$BASE/api/invoices"

page=$(curl -fsS "$BASE/invoices.xhtml")
for ref in $(jq -r '.[].reference' "$OUT/invoices.json"); do
    grep -q "$ref" <<<"$page" || { echo "smoke: the JSF page does not list invoice $ref"; exit 1; }
done
grep -o 'Running on Java [^<]*' <<<"$page" | sed 's/^/smoke: page footer: /'

if $RECORD; then
    rm -rf "$GOLDEN" && mkdir -p "$GOLDEN" && cp "$OUT"/* "$GOLDEN"/
    echo "smoke: recorded $(ls "$GOLDEN" | wc -l) golden files"
    exit 0
fi

if diff -ru "$GOLDEN" "$OUT"; then
    echo "smoke: PASS, $(ls "$GOLDEN" | wc -l) responses match the golden files"
else
    echo "smoke: FAIL, the responses above differ from smoke/golden"
    exit 1
fi
