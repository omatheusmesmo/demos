---
id: MIG-02
title: Replace sun.misc.BASE64Encoder with java.util.Base64
priority: must
depends_on: []
runs_on: 8
estimate_bobcoins: 2
cap_bobcoins: 4
---

## Why

`ReferenceCodec` builds invoice references with `sun.misc.BASE64Encoder`, an internal JDK API that was removed in
Java 9. The code does not compile on any modern JDK.

## Scope

- In: `ReferenceCodec` and its test.
- Out: the reference format. References already printed on issued invoices must decode the same way.

## Acceptance

`make card CARD=MIG-02` prints `DONE`:

- no `sun.misc` API anywhere under `src/`
- references are built with `java.util.Base64`
- unit tests and smoke still pass on JDK 8 (the golden files hold real references)

## Pull request

- Branch: `mig-02-base64`, from `javaee-migration` once the previous card's pull request is merged.
- Base of the pull request: `javaee-migration`, never `main`.
- Title and commit message: `refactor(java/javaee-legacy-modernization): encode invoice references with java.util.Base64`
- The description follows the template in [README.md](README.md#pull-requests) and mentions, in a line, that the
  references are byte for byte the ones the legacy encoder produced.

## Prompt for Bob

> Implement roadmap card MIG-02 (roadmap/MIG-02-base64.md) on the branch named in its Pull request
> section. Stay inside its scope and finish only when `make card CARD=MIG-02` prints DONE. Then commit
> with the title from the card, push the branch and open the pull request against `javaee-migration`
> with the description from the template.
