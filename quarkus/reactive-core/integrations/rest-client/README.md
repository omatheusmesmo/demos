# integrations/rest-client

Calling a slow HTTP service (300 ms per call) twice, in the three models.

| Variant | Endpoint | How it calls the upstream | Timeout mechanism |
|---|---|---|---|
| Worker | `GET /quotes/worker` | two blocking calls in a row | REST Client `read-timeout` |
| Reactive | `GET /quotes/reactive` | `Uni.combine()` of two async calls, overlapping | `ifNoItem().after()` |
| Virtual thread | `GET /quotes/virtual` | two `await().atMost()` in a row | `await().atMost()` |

`GET /quotes/peak` returns the highest number of requests in flight since `DELETE /quotes/peak`.

The benchmark does not use these endpoints: their shapes differ (calls in a row or overlapping, three timeout
mechanisms), so it would compare code instead of models. It calls `GET /fan-out/{worker,reactive,virtual}`, where
every model starts both calls at once through the reactive client and the pair shares one 1 second deadline. The
only difference left is what waits: a blocked worker, the event loop that does not wait, or a parked virtual thread.

## What the tests prove

- Each variant runs on the thread its model promises.
- With `quarkus.thread-pool.max-threads=4` and 12 concurrent requests, the worker variant never exceeds 4 requests in
  flight, while the reactive and virtual thread variants reach 12.
- An upstream slower than one second returns `504` in every variant.
- The REST Client does not pin a virtual thread on Java 25.
- The benchmark endpoints overlap both calls in every model (one upstream delay, not two) and fail through the
  same deadline with the same Mutiny `TimeoutException`, even when the client read timeout is longer.

## Finding worth knowing

The reactive REST Client fails a read timeout with `io.vertx.core.impl.NoStackTraceTimeoutException`, a checked
`java.util.concurrent.TimeoutException`. An exception mapper declared for `RuntimeException` never sees it and the
request ends as a `500`. `ClientTimeoutFirstTest` reproduces it by making the client timeout fire before
`ifNoItem()`.

## Run

```shell
make dev MODULE=integrations/rest-client   # dev mode ships its own slow upstream at /upstream/quote
make test MODULE=integrations/rest-client
make it MODULE=integrations/rest-client
```
