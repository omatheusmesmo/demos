# Entry point for every task of the demo. MODULE is a module path such as integrations/rest-client.
MVN := ./mvnw -B
SELECT := $(if $(MODULE),-pl $(MODULE) -am,)
QUARKUS_4 ?= 4.0.0.Beta1

.PHONY: test it dev threads bench pinning-report try-quarkus-4

test:
	$(MVN) verify $(SELECT)

it:
	$(MVN) verify -DskipITs=false $(SELECT)

dev:
	@test -n "$(MODULE)" || { echo "usage: make dev MODULE=integrations/rest-client"; exit 1; }
	$(MVN) -q install -DskipTests -pl shared/common,shared/test-support -am
	cd $(MODULE) && ../../mvnw quarkus:dev

threads:
	@test -n "$(MODULE)" || { echo "usage: make threads MODULE=integrations/rest-client"; exit 1; }
	scripts/threads.sh $(MODULE)

bench:
	benchmark/run.sh

pinning-report:
	scripts/pinning-report.sh

try-quarkus-4:
	../../tooling/quarkus/try-quarkus-version.sh . $(QUARKUS_4)
