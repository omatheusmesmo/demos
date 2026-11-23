# One message per submitted claim. A message that fails three deliveries moves to the queue's dead-letter queue.

resource "oci_queue_queue" "receipt_processing" {
  compartment_id                   = var.tenancy_ocid
  display_name                     = "receipt-processing"
  retention_in_seconds             = 86400
  visibility_in_seconds            = 60
  dead_letter_queue_delivery_count = 3
}
