# labs/threading

Who runs your code in Quarkus. Every endpoint returns the thread that served it, and every test asserts on
that thread, not only on the response body.

| Area | Endpoints | What the tests prove |
|---|---|---|
| Dispatch | `/dispatch/*` | A synchronous signature runs on a worker, a `Uni` on an event loop. `@Blocking`, `@NonBlocking` and `@RunOnVirtualThread` override the signature. `@Transactional` no longer does (see below). |
| Context | `/context/*` | The request filter runs on the same thread as the endpoint in all three models. `ContextLocals` follow work that hops to another thread inside the same duplicated context; a `ThreadLocal` stays behind. The root context refuses locals. |
| Event loops | `/event-loops/current` | With `quarkus.vertx.event-loops-pool-size=2`, every non-blocking request is served by one of two event loops. |
| Blocking | `/await/*`, `/execute-blocking/*` | `await()` is refused on an event loop and fine on a virtual thread. Two `executeBlocking` tasks from the same context run one at a time unless `ordered = false`. |
| CPU | `/cpu/*` | The same CPU-bound work in the three models, used by the benchmark. |
| Pinning | `/pinning/class-initializer` | A blocking static initializer still pins the carrier on Java 25. |

## Findings worth knowing

- `@Transactional` used to send a `Uni` endpoint to a worker. Quarkus removed that rule when `@Transactional` gained
  Hibernate Reactive support (quarkusio/quarkus#51063), but the Quarkus REST guide still documents it. On 3.40.1 the
  `Uni` signature wins and JTA refuses to start on the event loop.
- The virtual thread guide says filters may not run on the virtual thread. With a `@ServerRequestFilter`, they do:
  the filter and the endpoint share the thread in every model, which is why a `ThreadLocal` set in the filter is
  visible. The difference only shows when work moves to another thread.

## Run

```shell
make test MODULE=labs/threading     # @QuarkusTest
make it MODULE=labs/threading       # same assertions against the packaged jar
```
