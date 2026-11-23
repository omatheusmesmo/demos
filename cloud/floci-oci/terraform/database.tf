# The claims live in OCI Database with PostgreSQL. The admin password never leaves the Vault: the DB system reads it
# from the secret, and so does the application at startup.

resource "random_password" "database" {
  length  = 32
  special = false
}

resource "oci_vault_secret" "database_password" {
  compartment_id = var.tenancy_ocid
  vault_id       = oci_kms_vault.expenses.id
  key_id         = oci_kms_key.receipts.id
  secret_name    = "expenses-db-password"

  secret_content {
    content_type = "BASE64"
    content      = base64encode(random_password.database.result)
  }
}

resource "oci_psql_db_system" "expenses" {
  compartment_id              = var.tenancy_ocid
  display_name                = "expenses-db"
  db_version                  = "16"
  shape                       = "PostgreSQL.VM.Standard.E4.Flex"
  instance_ocpu_count         = 2
  instance_memory_size_in_gbs = 32

  network_details {
    subnet_id = var.database_subnet_id
  }

  storage_details {
    is_regionally_durable = true
    system_type           = "OCI_OPTIMIZED_STORAGE"
  }

  credentials {
    username = "expenses"

    password_details {
      password_type  = "VAULT_SECRET"
      secret_id      = oci_vault_secret.database_password.id
      secret_version = "1"
    }
  }
}

data "oci_psql_db_system_connection_detail" "expenses" {
  db_system_id = oci_psql_db_system.expenses.id
}
