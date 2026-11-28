---
id: MIG-03
title: Close the audit log explicitly instead of in finalize()
priority: must
depends_on: []
runs_on: 8
estimate_bobcoins: 3
cap_bobcoins: 6
---

## Why

`AuditLog` closes its file in `finalize()`, which is deprecated for removal and may never run. `InvoiceService`
opens a new `AuditLog` for every invoice and never closes it, so file handles leak until a garbage collection.

## Scope

- In: `AuditLog`, the place in `InvoiceService` that uses it, and `AuditLogTest`.
- Out: the audit line format and the audit file location (`invoicing.audit.file`).

## Acceptance

`make card CARD=MIG-03` prints `DONE`:

- no `finalize()` left in `src/main/java`
- `AuditLog` implements `AutoCloseable` (or `Closeable`) and every use closes it
- unit tests and smoke still pass on JDK 8

## Pull request

- Branch: `mig-03-finalize`, from the branch of the last finished card (`main` if there is none).
- Title and commit message: `refactor(java/javaee-legacy-modernization): close the audit log explicitly instead of in finalize()`
- The description follows the template in [README.md](README.md#pull-requests) and also explains where the audit log
  is closed now, and why `finalize()` never guaranteed it.

## Prompt for Bob

> Implement roadmap card MIG-03 (roadmap/MIG-03-finalize.md) on the branch named in its Pull request
> section. Stay inside its scope and finish only when `make card CARD=MIG-03` prints DONE. Then commit
> with the title from the card and write the pull request description.

## Demo value

Medium. A design change, not a syntax change: Bob has to find the callers and decide who owns the resource.
