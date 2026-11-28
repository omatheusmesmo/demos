---
id: MIG-08
title: Move from Java EE 8 (javax) to Jakarta EE 11 (jakarta)
priority: must
depends_on: [MIG-06]
runs_on: 25
estimate_bobcoins: 6
cap_bobcoins: 12
---

## Why

Java EE 8 is the last release in the `javax` namespace. Every current runtime, library and fix lives in
`jakarta`. Jakarta EE 11 is the newest final release that Open Liberty supports (`jakartaee-11.0`).

## Scope

- In: Java imports, XML descriptors (`persistence.xml`, `web.xml`, `beans.xml`), the JSF page, dependencies in
  `pom.xml`, the features in `src/main/liberty/config/server.xml`.
- Out: behaviour. The API, the XML export and the JSF page answer exactly as before.

## Acceptance

`make card CARD=MIG-08` prints `DONE`:

- no Java EE `javax.*` package in the code and no `xmlns.jcp.org` schema in the descriptors
- Liberty runs `jakartaee-11.0`, no `javaee-8.0` left, no `javaee-api` dependency left
- the JDK's own `javax` packages are untouched
- unit tests and smoke pass on JDK 25

## Pull request

- Branch: `mig-08-jakarta-ee-11`, from `javaee-migration` once the previous card's pull request is merged.
- Base of the pull request: `javaee-migration`, never `main`.
- Title and commit message: `refactor(java/javaee-legacy-modernization): migrate from Java EE 8 to Jakarta EE 11`
- The description follows the template in [README.md](README.md#pull-requests) and mentions, in a line, the recipe
  that was run, what of its diff was kept, what was fixed by hand and why, and the Liberty features before and after.

## Prompt for Bob

> Implement roadmap card MIG-08 (roadmap/MIG-08-jakarta-ee-11.md) on the branch named in its Pull request
> section. Stay inside its scope and finish only when `make card CARD=MIG-08` prints DONE. Then commit
> with the title from the card, push the branch and open the pull request against `javaee-migration`
> with the description from the template.
