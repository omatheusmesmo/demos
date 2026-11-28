---
id: MIG-10
title: Move from Jakarta EE 11 on Liberty to Quarkus
priority: stretch
depends_on: [MIG-08]
runs_on: 25
estimate_bobcoins: 20
cap_bobcoins: 30
---

## Why

Quarkus is built on the `jakarta` namespace, so once MIG-08 is done the remaining gap is the runtime, not the code
base. Not validated end to end yet: treat the estimate as a guess.

## Scope

- In: the build (Quarkus instead of WAR and Liberty), EJBs (to CDI with explicit `@Transactional`), configuration
  (`application.properties` instead of `server.xml` and JNDI), the JSF page (Quarkiverse MyFaces or a Qute page),
  and the start and stop commands in `scripts/smoke.sh`.
- Out: `smoke/golden/` and every assertion in the tests.

## Acceptance

`make card CARD=MIG-10` prints `DONE`:

- the build is a Quarkus application, with no WAR packaging, Liberty or EJB left
- unit tests and smoke pass on JDK 25

## Pull request

- Branch: `mig-10-quarkus`, from the branch of the last finished card (`main` if there is none).
- Title and commit message: `refactor(java/javaee-legacy-modernization): move from Open Liberty to Quarkus`
- The description follows the template in [README.md](README.md#pull-requests) and also explains what replaced each of
  EJB, JSF and Liberty, and what was left out of the card and why.

## Prompt for Bob

> Implement roadmap card MIG-10 (roadmap/MIG-10-quarkus.md) on the branch named in its Pull request
> section. Stay inside its scope and finish only when `make card CARD=MIG-10` prints DONE. Then commit
> with the title from the card and write the pull request description.

## Demo value

High but risky: an EJB is transactional by default and a CDI bean is not, so a missing `@Transactional` compiles
and silently stops saving. Better as the "what comes next" slide unless there is time and Bobcoins left.
