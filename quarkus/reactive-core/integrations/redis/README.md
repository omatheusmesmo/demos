# integrations/redis

Incrementing a counter in Redis in the three models.

| Variant | Endpoint | Client | Thread |
|---|---|---|---|
| Worker | `GET /counters/worker` | `RedisDataSource` (blocking) | worker |
| Reactive | `GET /counters/reactive` | `ReactiveRedisDataSource` | event loop |
| Virtual thread | `GET /counters/virtual` | `RedisDataSource` (blocking) | virtual thread |

## What the tests prove

- Each variant runs on the thread its model promises and increments its own counter, against a Redis started by
  Dev Services.
- The blocking Redis client does not pin a virtual thread. It is a blocking facade over the reactive Vert.x client,
  so a virtual thread waits on a Vert.x connection rather than on a JDK socket.

## Run

```shell
make dev MODULE=integrations/redis         # Dev Services starts Redis
make test MODULE=integrations/redis
make it MODULE=integrations/redis
```
