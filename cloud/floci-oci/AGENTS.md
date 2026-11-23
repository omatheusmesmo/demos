# AGENTS.md: cloud/floci-oci

Instructions for coding agents working on this demo. The repository rules in `/AGENTS.md` also apply.

## What this demo is

A Quarkus service that uses OCI Vault/KMS, Object Storage and Queue through the OCI Java SDK, with Oracle Database
from Quarkus Dev Services, tested against floci-oci. Every test checks the OCI service directly (the row, the
object, the message), not only the HTTP response. Keep it that way.

## Rules

- The application never provisions OCI resources. Terraform (`terraform/`) does in dev, `OciProvisioner` does in
  tests. A new resource goes in both, and its identifiers reach the application as `orders.*` configuration.
- Keep the floci-oci image pinned (`compose.yaml`, `FlociOciContainer.IMAGE`) and the same in both places.
- floci-oci has no Oracle Database service. Do not describe the database as emulated by floci-oci.
- Before claiming floci-oci behaviour in docs or the article, verify it in the floci-oci source at the pinned tag
  or with a test here.

## Build and test

- Java 25 (`.sdkmanrc`). `make test` runs everything and needs only Docker.
- Oracle Database Free can take over a minute to start; `quarkus.devservices.timeout` is 5 minutes for that reason.
- Terraform, OpenTofu and the OCI CLI run in containers through `scripts/`.
