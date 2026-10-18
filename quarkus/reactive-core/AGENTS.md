# AGENTS.md: quarkus/reactive-core

Instructions for coding agents working on this demo. The repository rules in `/AGENTS.md` also apply.

## What this demo is

An executable specification of who runs your code in Quarkus. Every endpoint, consumer and job returns or records
the thread that ran it (`ThreadReport`), and every test asserts on that thread, not only on the result. Keep it
that way: a change that makes a test pass without checking the thread defeats the purpose of the demo.

## Layout and where new code goes

| Folder | Put here |
|---|---|
| `shared/common` | records and helpers used by several modules. Plain Java, no CDI beans, so no Jandex index is needed |
| `shared/test-support` | test helpers (WireMock backend, thread matchers, parallel requests) |
| `labs/threading` | Quarkus behaviours without a business integration: dispatch, contexts, event loops, pinning |
| `labs/mutiny` | Mutiny on its own, plain JUnit, no Quarkus |
| `integrations/<name>` | one module per real integration, each with the three variants |
| `benchmark/`, `scripts/` | load scenarios and demo-specific scripts; generic scripts go to `/tooling` |

## The three-variant contract

Each integration exposes the same work three times: a worker variant (synchronous signature), a reactive variant
(`Uni` or `Multi`, on the event loop) and a virtual thread variant (`@RunOnVirtualThread`). Endpoints return
`Observed<T>`, which carries the result and the `ThreadReport`. New integrations follow the same contract.

## Workflow

1. Every behaviour starts as a failing test. Show the failure before writing the production code.
2. Assert the thread with `ThreadMatchers` (`ranOnEventLoop`, `ranOnWorker`, `ranOnVirtualThread`).
3. Concurrency claims assert peaks from `ConcurrencyTracker`, never elapsed time.
4. Pinning tests use `@VirtualThreadUnit` with `@ShouldNotPin` or `@ShouldPin`, assert that a virtual thread really
   served the request (otherwise the check passes vacuously), and warm up first when the integration does JDK
   socket I/O.
5. Each `*Test` has a `*IT` subclass with `@QuarkusIntegrationTest` when the test does not depend on a test profile
   or on in-JVM state.

## Build and test

- Java 25 (`.sdkmanrc`). `make test MODULE=<path>` for `@QuarkusTest`, `make it MODULE=<path>` to add the
  integration tests against the packaged jar. Without `MODULE`, every module runs.
- Quarkus modules use `<packaging>quarkus</packaging>` and declare `quarkus-maven-plugin` with its version and
  `<extensions>true</extensions>`. Without both, the application is not packaged and integration tests fail.
- New Quarkus modules are created with the Quarkus CLI (`quarkus create app ... -o integrations`) and then wired to
  the parent: add `<parent>`, keep the dependencies, drop the BOM import and plugin versions the parent manages.

## Quarkus tooling

- Use the Quarkus MCP tools when they are available: `quarkus_skills` before writing code for an extension,
  `quarkus_searchDocs` for documentation (it is version-aware).
- Prefer an existing Quarkus extension over hand-written infrastructure.
- Verify version-specific claims in the source or BOM of the Quarkus version in `pom.xml`, not in memory or in
  an issue's status.
