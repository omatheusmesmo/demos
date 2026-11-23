# cloud/floci-oci

Oracle Cloud on localhost: an expense receipts service built only on OCI services, tested end to end against
[floci-oci](https://github.com/floci-io/floci-oci), the free and open source OCI emulator, with no Oracle Cloud
account.

An employee opens a claim, uploads the receipt straight to Object Storage and submits. A worker checks the file,
encrypts it and marks the claim ready for review:

| Step | Service | Where it runs locally |
|---|---|---|
| Keep the claims, read the admin password from the Vault, connect with TLS `verify-full` | OCI Database with PostgreSQL + Vault Secrets | floci-oci (a real PostgreSQL 16 container per DB system) |
| Hand the client a URL that accepts one upload, with no OCI credentials | Object Storage pre-authenticated request | floci-oci |
| One job per submitted claim, retried, dead-lettered after 3 deliveries | OCI Queue | floci-oci |
| Encrypt every receipt with its own data key (envelope encryption) | OCI Vault + KMS | floci-oci (real AES-GCM) |
| Create all of the above | Terraform or OpenTofu with the official `oracle/oci` provider | your machine, in a container |

Everything is an OCI service that floci-oci emulates. The one thing it does not emulate is Networking, so the DB
system's subnet OCID is stored as given.

## The flow

```
client                 expenses service                     OCI (floci-oci)
  | POST /expenses        |  INSERT claim (AWAITING_RECEIPT)  -> PostgreSQL
  |<- upload URL ---------|  CreatePreauthenticatedRequest     -> Object Storage (ObjectWrite, incoming/<id>, 15 min)
  | PUT receipt --------------------------------------------- -> Object Storage (no signature)
  | POST /{id}/submit     |  HeadObject, UPDATE SUBMITTED, PutMessages -> Object Storage, PostgreSQL, Queue
  |                       |
  |               worker  |  GetMessages                       -> Queue
  |                       |  GetObject incoming/<id>, check PDF/PNG/JPEG
  |                       |  GenerateDataEncryptionKey, AES-GCM -> KMS
  |                       |  PutObject receipts/<id>, UPDATE READY_FOR_REVIEW, DeleteObject incoming/<id>, DeleteMessage
  | GET /{id}/receipt     |  GetObject, KMS Decrypt of the data key, AES-GCM open, SHA-256 check
```

A job that keeps failing is left to the queue: after 3 deliveries it lands in the dead-letter queue, visible in
`GetStats`.

## What the tests prove

`make test` starts floci-oci in a container, provisions everything (the PostgreSQL DB system included) and runs 15
tests. The tests check the OCI side directly, not only the HTTP responses:

| Test | Proves |
|---|---|
| `ExpenseWorkflowTest` | A PDF uploaded through the pre-authenticated URL comes back byte for byte; the bucket holds only ciphertext under `receipts/` and nothing left in `incoming/`; a file that is not a receipt is rejected; submitting without an upload or twice is a 409; the upload URL cannot write any other object nor read its own; a job that arrives before its submission commits is retried, not dropped; a poison job reaches the dead-letter queue; claims are listed from the database; invalid input is a 400; an unknown claim is a 404 |
| `ReceiptCipherRotationTest` | KMS envelope encryption is real: every receipt gets its own data key, and a receipt sealed before a master key rotation still opens after it |
| `TenancyIsolationTest` | One floci-oci, two tenancies: a bucket of tenancy A is invisible to tenancy B (`404 NotAuthorizedOrNotFound` on A's compartment, `404 BucketNotFound` by name), and both can own a bucket with the same name |

## Requirements

- Java 25 (`.sdkmanrc`), Maven wrapper included
- Docker. Terraform, OpenTofu and the OCI CLI run in containers, nothing else to install. floci-oci gets the Docker
  socket, because it starts one PostgreSQL container per DB system

Tested with floci-oci 0.7.0 (PostgreSQL 16 sidecar), Quarkus 3.40.1, OCI Java SDK 3.102.0, Testcontainers 2.0.5,
Terraform 1.10.5, OpenTofu 1.9.1, OCI CLI 3.96.0 and the `oracle/oci` provider 9.12.1, on Docker 29.9.2.

## Run it

```shell
make test        # the 15 tests, nothing to start by hand
make dev         # floci-oci + Terraform apply + quarkus dev
make cli-tour    # the unmodified OCI CLI against the same resources
make drift       # terraform plan -detailed-exitcode: exit 0 means no drift
make destroy     # terraform destroy (empties the bucket first)
make down        # remove the PostgreSQL containers, stop floci-oci, forget the local state
```

`TOOL=tofu make provision drift destroy` does the same with OpenTofu.

With `make dev` running:

```shell
curl -s -X POST localhost:8080/expenses -H 'Content-Type: application/json' \
  -d '{"employee":"ana","merchant":"Hotel Aurora","amount":189.90,"currency":"BRL","spentOn":"2026-11-20"}'
curl -X PUT --data-binary @receipt.pdf '<upload.url from the response>'
curl -X POST localhost:8080/expenses/<id>/submit
curl localhost:8080/expenses/<id>                 # READY_FOR_REVIEW after a second
curl -o back.pdf localhost:8080/expenses/<id>/receipt
```

## How it is wired

There is no Quarkus extension for the OCI SDK, so [`OciClients`](src/main/java/dev/omatheusmesmo/demos/flocioci/oci/OciClients.java)
is the whole integration: one signer and one client per service.

- **Signer.** `oci.auth.mode=config-file` reads `~/.oci/config` as the OCI CLI does. In dev and test,
  `throwaway-key` signs with an RSA key generated at startup ([`ThrowawayKey`](src/main/java/dev/omatheusmesmo/demos/flocioci/oci/ThrowawayKey.java)).
  floci-oci reads the tenancy and user from the signature's `keyId` but never verifies the signature.
- **Endpoints.** `oci.endpoint` points Object Storage and Secrets at the emulator. The Queue and KMS crypto clients
  do not use the regional endpoint even on real OCI: they talk to the queue's `messagesEndpoint` and the vault's
  `cryptoEndpoint`, so both are configuration (`expenses.jobs.messages-endpoint`, `expenses.keys.crypto-endpoint`).
- **Database password.** It is never in configuration. The datasource uses a Quarkus `CredentialsProvider`
  ([`VaultSecretCredentialsProvider`](src/main/java/dev/omatheusmesmo/demos/flocioci/oci/VaultSecretCredentialsProvider.java))
  that reads the secret the DB system was created with. The JDBC URL verifies the server against the CA certificate
  from the DB system's connection details.
- **Provisioning.** The application never creates resources. In dev, [`terraform/`](terraform) does, and
  `scripts/provision.sh` writes the outputs to `.env` (and the CA certificate to `.certs/`), which Quarkus reads. In
  tests, [`OciProvisioner`](src/test/java/dev/omatheusmesmo/demos/flocioci/support/OciProvisioner.java) does the same
  through the SDK.
- **The emulator's own address.** floci-oci builds the endpoints it returns from `FLOCI_OCI_BASE_URL`
  (default `http://localhost:4599`). [`FlociOciContainer`](src/test/java/dev/omatheusmesmo/demos/flocioci/support/FlociOciContainer.java)
  binds a free host port chosen up front and passes it in that variable, so the returned endpoints are right. It
  also sets `FLOCI_OCI_DOCKER_ENDPOINT_MODE=published`, so the database endpoint is the PostgreSQL container's host
  port, and resets floci-oci before stopping it so the PostgreSQL containers go away too.
- **Terraform.** The official `oracle/oci` provider, unchanged, pointed at floci-oci through
  `TF_VAR_CLIENT_HOST_OVERRIDES` (set in `scripts/tf.sh`).

## Known floci-oci gaps

- [floci-io/floci-oci#144](https://github.com/floci-io/floci-oci/issues/144): a PAR's `fullPath` is relative (real
  OCI returns the complete URL), and Get/List return it with the token. `ReceiptBucket` resolves a relative path
  against the Object Storage endpoint, which also works on real OCI.
- [floci-io/floci-oci#143](https://github.com/floci-io/floci-oci/issues/143): no Oracle Database or Autonomous
  Database yet, which is why the claims live in OCI Database with PostgreSQL.

## Article

[Oracle Cloud on localhost with floci-oci](https://blog.omatheusmesmo.dev/en/posts/floci-oci-oracle-cloud-localhost/)
([Portuguese](https://blog.omatheusmesmo.dev/posts/floci-oci-oracle-cloud-local/)).
