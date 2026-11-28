---
id: MIG-04
title: Port the Nashorn discount rules to Java
priority: must
depends_on: []
runs_on: 8
estimate_bobcoins: 6
cap_bobcoins: 10
---

## Why

`DiscountRuleEngine` evaluates `rules/discounts.js` with Nashorn, the JavaScript engine that was removed from the
JDK in Java 15. On a modern JDK the engine is simply missing and every invoice fails.

## Scope

- In: `DiscountRuleEngine`, `rules/discounts.js`, `DiscountRuleEngineTest` (adapting how the engine is created is
  fine; the cases and expected amounts are not).
- Out: `InvoiceService` and every other caller: the engine keeps its `discountFor(CustomerTier, BigDecimal)` API.

## Business rules

- GOLD: 10% of the subtotal.
- SILVER: 5% when the subtotal is above 500.
- Anyone: a flat 25 when the subtotal is above 1000.
- Otherwise no discount.
- Issued invoices carry the amounts the current engine computed. A port that is "more correct" but changes any
  amount, even by a cent, is a regression.

## Acceptance

`make card CARD=MIG-04` prints `DONE`:

- no `javax.script`, `ScriptEngine` or Nashorn in the code, no `.js` rules left, no standalone Nashorn dependency
- unit tests and smoke still pass on JDK 8

## Pull request

- Branch: `mig-04-nashorn-rules`, from `javaee-migration` once the previous card's pull request is merged.
- Base of the pull request: `javaee-migration`, never `main`.
- Title and commit message: `refactor(java/javaee-legacy-modernization): port the discount rules from Nashorn to Java`
- The description follows the template in [README.md](README.md#pull-requests) and mentions, in a line, how the Java
  rules keep every amount the JavaScript rules produced, including the half cent case, and that the script is gone.

## Prompt for Bob

> Implement roadmap card MIG-04 (roadmap/MIG-04-nashorn-rules.md) on the branch named in its Pull request
> section. Stay inside its scope and finish only when `make card CARD=MIG-04` prints DONE. Then commit
> with the title from the card, push the branch and open the pull request against `javaee-migration`
> with the description from the template.
