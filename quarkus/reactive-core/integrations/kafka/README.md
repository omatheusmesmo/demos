# integrations/kafka

Three Kafka consumers doing the same work (`sequence:processingMillis`), one per model. Each records the thread it
was handed to and how many orders it processed at once.

| Consumer | Topic | Signature | Thread | Order |
|---|---|---|---|---|
| `event-loop` | `orders-event-loop` | `Uni<Void> consume(String)` | event loop | kept |
| `blocking` | `orders-blocking` | `@Blocking void consume(String)` | worker (`executor-thread-*`) | kept |
| `virtual` | `orders-virtual` | `@RunOnVirtualThread void consume(String)` | virtual thread | **lost** |

`GET /consumers/{event-loop|blocking|virtual}` returns the processed orders in completion order and the peak
concurrency; `DELETE /consumers` clears them. Tests send orders straight to Kafka with a plain `KafkaProducer`,
using the broker that Dev Services started.

## What the tests prove

- Each consumer runs on the thread its model promises.
- Orders sent as `1:300`, `2:200`, `3:100` finish as `1, 2, 3` on the event loop and blocking consumers (one at a
  time) and as `3, 2, 1` on the virtual thread consumer (three at once). `@RunOnVirtualThread` changes the
  semantics, not only the thread.
- `smallrye.messaging.worker.<virtual-thread>.max-concurrency=2` caps the virtual thread consumer at two orders at a
  time. The `<virtual-thread>` placeholder is literal: written without the angle brackets, the key is ignored and
  the six orders run at once.
- In steady state, a virtual thread consumer does not pin.

## Run

```shell
make dev MODULE=integrations/kafka         # Dev Services starts a Kafka broker
make test MODULE=integrations/kafka
make it MODULE=integrations/kafka
```
