# Entry point for every task of the demo. TOOL=tofu runs the Terraform targets with OpenTofu.
MVN := ./mvnw -B

.PHONY: test up provision dev cli-tour drift destroy down

test:
	$(MVN) verify

up:
	docker compose up -d --wait

provision: up
	scripts/provision.sh

dev: provision
	$(MVN) quarkus:dev

cli-tour:
	scripts/cli-tour.sh

drift:
	scripts/tf.sh plan -detailed-exitcode -input=false

# OCI refuses to delete a bucket that still holds objects (409; the code is Conflict in floci-oci 0.7.0), so empty it first.
destroy:
	docker exec floci-oci ocilocal os object bulk-delete -bn expense-receipts --namespace floci-local --force
	scripts/tf.sh destroy -auto-approve -input=false

# The reset removes the PostgreSQL containers floci-oci started; compose only knows about floci-oci itself.
down:
	-curl -fsS -X POST http://localhost:4599/_floci/state/reset >/dev/null
	docker compose down
	rm -rf .env .certs terraform/terraform.tfstate terraform/terraform.tfstate.backup
