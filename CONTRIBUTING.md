# Adding a demo

## Where it goes

1. Pick the area that matches the domain of the demo: `quarkus/`, `java/`, `ai/` or `cloud/`. Create the area folder
   if this is its first demo. A demo lives in exactly one area.
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

## Rules

- A demo is self-contained. It builds and runs from its own folder and never depends on another demo.
- `tooling/` only holds domain-agnostic scripts. If a script names a module, an endpoint or a demo path, it belongs
  in the demo's `scripts/`.
- Every demo has a `README.md` (what it shows, how to run it, what the tests prove) and a `Makefile` with at least a
  `test` target.
- Every demo has exactly one workflow in `.github/workflows/`, named after the demo path, that only calls a
  reusable workflow and filters on the demo folder and `tooling/**`.
- Code, tests, documentation and commit messages are in English. Commits follow Conventional Commits with the demo
  path as scope, for example `feat(quarkus/reactive-core): ...`.

## Checklist

- [ ] Folder in the right area, named after what it shows
- [ ] `README.md` and `Makefile` with `make test`
- [ ] Workflow calling a reusable workflow
- [ ] Row added to the table in the root `README.md`
