# AGENTS.md: cloud/floci-oci

Instructions for coding agents working on this demo. The repository rules in `/AGENTS.md` also apply.

## What this demo is

An expense receipts service (Quarkus) that uses OCI Database with PostgreSQL, Vault Secrets, Object Storage
pre-authenticated requests, Queue and KMS through the OCI Java SDK, tested against floci-oci. It is a floci-oci
showroom: every backing service is an OCI service that floci-oci emulates. Every test checks the OCI side directly
(the object, the ciphertext, the dead-letter queue), not only the HTTP response. Keep it that way.

## Rules

- The application never provisions OCI resources. Terraform (`terraform/`) does in dev, `OciProvisioner` does in
  tests. A new resource goes in both, and its identifiers reach the application as `expenses.*` configuration.
- Keep the floci-oci image pinned (`compose.yaml`, `FlociOciContainer.IMAGE`) and the same in both places.
- Do not add a service floci-oci does not emulate. Check the list at the pinned tag, not on `main`.
- The database password stays in the Vault secret. Never put it in `.env`, properties or test config.
- Before claiming floci-oci behaviour in docs or the article, verify it in the floci-oci source at the pinned tag
  or with a test here. A floci-oci bug gets an upstream issue and a workaround that is also correct on real OCI.

## Build and test

- Java 25 (`.sdkmanrc`). `make test` runs everything and needs only Docker (floci-oci uses the Docker socket to
  start PostgreSQL).
- Terraform, OpenTofu and the OCI CLI run in containers through `scripts/`.
- If a run is killed, `docker ps -a --filter name=floci-oci-psql` shows PostgreSQL containers left behind.
