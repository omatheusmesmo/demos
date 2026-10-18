# labs/mutiny

Mutiny on its own, without Quarkus, so the tests run in seconds. Each behaviour is driven by a small production
class, and each test was first run against a naive version of that class to show the pitfall it guards against.

| Package | Class | What the tests prove |
|---|---|---|
| `threads` | `BlockingLookup` | Mutiny schedules nothing: a blocking call runs on the subscribing thread until `runSubscriptionOn` moves it |
| `threads` | `ItemProcessor` | stages run on the emitting thread; `emitOn` moves every following stage |
| `composition` | `QuoteAggregator` | `Uni.combine()` over synchronous sources runs them one after the other; sources overlap only when they are asynchronous |
| `composition` | `OrderedFanOut` | `transformToUniAndConcatenate` keeps the input order, `transformToUniAndMerge` emits in completion order |
| `failures` | `RetryingCall` | retrying resubscribes to the upstream, and gives up with the last failure |
| `failures` | `ResilientStream` | a failure ends a `Multi` even after recovery; isolating each item in its own `Uni` keeps it alive |
| `streams` | `PageFetcher` | a paginated source stops fetching pages as soon as the consumer has enough |
| `streams` | `Sensors` | a demand-driven source produces only what was requested |
| `streams` | `BurstBuffer` | a bounded overflow buffer fails with `BackPressureFailure` instead of growing |

Red messages from the naive versions, kept because they explain the behaviour better than prose:

- `expected: <[a, b]> but was: <[a:ran-alone, b]>`: chaining two sources with `chain` runs them one at a time.
- `expected: <3> but was: <100>`: an emitter that ignores demand produced 100 readings when 3 were requested.
- `expected: <lookup-worker> but was: <main>`: without `runSubscriptionOn`, the blocking call ran on the caller.

## Run

```shell
make test MODULE=labs/mutiny
```
