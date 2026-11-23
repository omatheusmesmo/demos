terraform {
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "9.12.1"
    }
  }
}

variable "tenancy_ocid" {
  type    = string
  default = "ocid1.tenancy.oc1..flocilocaltenancy0000000000000000000000000000000000000000"
}

variable "user_ocid" {
  type    = string
  default = "ocid1.user.oc1..flocidemouser00000000000000000000000000000000000000000000000"
}

# The provider signs every call, so it needs a parseable RSA key. floci-oci never verifies the signature:
# scripts/tf.sh generates a throwaway key on first use. The endpoint comes from the provider's per-client
# host overrides (TF_VAR_CLIENT_HOST_OVERRIDES), set by scripts/tf.sh as well.
provider "oci" {
  tenancy_ocid     = var.tenancy_ocid
  user_ocid        = var.user_ocid
  fingerprint      = "aa:bb:cc:dd:ee:ff:00:11:22:33:44:55:66:77:88:99"
  private_key_path = "./floci-key.pem"
  region           = "us-ashburn-1"
}
