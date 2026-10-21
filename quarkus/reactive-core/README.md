# quarkus/reactive-core

Who runs your code in Quarkus? This demo answers with tests. Every endpoint, Kafka consumer and scheduled job
reports the thread that ran it, and every test asserts on that thread, across three models:

- **worker**: a synchronous signature on the worker pool
- **reactive**: a `Uni` or `Multi` on the Vert.x event loop
- **virtual thread**: `@RunOnVirtualThread`

```
$ make threads MODULE=labs/threading
PATH                     THREAD                               VIRTUAL  EVENTLOOP  DUPLICATED
/dispatch/sync           executor-thread-1                    false    false      true
/dispatch/uni            vert.x-eventloop-thread-3            false    true       true
/dispatch/virtual        quarkus-virtual-thread-0             true     false      true
```

Built on Quarkus 3.40.1 (Vert.x 4.5, Netty 4.1, Mutiny 3.3) and Java 25, and rerun against Quarkus 4.0.0.Beta1
(Vert.x 5.1, Netty 4.2).

## Prerequisites

- Java 25. With SDKMAN, `sdk env` picks it from `.sdkmanrc`.
- Docker, for the Dev Services that start PostgreSQL, Kafka and Redis in tests and dev mode.
- JBang and Python 3, only for the benchmark (JBang fetches Hyperfoil, the load generator).

## Quick start

```shell
make test                                   # every module, @QuarkusTest and plain JUnit
make it                                     # plus the same assertions against each packaged jar
make dev MODULE=integrations/rest-client    # dev mode, with a built-in slow upstream
make threads MODULE=integrations/rest-client
```

## Modules

| Module | Shows |
|---|---|
| [`labs/threading`](labs/threading) | dispatch rules, duplicated contexts and context locals, event loops, `await()`, `executeBlocking`, CPU work, pinning |
| [`labs/mutiny`](labs/mutiny) | Mutiny without Quarkus: threads, composition, failures, streams, back-pressure |
| [`integrations/rest-client`](integrations/rest-client) | calling a slow HTTP service; the worker pool as a limit; timeouts |
| [`integrations/postgres`](integrations/postgres) | Hibernate ORM and Hibernate Reactive side by side; the connection pool as a limit |
| [`integrations/kafka`](integrations/kafka) | consumers per model; ordering and concurrency |
| [`integrations/redis`](integrations/redis) | the blocking and reactive Redis clients |
| [`integrations/scheduler`](integrations/scheduler) | scheduled jobs per model |
| `shared/common`, `shared/test-support` | `ThreadReport`, `Observed`, `ConcurrencyTracker`, WireMock backend, thread matchers |

## Behaviour, test and article

| Behaviour | Test | Article |
|---|---|---|
| A synchronous signature runs on a worker, a `Uni` on an event loop | `labs/threading` `DispatchTest` | 2 |
| `@Blocking`, `@NonBlocking` and `@RunOnVirtualThread` override the signature | `DispatchTest` | 2 |
| `@Transactional` no longer sends a `Uni` endpoint to a worker | `DispatchTest` | 2 |
| `@RunOnVirtualThread` also moves a `Uni` endpoint to a virtual thread | `DispatchTest` | 2 |
| Non-blocking requests are served by the configured number of event loops | `EventLoopsTest` | 1 |
| `await()` is refused on an event loop and fine on a virtual thread | `AwaitTest` | 1, 4 |
| Two `executeBlocking` tasks from one context run one at a time unless unordered | `ExecuteBlockingTest` | 1 |
| The request filter runs on the endpoint's thread; context locals follow work to another thread, thread locals do not | `ContextTest` | 2 |
| A pre-matching filter runs on the event loop, so its thread local never reaches a worker or virtual thread | `ContextTest` | 2 |
| The root context refuses context locals | `ContextTest` | 2 |
| A blocking static initializer pins a virtual thread on Java 25 | `labs/threading` `PinningTest` | 4 |
| A `Uni` is a recipe: nothing runs until a subscription, each subscription runs again; `invoke` drops a returned `Uni` | `labs/mutiny` `recipes` | 3 |
| Mutiny runs stages on the emitting thread; `emitOn` and `runSubscriptionOn` move them | `labs/mutiny` `threads` | 3 |
| `combine` over synchronous sources is sequential; merge and concatenate differ in order | `composition` | 3 |
| Retry is resubscription; failures are terminal unless isolated per item | `failures` | 3 |
| Pagination stops on cancellation; sources respect demand; bounded buffers fail | `streams` | 3 |
| The worker pool limits concurrent requests; reactive and virtual threads do not | `rest-client` `ConcurrencyTest` | 4 |
| Each model has its own timeout mechanism, all mapped to `504` | `rest-client` `TimeoutTest` | 3, 4 |
| The connection pool limits virtual threads | `postgres` `PoolLimitTest` | 4 |
| `@RunOnVirtualThread` consumers lose the message order; the concurrency key caps them | `kafka` `ConsumersTest`, `ConcurrencyLimitTest` | 3, 4 |
| The scheduler picks the thread from the method signature | `scheduler` `JobsTest` | 2 |
| Messaging picks the thread from the signature too, and still treats `@Transactional` as blocking | `kafka` `ConsumersTest`, `EventLoopExecutionModeTest` | 2 |

Articles of the series *Inside the Quarkus reactive core* (one per Sunday; links are added as each one is published):

1. The event loop from the ground up
2. Who runs your code in Quarkus
3. Mutiny is a recipe, not a result
4. Reactive or virtual threads in Quarkus 4

## Pinning audit (Java 25.0.3, Quarkus 3.40.1)

Every virtual thread path runs under `@VirtualThreadUnit` with `@ShouldNotPin`, after a warm-up request, and each
test also asserts that a virtual thread really served the request. Generate the table with `make pinning-report`
after `make test`.

| Module | Test | Outcome |
|---|---|---|
| `integrations/kafka` | aVirtualThreadConsumerDoesNotPin | passed |
| `integrations/postgres` | hibernateOrmAndThePostgresqlDriverDoNotPinAVirtualThread | passed |
| `integrations/redis` | theBlockingRedisClientDoesNotPinAVirtualThread | passed |
| `integrations/rest-client` | theRestClientDoesNotPinAVirtualThread | passed |
| `integrations/scheduler` | aVirtualThreadJobDoesNotPin | passed |
| `labs/threading` | aPlainVirtualThreadEndpointDoesNotPin | passed |
| `labs/threading` | aBlockingStaticInitializerPinsTheCarrier (`@ShouldPin`) | passed: it still pins |

## Quarkus 4 rerun (4.0.0.Beta1, 2026-10-18)

`make try-quarkus-4` reruns the whole suite with `-Dquarkus.platform.version=4.0.0.Beta1` (Vert.x 5.1.8, Netty
4.2.18) and lists the tests whose outcome changed.

- Every test of `labs/mutiny`, `rest-client`, `postgres`, `kafka`, `redis` and `scheduler` has the same outcome on
  3.40.1 and 4.0.0.Beta1, thread assertions, concurrency limits and pinning audit included.
- `labs/threading` first failed to compile: `ContextResource` stored a local on the root context with
  `Context.putLocal(Object, Object)`, which Vert.x 5 removed (its `Context` only takes typed `ContextLocal` keys now).
  The endpoint now runs `ContextLocals.put` on the root context through `runOnContext`. `ContextLocals` is the
  SmallRye API Quarkus ships for both lines (`smallrye-common-vertx-context` on Vert.x 4,
  `smallrye-common-vertx5-context` on Vert.x 5), so the same source compiles on both, and all 39 tests of the module
  pass on both.

## Benchmark

`make bench` compares the three models under load, following the methodology of the
[Quarkus performance guide](https://quarkus.io/guides/performance-measure) and its reference lab,
[quarkusio/spring-quarkus-perf-comparison](https://github.com/quarkusio/spring-quarkus-perf-comparison):

- [Hyperfoil](https://hyperfoil.io) as the load generator, in an open model (a constant arrival rate), so a slow
  server shows up as latency and unsent requests instead of quietly lowering the load (coordinated omission).
- A fresh JVM per iteration (3), each with a 2 minute warmup and a 30 second cooldown before anything is measured.
- `-Xms` = `-Xmx`, `-XX:ActiveProcessorCount` matching the CPU set, ParallelGC (the collector of the reference lab
  and of the UBI image), and separate CPU sets for the application, the load generator and the dependencies.
- Memory as the RSS of the JVM process (`docker top`), never `docker stats`, which the guide warns against.
- CPU from the container cgroup, plus the time other host processes took on the application CPUs, so a disturbed
  run is visible in the results.

Scenarios and arrival rates are described at the top of `benchmark/run.sh`; the shared tooling lives in
`tooling/bench`. Summaries land in `benchmark/results/` (raw Hyperfoil results and GC logs stay local).

## Findings

Things the tests turned up while the demo was written, each documented in its module:

- **`@Transactional` no longer forces a worker.** The rule was removed when `@Transactional` gained Hibernate
  Reactive support ([quarkusio/quarkus#51063](https://github.com/quarkusio/quarkus/pull/51063)); the Quarkus REST
  guide still documents it. A `Uni` endpoint with `@Transactional` stays on the event loop and JTA refuses to start.
- **Request filters share the endpoint's thread.** In all three models, a `@ServerRequestFilter` runs on the thread
  that runs the endpoint, which is why a `ThreadLocal` set in a filter looks like it works. A pre-matching filter
  runs on the event loop instead, and its `ThreadLocal` is gone by the time a worker or virtual thread runs the
  endpoint.
- **`@RunOnVirtualThread` is not limited to blocking signatures in Quarkus REST.** The virtual thread guide says it
  is; the code only rejects it together with `@NonBlocking`, and a `Uni` endpoint with it runs on a virtual thread.
- **The same annotation, two rules.** Since Quarkus 3.10 ([quarkusio/quarkus#39768](https://github.com/quarkusio/quarkus/pull/39768)),
  messaging dispatches a synchronous consumer to a worker, like Quarkus REST. Messaging still treats `@Transactional`
  as blocking; Quarkus REST no longer does.
- **The reactive REST Client fails a timeout with a checked exception.** `NoStackTraceTimeoutException` extends
  `java.util.concurrent.TimeoutException`, so an exception mapper for `RuntimeException` misses it.
- **The first JDBC call on a virtual thread can pin once.** The JDK initialises `sun.nio.ch.Poller` on the first
  blocking socket read from a virtual thread, and a virtual thread waiting for that class initialisation is pinned.
- **The messaging concurrency key is literal.** `smallrye.messaging.worker.<virtual-thread>.max-concurrency` works
  with the angle brackets; without them the key is ignored.
- **Reading a field through a client proxy returns the proxy's field.** The scheduler module met it; the fix is to
  read bean state through methods.
