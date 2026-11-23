# cloud/floci-oci

Oracle Cloud on localhost: a Quarkus orders service that uses four OCI services, tested end to end against
[floci-oci](https://github.com/floci-io/floci-oci), the free and open source OCI emulator, with no Oracle Cloud
account.

Placing an order touches every service:

| Step | Service | Where it runs locally |
|---|---|---|
| Encrypt the card number with an AES key | OCI Vault + KMS | floci-oci (real AES-GCM) |
| Store the order (only the last four digits and the ciphertext) | Oracle Database | Oracle Database Free, through Quarkus Dev Services |
| Write a JSON receipt | OCI Object Storage | floci-oci |
| Publish an `order-created` event | OCI Queue | floci-oci |

floci-oci 0.7.0 does not emulate Oracle Database or Autonomous Database (its only database service is OCI
Database with PostgreSQL), so the database is the Oracle Database Free container that Quarkus Dev Services start.

## What the tests prove

`make test` starts floci-oci and Oracle Database Free in containers and runs 10 tests. The tests check every OCI
service directly, not through the application:

| Test | Proves |
|---|---|
| `OrderResourceTest` | The API never returns the card; Oracle holds only the KMS ciphertext, which decrypts back to the card; the receipt is in the bucket; the event is in the queue; invalid input is a 400 before any OCI call; unknown orders are 404 |
| `CardVaultRotationTest` | KMS encryption is real: the same card never encrypts to the same ciphertext, and a card encrypted before a key rotation still decrypts after it |
| `TenancyIsolationTest` | One floci-oci, two tenancies: a bucket of tenancy A is invisible to tenancy B (`404 NotAuthorizedOrNotFound` on A's compartment, `404 BucketNotFound` by name), and both can own a bucket with the same name |

## Requirements

- Java 25 (`.sdkmanrc`), Maven wrapper included
- Docker. Terraform, OpenTofu and the OCI CLI run in containers, nothing else to install

Tested with floci-oci 0.7.0, Quarkus 3.40.1, OCI Java SDK 3.102.0, Oracle Database Free
(`gvenzl/oracle-free:23-slim-faststart`, Oracle AI Database 26ai Free 23.26.4), Testcontainers 2.0.5,
Terraform 1.10.5, OpenTofu 1.9.1, OCI CLI 3.96.0 and the `oracle/oci` provider 9.12.1, on Docker 29.9.2.

## Run it

```shell
make test        # the 10 tests, nothing to start by hand
make dev         # floci-oci + Terraform apply + quarkus dev
make cli-tour    # the unmodified OCI CLI against the same resources
make drift       # terraform plan -detailed-exitcode: exit 0 means no drift
make down        # stop floci-oci and forget the local state
```

`TOOL=tofu make provision drift` does the same with OpenTofu.

With `make dev` running:

```shell
curl -i -X POST localhost:8080/orders -H 'Content-Type: application/json' \
  -d '{"customer":"ana","total":129.90,"cardNumber":"4111111111114242"}'
curl localhost:8080/orders/1
curl localhost:8080/orders/1/receipt
```

## How it is wired

There is no Quarkus extension for the OCI SDK, so [`OciClients`](src/main/java/dev/omatheusmesmo/demos/flocioci/oci/OciClients.java)
is the whole integration: one signer and one client per service.

- **Signer.** `oci.auth.mode=config-file` reads `~/.oci/config` as the OCI CLI does. In dev and test,
  `throwaway-key` signs with an RSA key generated at startup ([`ThrowawayKey`](src/main/java/dev/omatheusmesmo/demos/flocioci/oci/ThrowawayKey.java)).
  floci-oci reads the tenancy and user from the signature's `keyId` but never verifies the signature.
- **Endpoints.** `oci.endpoint` points Object Storage at the emulator. The Queue and KMS crypto clients do not use
  the regional endpoint even on real OCI: they talk to the queue's `messagesEndpoint` and the vault's
  `cryptoEndpoint`, so both are configuration (`orders.events.messages-endpoint`, `orders.cards.crypto-endpoint`).
- **Provisioning.** The application never creates resources. In dev, [`terraform/`](terraform) does, and
  `scripts/provision.sh` writes the outputs to `.env`, which Quarkus reads. In tests,
  [`FlociOciResource`](src/test/java/dev/omatheusmesmo/demos/flocioci/support/FlociOciResource.java) does the same
  through the SDK.
- **The emulator's own address.** floci-oci builds the endpoints it returns from `FLOCI_OCI_BASE_URL`
  (default `http://localhost:4599`). [`FlociOciContainer`](src/test/java/dev/omatheusmesmo/demos/flocioci/support/FlociOciContainer.java)
  binds a free host port chosen up front and passes it in that variable, so the returned endpoints are right.
- **Terraform.** The official `oracle/oci` provider, unchanged, pointed at floci-oci through
  `TF_VAR_CLIENT_HOST_OVERRIDES` (set in `scripts/tf.sh`).

## Article

*Oracle Cloud on localhost with floci-oci* on [blog.omatheusmesmo.dev](https://blog.omatheusmesmo.dev).
