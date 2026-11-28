---
id: MIG-07
title: Move the tests to JUnit 5
priority: could
depends_on: [MIG-01]
runs_on: declared
estimate_bobcoins: 2
cap_bobcoins: 4
---

## Why

JUnit 4 is in maintenance mode. JUnit 5 (Jupiter) is what current tooling and the Jakarta ecosystem expect.

## Scope

- In: `src/test/java` and test dependencies in `pom.xml`.
- Out: what the tests assert. Every test keeps its cases and expected values.

## Acceptance

`make card CARD=MIG-07` prints `DONE`:

- no JUnit 4 API left in the tests, the tests use `org.junit.jupiter`
- no JUnit 4 or Mockito 1 dependency
- unit tests and smoke pass on the JDK the project declares

## Pull request

- Branch: `mig-07-junit-5`, from the branch of the last finished card (`main` if there is none).
- Title and commit message: `test(java/javaee-legacy-modernization): migrate the tests from JUnit 4 to JUnit 5`
- The description follows the template in [README.md](README.md#pull-requests) and also explains the recipe that was
  run, and anything it left for a manual change.

## Prompt for Bob

> Implement roadmap card MIG-07 (roadmap/MIG-07-junit-5.md) on the branch named in its Pull request
> section. Stay inside its scope and finish only when `make card CARD=MIG-07` prints DONE. Then commit
> with the title from the card and write the pull request description.

## Demo value

Medium as a contrast: an OpenRewrite recipe (`org.openrewrite.java.testing.junit5.JUnit4to5Migration`) does almost
all of it in seconds, without spending tokens on the transformation itself.
