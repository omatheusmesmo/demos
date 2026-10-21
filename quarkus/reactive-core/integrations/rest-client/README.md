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
- A call cancelled by the deadline while it still waits for a connection is never sent.
- Above the capacity of the connection pool (5 connections, 100 ms upstream, 30 requests per second for 25 that
  fit), the worker variant serves every request and sends no wasted call: 20 workers keep the wait for a
  connection under the deadline. The reactive and virtual thread variants fail most requests, yet the upstream
  receives every call, faster than 5 connections should carry.

## Overload: why the worker survives

The reactive and virtual thread variants let every call wait for a connection. Once that wait nears the deadline, a
call leaves the queue just in time to be sent, and its answer arrives after the request has already failed. The
upstream does the work for nothing. Cancelling a call already sent resets the stream, and on HTTP/1.1 Vert.x closes
the connection (`Http1ClientConnection.reset` in Vert.x 5, `Http1xClientConnection.reset` in Vert.x 4), so the pool
opens a new one while the upstream is still busy with the abandoned call. The worker pool acts as a bulkhead: the
extra requests wait for a worker, before their deadline starts.

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
