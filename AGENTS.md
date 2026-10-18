# AGENTS.md

Instructions for coding agents working in this repository. Humans should read `CONTRIBUTING.md`; the rules are the
same.

## Repository layout

- Demos live in `<area>/<demo>/`, where the area is a domain: `quarkus/`, `quarkus-langchain4j/`, `cloud/`, and new
  ones as needed. Areas are one level deep.
- Some demos are git submodules (see `.gitmodules`). Do not edit them from the monorepo: work in the submodule's own
  repository, then update the recorded commit here.
- `tooling/` holds domain-agnostic scripts only. Never add a reference to a specific demo there.
- Each demo has its own `AGENTS.md` with demo-specific instructions. Read it before changing the demo.

## Rules

- A demo never depends on another demo. Do not add relative paths or Maven dependencies across demos.
- Keep each demo buildable from its own folder.
- New demo: follow `CONTRIBUTING.md`, including the row in the root `README.md` and a workflow that calls a reusable
  workflow from `.github/workflows/`.
- Language: English for code, tests, documentation and commit messages.
- Commits: Conventional Commits, scope is the demo path (`feat(quarkus/reactive-core): ...`) or `tooling`. No AI
  attribution of any kind in commits, branches or pull requests.
- Never run `git push`. Leave commits local and let the maintainer push.
- Prose: do not use em dashes or en dashes; use commas, colons or parentheses.
