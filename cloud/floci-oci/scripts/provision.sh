#!/usr/bin/env bash
# Provisions the expenses resources in floci-oci with Terraform and writes what the application needs to .env,
# which Quarkus reads in dev mode. The database password is not in .env: the application reads it from the Vault.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
tf="$root/scripts/tf.sh"

"$tf" init -input=false -no-color >/dev/null
"$tf" apply -auto-approve -input=false -no-color

output() { "$tf" output -raw "$1"; }

mkdir -p "$root/.certs"
ca="$root/.certs/expenses-db-ca.pem"
output database_ca_certificate > "$ca"

cat > "$root/.env" <<ENV
EXPENSES_STORAGE_NAMESPACE=$(output storage_namespace)
EXPENSES_STORAGE_BUCKET=$(output storage_bucket)
EXPENSES_JOBS_QUEUE_ID=$(output jobs_queue_id)
EXPENSES_JOBS_MESSAGES_ENDPOINT=$(output jobs_messages_endpoint)
EXPENSES_KEYS_KEY_ID=$(output keys_key_id)
EXPENSES_KEYS_CRYPTO_ENDPOINT=$(output keys_crypto_endpoint)
EXPENSES_DATABASE_JDBC_URL=jdbc:postgresql://$(output database_host):$(output database_port)/postgres?sslmode=verify-full&sslrootcert=$ca
EXPENSES_DATABASE_USERNAME=$(output database_username)
EXPENSES_DATABASE_PASSWORD_SECRET_ID=$(output database_password_secret_id)
ENV

echo "Wrote .env:"
cat "$root/.env"
