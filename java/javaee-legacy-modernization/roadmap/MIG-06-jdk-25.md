---
id: MIG-06
title: Run on JDK 25, still on Java EE 8
priority: must
depends_on: [MIG-01, MIG-02, MIG-03, MIG-04, MIG-05]
runs_on: 25
estimate_bobcoins: 5
cap_bobcoins: 10
---

## Why

With the blockers gone, the application can move to Java 25 (LTS) on its current platform. Open Liberty runs the
`javaee-8.0` feature on Java 25, so the JDK and the Jakarta namespace can move in separate steps.

## Scope

- In: `.sdkmanrc` (`java=25...`), the compiler release in `pom.xml`, and whatever the build and the tests need to
  run on JDK 25.
- Out: the Liberty features (stay on `javaee-8.0`; Jakarta is MIG-08), the application code's behaviour.

## Acceptance

`make card CARD=MIG-06` prints `DONE`:

- `.sdkmanrc` declares JDK 25 and the compiler targets release 25 (no `source`/`target` 1.8 left)
- unit tests and smoke pass on JDK 25

## Pull request

- Branch: `mig-06-jdk-25`, from `javaee-migration` once the previous card's pull request is merged.
- Base of the pull request: `javaee-migration`, never `main`.
- Title and commit message: `build(java/javaee-legacy-modernization): build and run on JDK 25`
- The description follows the template in [README.md](README.md#pull-requests) and mentions, in a line, every change
  besides the JDK version that JDK 25 required, each with the error that made it necessary.

## Prompt for Bob

> Implement roadmap card MIG-06 (roadmap/MIG-06-jdk-25.md) on the branch named in its Pull request
> section. Stay inside its scope and finish only when `make card CARD=MIG-06` prints DONE. Then commit
> with the title from the card, push the branch and open the pull request against `javaee-migration`
> with the description from the template.
