#!/usr/bin/env bash
# A tour of the expenses resources with the unmodified OCI CLI, run inside the compat container
# (ocilocal = oci --endpoint ...). Reads the identifiers that scripts/provision.sh wrote to .env.
set -euo pipefail

cd "$(dirname "$0")/.."
# shellcheck disable=SC1091
source .env

tenancy=ocid1.tenancy.oc1..flocilocaltenancy0000000000000000000000000000000000000000
bucket="$EXPENSES_STORAGE_BUCKET"
namespace="$EXPENSES_STORAGE_NAMESPACE"
oci() { docker exec -i floci-oci ocilocal "$@"; }
step() { printf '\n\033[1m$ %s\033[0m\n' "$*"; }

step oci psql db-system-collection list-db-systems --compartment-id "$tenancy"
oci psql db-system-collection list-db-systems --compartment-id "$tenancy" \
  --query 'data.items[].{name:"display-name",state:"lifecycle-state",version:"db-version",shape:shape}' --output table

db_system_id=$(oci psql db-system-collection list-db-systems --compartment-id "$tenancy" \
  --query 'data.items[0].id' --raw-output)
step oci psql connection-details get --db-system-id "\$DB_SYSTEM_ID"
oci psql connection-details get --db-system-id "$db_system_id" --query 'data."primary-db-endpoint"'

step oci secrets secret-bundle get --secret-id "\$EXPENSES_DATABASE_PASSWORD_SECRET_ID"
oci secrets secret-bundle get --secret-id "$EXPENSES_DATABASE_PASSWORD_SECRET_ID" \
  --query 'data.{version:"version-number",stages:stages,type:"secret-bundle-content"."content-type"}'

step oci os preauth-request create -bn "$bucket" --access-type ObjectWrite --object-name incoming/cli-tour
access_uri=$(oci os preauth-request create -bn "$bucket" --namespace "$namespace" --name cli-tour \
  --access-type ObjectWrite --object-name incoming/cli-tour \
  --time-expires "$(date -u -d '+15 minutes' +%Y-%m-%dT%H:%M:%SZ)" --query 'data."access-uri"' --raw-output)
echo "$access_uri"

step curl -X PUT --data-binary @receipt.pdf "http://localhost:4599\$ACCESS_URI"
printf '%%PDF-1.7 taxi receipt\n' | curl -fsS -X PUT --data-binary @- "http://localhost:4599$access_uri" \
  -o /dev/null -w 'HTTP %{http_code}, no OCI signature\n'

step oci os object list -bn "$bucket" --prefix incoming/
oci os object list -bn "$bucket" --namespace "$namespace" --prefix incoming/ --query 'data[].{name:name,size:size}'

step oci kms crypto generate-data-encryption-key --key-id "\$EXPENSES_KEYS_KEY_ID"
oci kms crypto generate-data-encryption-key --key-id "$EXPENSES_KEYS_KEY_ID" \
  --key-shape '{"algorithm": "AES", "length": 32}' --include-plaintext-key false \
  --query 'data.{ciphertext:ciphertext,plaintext:plaintext}'

step oci queue messages get-stats --queue-id "\$EXPENSES_JOBS_QUEUE_ID"
oci queue messages get-stats --queue-id "$EXPENSES_JOBS_QUEUE_ID" --query 'data.queue'

step oci os bucket get -bn does-not-exist
oci os bucket get -bn does-not-exist --namespace "$namespace" 2>&1 | grep -E '"(code|message|status)"' || true
