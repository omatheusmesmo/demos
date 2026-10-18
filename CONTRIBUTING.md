# Adding a demo

## Where it goes

1. Pick the area that matches the domain of the demo, for example `quarkus/`, `quarkus-langchain4j/`, `java/` or
   `cloud/`. Areas stay at one level: a narrower domain gets its own area (`quarkus-langchain4j/`) rather than a
   folder inside another area. Create the area folder with its first demo. A demo lives in exactly one area.
2. Name the demo folder after what it shows, in kebab case: `quarkus/reactive-core`, not `quarkus/demo1`.
3. Inside the demo, name folders by role:

   | Folder | Holds |
   |---|---|
   | `shared/` | code used by several modules of the demo |
   | `labs/` | modules that isolate one behaviour, without a business integration |
   | `integrations/` | modules built around a real integration (database, broker, HTTP client) |
   | `benchmark/` | load scenarios and their results |
   | `scripts/` | scripts that only make sense for this demo |

   A small demo does not need all of them.

## In-tree demo or submodule

- **In-tree**: the demo lives in this repository. Use it for new demos.
- **Submodule**: the demo already has its own repository, history and issues. Add it with
  `git submodule add https://github.com/<owner>/<repo>.git <area>/<demo>`, using the HTTPS URL so the monorepo can be
  cloned without SSH keys. Its README, tests and CI stay in its own repository, so it needs no workflow here.
  Changes to a submodule are committed and pushed in that repository first; the monorepo then records the new commit.

## Rules

- A demo is self-contained. It builds and runs from its own folder and never depends on another demo.
- `tooling/` only holds domain-agnostic scripts. If a script names a module, an endpoint or a demo path, it belongs
  in the demo's `scripts/`.
- Every demo has a `README.md` (what it shows, how to run it, what the tests prove) and a `Makefile` with at least a
  `test` target.
- Every in-tree demo has exactly one workflow in `.github/workflows/`, named after the demo path, that only calls a
  reusable workflow and filters on the demo folder and `tooling/**`.
- Code, tests, documentation and commit messages are in English. Commits follow Conventional Commits with the demo
  path as scope, for example `feat(quarkus/reactive-core): ...`.

## Checklist

- [ ] Folder in the right area, named after what it shows
- [ ] `README.md` and `Makefile` with `make test`
- [ ] Workflow calling a reusable workflow (in-tree demos only)
- [ ] Row added to the table in the root `README.md`
