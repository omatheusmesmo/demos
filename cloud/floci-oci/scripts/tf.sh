#!/usr/bin/env bash
# Runs Terraform (TOOL=terraform, the default) or OpenTofu (TOOL=tofu) in a container against floci-oci.
# Usage: scripts/tf.sh <command> [args...], for example scripts/tf.sh plan
set -euo pipefail

cd "$(dirname "$0")/../terraform"

endpoint="${FLOCI_OCI_ENDPOINT:-http://localhost:4599}"
case "${TOOL:-terraform}" in
  terraform) image=hashicorp/terraform:1.10.5 ;;
  tofu) image=ghcr.io/opentofu/opentofu:1.9.1 ;;
  *) echo "TOOL must be terraform or tofu" >&2; exit 2 ;;
esac

[ -f floci-key.pem ] || openssl genrsa -out floci-key.pem 2048 2>/dev/null

clients=(
  oci_identity.IdentityClient
  oci_object_storage.ObjectStorageClient
  oci_kms.KmsVaultClient
  oci_queue.QueueAdminClient
)
overrides=""
for client in "${clients[@]}"; do
  overrides+="${overrides:+;}${client}=${endpoint}"
done

exec docker run --rm --network host \
  --user "$(id -u):$(id -g)" \
  -e HOME=/tmp \
  -e TF_VAR_CLIENT_HOST_OVERRIDES="$overrides" \
  -v "$PWD":/work -w /work \
  "$image" "$@"
