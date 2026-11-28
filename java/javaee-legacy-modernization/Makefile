# Entry point for every task of the demo. The JDK comes from .sdkmanrc unless JDK=<major> is given.
JDK ?= $(shell sed -n 's/^java=\([0-9]*\).*/\1/p' .sdkmanrc)

.PHONY: test smoke run card board

test:
	bash -c "source scripts/lib.sh && clean_build_outputs"
	scripts/with-jdk.sh $(JDK) ./mvnw -B verify

smoke:
	JDK=$(JDK) scripts/smoke.sh

run:
	scripts/with-jdk.sh $(JDK) ./mvnw -B liberty:dev -Dclean=true

card:
	@test -n "$(CARD)" || { echo "usage: make card CARD=MIG-02"; exit 2; }
	scripts/card.sh $(CARD)

board:
	scripts/board.sh
