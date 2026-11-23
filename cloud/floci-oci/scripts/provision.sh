#!/usr/bin/env bash
# Provisions the orders resources in floci-oci with Terraform and writes their identifiers to .env,
# which Quarkus reads in dev mode.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
tf="$here/tf.sh"

"$tf" init -input=false -no-color >/dev/null
"$tf" apply -auto-approve -input=false -no-color

output() { "$tf" output -raw "$1"; }

cat > "$here/../.env" <<EOF
ORDERS_RECEIPTS_NAMESPACE=$(output receipts_namespace)
ORDERS_RECEIPTS_BUCKET=$(output receipts_bucket)
ORDERS_EVENTS_QUEUE_ID=$(output events_queue_id)
ORDERS_EVENTS_MESSAGES_ENDPOINT=$(output events_messages_endpoint)
ORDERS_CARDS_KEY_ID=$(output cards_key_id)
ORDERS_CARDS_CRYPTO_ENDPOINT=$(output cards_crypto_endpoint)
EOF

echo "Wrote .env:"
cat "$here/../.env"
