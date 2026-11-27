---
id: MIG-01
title: Build plugins that run on a modern JDK
priority: must
depends_on: []
runs_on: 8
estimate_bobcoins: 2
cap_bobcoins: 4
---

## Why

The build itself cannot run on a modern JDK: `maven-war-plugin` 2.2 fails on JDK 25 with
`Cannot access defaults field of Properties`. Every later card needs a build that works on both JDKs.

## Scope

- In: plugin versions in `pom.xml` (`maven-compiler-plugin`, `maven-surefire-plugin`, `maven-war-plugin`).
- Out: dependencies, Java code, the compiler's source and target (still 1.8), the Liberty configuration.

## Acceptance

`make card CARD=MIG-01` prints `DONE`:

- the WAR plugin runs on JDK 25
- `maven-compiler-plugin` >= 3.11.0, `maven-war-plugin` >= 3.4.0, `maven-surefire-plugin` >= 3.0.0
- unit tests and smoke still pass on JDK 8

## Pull request

- Branch: `mig-01-build-plugins`, from the branch of the last finished card (`main` if there is none).
- Title and commit message: `build(java/javaee-legacy-modernization): upgrade the build plugins to versions that run on a modern JDK`
- The description follows the template in [README.md](README.md#pull-requests) and also explains the version of each
  plugin before and after, and the error the old one gives on a modern JDK.

## Prompt for Bob

> Implement roadmap card MIG-01 (roadmap/MIG-01-build-plugins.md) on the branch named in its Pull request
> section. Stay inside its scope and finish only when `make card CARD=MIG-01` prints DONE. Then commit
> with the title from the card and write the pull request description.

## Demo value

Low. A warm-up card: it shows the loop (read the card, change, `make card`) with almost no risk.
