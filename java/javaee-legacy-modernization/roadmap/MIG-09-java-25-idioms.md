---
id: MIG-09
title: Use Java 25 idioms in the API and the dates
priority: could
depends_on: [MIG-08]
runs_on: 25
estimate_bobcoins: 5
cap_bobcoins: 10
---

## Why

The code still reads like Java 8: hand-written DTO getters, `java.util.Date` and a `SimpleDateFormat` that is not
thread safe.

## Scope

- In: the API DTOs (`api` package, response types), dates in the domain and in `Dates`.
- Out: JSON field names, date formats and the XML export.

## Acceptance

`make card CARD=MIG-09` prints `DONE`:

- no `SimpleDateFormat` or `java.util.Date` in `src/main/java`
- the API response DTOs are records
- unit tests and smoke pass on JDK 25

## Pull request

- Branch: `mig-09-java-25-idioms`, from the branch of the last finished card (`main` if there is none).
- Title and commit message: `refactor(java/javaee-legacy-modernization): use records and java.time in the API`
- The description follows the template in [README.md](README.md#pull-requests) and also explains which classes became
  records, how dates are represented now, and that the JSON did not change.

## Prompt for Bob

> Implement roadmap card MIG-09 (roadmap/MIG-09-java-25-idioms.md) on the branch named in its Pull request
> section. Stay inside its scope and finish only when `make card CARD=MIG-09` prints DONE. Then commit
> with the title from the card and write the pull request description.

## Demo value

Medium. Nice before/after code on a slide; only worth Bobcoins if the reserve is healthy.
