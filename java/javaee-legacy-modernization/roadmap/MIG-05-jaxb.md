---
id: MIG-05
title: Make XML binding independent of the JDK
priority: must
depends_on: [MIG-01, MIG-02]
runs_on: 8
estimate_bobcoins: 3
cap_bobcoins: 6
---

## Why

The XML export uses JAXB (`javax.xml.bind`), which shipped inside the JDK until Java 10 and was removed in 11.
`javaee-api` 8.0 does not contain it either, so on JDK 25 the export does not even compile.

## Scope

- In: dependencies in `pom.xml`.
- Out: the export code and its XML format; the Liberty configuration (on Java EE 8 the server provides JAXB).

## Acceptance

`make card CARD=MIG-05` prints `DONE`:

- `InvoiceXmlExporterTest` passes on JDK 25
- unit tests and smoke still pass on JDK 8

## Pull request

- Branch: `mig-05-jaxb`, from the branch of the last finished card (`main` if there is none).
- Title and commit message: `build(java/javaee-legacy-modernization): depend on XML binding explicitly instead of on the JDK`
- The description follows the template in [README.md](README.md#pull-requests) and also explains which artifact
  provides XML binding now, in which scope, and why the server still provides it at runtime.

## Prompt for Bob

> Implement roadmap card MIG-05 (roadmap/MIG-05-jaxb.md) on the branch named in its Pull request
> section. Stay inside its scope and finish only when `make card CARD=MIG-05` prints DONE. Then commit
> with the title from the card and write the pull request description.

## Demo value

Low to medium. Shows the difference between what the JDK, the platform API and the server each provide, which
matters again in MIG-08.
