# Migration rules

This project is a legacy Java EE 8 application on Java 8 that is migrated one roadmap card at a time.

## Working on a card

- Work on exactly one card, the one named in the request. Read `roadmap/<ID>-*.md` and stay inside its Scope.
  If a change outside the scope is needed, stop and say why instead of making it.
- The card is done when `make card CARD=<ID>` prints `DONE`. Run it before saying the card is finished.
- When a check fails, read the log path it prints before changing anything.

## Order of tools

1. Deterministic first: when an OpenRewrite recipe covers the change, run it instead of editing by hand, then
   review its diff. Recipes are not always right: keep what is correct, fix what is not, and say what you fixed.
2. Edit by hand only what no recipe covers.
3. Build and test with the commands in the card, never with ad hoc flags that hide a problem
   (`-DskipTests`, `--add-opens`, `-Dmaven.test.failure.ignore`).

## Behaviour must not change

- `smoke/golden/` holds the responses of the legacy application. Never edit, re-record or delete them.
- Do not change or delete an existing test to make it pass. If a test looks wrong, stop and ask.
- Amounts are money. If a migrated result differs from the legacy one, even by a cent, the migration is wrong.

## Delivering a card

- The migration lives on `javaee-migration`. Start every card from it, up to date: `git switch javaee-migration`,
  `git pull`, then create the card's branch. If the previous card's pull request is still open, or
  `javaee-migration` does not exist, stop and say so.
- One card, one branch, one commit, one pull request. Use the branch name and the title in the card's Pull request
  section; the title is also the commit message.
- Push only the card's branch, then open the pull request against `javaee-migration`:
  `gh pr create --base javaee-migration --title "<title>" --body-file <description>`.
- Write the description with the template in `roadmap/README.md`, in `pr-body.md` at the demo root (git ignores
  it). Keep it short: the change, its benefit, the method and the `make card` output. Never put costs or Bobcoins
  in it.
- Never push to `javaee-migration` or `main`, never open a pull request against `main` and never merge: a person
  reviews and merges every card.
