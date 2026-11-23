# The OCI resources of the orders service. The same file would provision them on a real tenancy: only the
# provider credentials and the host overrides change.

data "oci_objectstorage_namespace" "ns" {
  compartment_id = var.tenancy_ocid
}

resource "oci_objectstorage_bucket" "receipts" {
  compartment_id = var.tenancy_ocid
  namespace      = data.oci_objectstorage_namespace.ns.namespace
  name           = "order-receipts"
}

resource "oci_kms_vault" "orders" {
  compartment_id = var.tenancy_ocid
  display_name   = "orders-vault"
  vault_type     = "DEFAULT"
}

resource "oci_kms_key" "cards" {
  compartment_id      = var.tenancy_ocid
  display_name        = "card-numbers"
  management_endpoint = oci_kms_vault.orders.management_endpoint

  key_shape {
    algorithm = "AES"
    length    = 32
  }
}

resource "oci_queue_queue" "events" {
  compartment_id        = var.tenancy_ocid
  display_name          = "order-events"
  retention_in_seconds  = 86400
  visibility_in_seconds = 30
}

output "receipts_namespace" {
  value = data.oci_objectstorage_namespace.ns.namespace
}

output "receipts_bucket" {
  value = oci_objectstorage_bucket.receipts.name
}

output "events_queue_id" {
  value = oci_queue_queue.events.id
}

output "events_messages_endpoint" {
  value = oci_queue_queue.events.messages_endpoint
}

output "cards_key_id" {
  value = oci_kms_key.cards.id
}

output "cards_crypto_endpoint" {
  value = oci_kms_vault.orders.crypto_endpoint
}
