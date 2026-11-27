# AGENTS.md: java/javaee-legacy-modernization

Instructions for coding agents working on this demo. The repository rules in `/AGENTS.md` also apply.

## What this demo is

A small invoicing back office written the way a Java EE 8 application on Java 8 was written around 2018 (EJB, JPA,
JAX-RS, JSF, JAXB, Nashorn), and a roadmap of cards (`roadmap/`) that migrates it to Java 25 and Jakarta EE 11 on
Open Liberty. The migration itself is done by IBM Bob, live, card by card. On `main` the application stays legacy:
it is the input of the demo.

## Rules

- On `main`, do not modernize the application code. Its legacy APIs are the traps the cards are about.
- `smoke/golden/` was recorded on the baseline. Never edit or re-record it; `smoke/golden.sha256` guards it.
- Every card has a checks file in `roadmap/checks/`. A card's checks must fail on `main` and pass after a
  correct migration, and must not fail again once a later card is done. Verify both when changing a card.
- When working on a card, follow `.bob/rules/migration.md`: one card per branch and per pull request, never merged
  into `main`.

## Build and test

- `make test` and `make smoke` use the JDK declared in `.sdkmanrc` (8 on `main`); `JDK=25 make test` overrides it.
  JDKs are resolved from `$JDK<major>_HOME` or from SDKMAN.
- `make smoke` downloads Open Liberty on the first run, starts it on port 9080 and stops it at the end.
- `make card CARD=MIG-04` checks one card; `make board` shows every card.
