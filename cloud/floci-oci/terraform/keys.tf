# The master key wraps one data key per receipt (envelope encryption) and protects the database password secret.

resource "oci_kms_vault" "expenses" {
  compartment_id = var.tenancy_ocid
  display_name   = "expenses-vault"
  vault_type     = "DEFAULT"
}

resource "oci_kms_key" "receipts" {
  compartment_id      = var.tenancy_ocid
  display_name        = "receipts-master-key"
  management_endpoint = oci_kms_vault.expenses.management_endpoint

  key_shape {
    algorithm = "AES"
    length    = 32
  }
}
