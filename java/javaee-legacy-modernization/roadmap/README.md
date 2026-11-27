# Migration roadmap

The migration from Java 8 and Java EE 8 to Java 25 and Jakarta EE 11 is split into cards, like tickets on a board.
Every card is small, has its own scope and leaves the application green, so the migration can stop after any card
and still be shippable.

## The board

`make board` prints every card with its status, computed from the code (`scripts/card.sh <ID> --quick`):

| Card | Title | Runs on | Priority | Bobcoins (estimate / cap) |
|---|---|---|---|---|
| [MIG-01](MIG-01-build-plugins.md) | Build plugins that run on a modern JDK | 8 | must | 2 / 4 |
| [MIG-02](MIG-02-base64.md) | Replace `sun.misc.BASE64Encoder` with `java.util.Base64` | 8 | must | 2 / 4 |
| [MIG-03](MIG-03-finalize.md) | Close the audit log explicitly instead of in `finalize()` | 8 | must | 3 / 6 |
| [MIG-04](MIG-04-nashorn-rules.md) | Port the Nashorn discount rules to Java | 8 | must | 6 / 10 |
| [MIG-05](MIG-05-jaxb.md) | Make XML binding independent of the JDK | 8 | must | 3 / 6 |
| [MIG-06](MIG-06-jdk-25.md) | Run on JDK 25, still on Java EE 8 | 25 | must | 5 / 10 |
| [MIG-07](MIG-07-junit-5.md) | Move the tests to JUnit 5 | either | could | 2 / 4 |
| [MIG-08](MIG-08-jakarta-ee-11.md) | Move from Java EE 8 (`javax`) to Jakarta EE 11 (`jakarta`) | 25 | must | 6 / 12 |
| [MIG-09](MIG-09-java-25-idioms.md) | Use Java 25 idioms in the API and the dates | 25 | could | 5 / 10 |
| [MIG-10](MIG-10-quarkus.md) | Move from Jakarta EE 11 on Liberty to Quarkus | 25 | stretch | 20 / 30 |
| [MIG-11](MIG-11-jakarta-ee-12.md) | Move to Jakarta EE 12 once it is released | 25 | blocked | ? |

The Bobcoin numbers are estimates for IBM Bob, to be recalibrated with the real cost of the first cards.

## Why this order

The cards that run on JDK 8 come first. `java.util.Base64`, `AutoCloseable` and an explicit JAXB dependency all
exist on Java 8, so the obstacles are removed while the application still runs where it always ran. The JDK moves
only in MIG-06, and the namespace only in MIG-08, so a failure always points at one change.

## Working on a card

Every card is one branch and one pull request. `main` holds only the legacy application and these cards: it is the
input of the demo, and the migration is never merged into it.

1. Create the branch named in the card, from the branch of the last finished card, or from `main` for the first one.
   Cards build on each other, so the pull requests are stacked.
2. Read the card. Its Scope says what may change; its Acceptance says when it is done.
3. Change the code. `make card CARD=MIG-06` runs the card's own checks, then the regression gates:
   - the golden files under `smoke/golden/` are untouched;
   - the unit tests pass on the card's JDK;
   - `scripts/smoke.sh` starts the application on Open Liberty and every response matches the golden files, which
     were recorded on the legacy baseline.
4. When it prints `DONE`, commit with the title in the card's Pull request section and open the pull request.

Every card's checks fail on `main` (red first) and pass after a correct migration. A finished card stays finished:
later cards never turn an earlier card red.

## Pull requests

The title is the one in the card, a Conventional Commit with the demo path as scope. The description has these
sections, plus what the card's Pull request section asks to explain:

```markdown
## Card
MIG-NN: <card title>, with a link to roadmap/MIG-NN-*.md

## What changed
One line per file or group of files, and why.

## How
What an OpenRewrite recipe did (which recipe, and what of its diff was kept or fixed) and what was changed by hand.

## Checks
The output of `make card CARD=MIG-NN`, ending in DONE.

## Cost
Bobcoins spent on the card, against its estimate and cap.
```

## Budget rules

- A card that goes over its `cap_bobcoins` is stopped, its branch is dropped and the card is split.
- Checking a card costs nothing: `make card` is a script, not a prompt.
- Resetting costs nothing: `git restore . && git clean -fd` on the card's branch, or a fresh branch from the previous
  card, never "undo that" in a prompt.

## Demo states

| State | Cards done | What it shows |
|---|---|---|
| A, minimum | MIG-01 to MIG-04 | the board, the cleanup on Java 8, Bob's judgment on the discount rules |
| B, target | + MIG-05, MIG-06 | the same application, the same smoke, now on JDK 25 |
| C, full | + MIG-08 | Jakarta EE 11 on Open Liberty, on Java 25 |

Moving between states (switching to another card's branch, or resetting a card that went wrong) is safe: `make test`,
`make smoke` and `make card` remove what an earlier build left in `target/` and start Liberty clean, because a
workarea built for `jakartaee-11.0` makes a `javaee-8.0` server start without security.
