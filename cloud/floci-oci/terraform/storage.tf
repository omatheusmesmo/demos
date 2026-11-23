# Receipts land in incoming/ through a pre-authenticated upload URL and are kept, encrypted, under receipts/.

data "oci_objectstorage_namespace" "ns" {
  compartment_id = var.tenancy_ocid
}

resource "oci_objectstorage_bucket" "receipts" {
  compartment_id = var.tenancy_ocid
  namespace      = data.oci_objectstorage_namespace.ns.namespace
  name           = "expense-receipts"
  access_type    = "NoPublicAccess"
}
