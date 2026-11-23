#!/usr/bin/env bash
# A tour of floci-oci with the unmodified OCI CLI, run inside the compat container (ocilocal = oci --endpoint ...).
# Reads the resource OCIDs that scripts/provision.sh wrote to .env.
set -euo pipefail

cd "$(dirname "$0")/.."
# shellcheck disable=SC1091
source .env

tenancy=ocid1.tenancy.oc1..flocilocaltenancy0000000000000000000000000000000000000000
oci() { docker exec -i floci-oci ocilocal "$@"; }
step() { printf '\n\033[1m$ oci %s\033[0m\n' "$*"; }

step os ns get
oci os ns get

step os bucket list --compartment-id "$tenancy"
oci os bucket list --compartment-id "$tenancy" --query 'data[].name'

step os object put -bn "$ORDERS_RECEIPTS_BUCKET" --name hello.txt
echo "hello from the OCI CLI" | oci os object put -bn "$ORDERS_RECEIPTS_BUCKET" --namespace "$ORDERS_RECEIPTS_NAMESPACE" \
  --name hello.txt --file - --force
step os object get -bn "$ORDERS_RECEIPTS_BUCKET" --name hello.txt --file -
oci os object get -bn "$ORDERS_RECEIPTS_BUCKET" --namespace "$ORDERS_RECEIPTS_NAMESPACE" --name hello.txt --file -

step queue messages put-messages --queue-id "\$ORDERS_EVENTS_QUEUE_ID"
oci queue messages put-messages --queue-id "$ORDERS_EVENTS_QUEUE_ID" --messages '[{"content":"hello queue"}]'
step queue messages get-messages --queue-id "\$ORDERS_EVENTS_QUEUE_ID" --visibility-in-seconds 0
oci queue messages get-messages --queue-id "$ORDERS_EVENTS_QUEUE_ID" --visibility-in-seconds 0 --limit 5 \
  --query 'data.messages[].content'

step kms crypto encrypt --key-id "\$ORDERS_CARDS_KEY_ID"
plaintext=$(printf '4111111111114242' | base64)
ciphertext=$(oci kms crypto encrypt --key-id "$ORDERS_CARDS_KEY_ID" --plaintext "$plaintext" \
  --query 'data.ciphertext' --raw-output)
echo "$ciphertext"
step kms crypto decrypt --key-id "\$ORDERS_CARDS_KEY_ID" --ciphertext ...
oci kms crypto decrypt --key-id "$ORDERS_CARDS_KEY_ID" --ciphertext "$ciphertext" \
  --query 'data.plaintext' --raw-output | base64 -d
echo

step os bucket get -bn does-not-exist
oci os bucket get -bn does-not-exist --namespace "$ORDERS_RECEIPTS_NAMESPACE" 2>&1 | grep -E '"(code|message|status)"' || true
