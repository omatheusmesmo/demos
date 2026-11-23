output "storage_namespace" {
  value = data.oci_objectstorage_namespace.ns.namespace
}

output "storage_bucket" {
  value = oci_objectstorage_bucket.receipts.name
}

output "jobs_queue_id" {
  value = oci_queue_queue.receipt_processing.id
}

output "jobs_messages_endpoint" {
  value = oci_queue_queue.receipt_processing.messages_endpoint
}

output "keys_key_id" {
  value = oci_kms_key.receipts.id
}

output "keys_crypto_endpoint" {
  value = oci_kms_vault.expenses.crypto_endpoint
}

output "database_username" {
  value = oci_psql_db_system.expenses.credentials[0].username
}

output "database_password_secret_id" {
  value = oci_vault_secret.database_password.id
}

output "database_host" {
  value = data.oci_psql_db_system_connection_detail.expenses.primary_db_endpoint[0].fqdn
}

output "database_port" {
  value = data.oci_psql_db_system_connection_detail.expenses.primary_db_endpoint[0].port
}

output "database_ca_certificate" {
  value = data.oci_psql_db_system_connection_detail.expenses.ca_certificate
}
