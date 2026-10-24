# integrations/postgres

The same slow query (`pg_sleep(0.2)`) in the three models. Hibernate ORM and Hibernate Reactive live in the same
application and manage the same plain `@Entity`, each through its own Panache repository.

| Variant | Endpoint | Stack | Thread |
|---|---|---|---|
| Worker | `GET /books/worker` | Hibernate ORM + JDBC (Agroal) | worker |
| Reactive | `GET /books/reactive` | Hibernate Reactive + reactive PostgreSQL client | event loop |
| Virtual thread | `GET /books/virtual` | Hibernate ORM + JDBC (Agroal) | virtual thread |

`GET /books/peak` returns the most requests in flight, `GET /books/pool` the most JDBC connections used at once;
`DELETE` on either resets it.

## What the tests prove

- Each variant runs on the thread its model promises, against a real PostgreSQL started by Dev Services.
- With `quarkus.datasource.jdbc.max-size=4`, twelve concurrent virtual thread requests are all in flight at once,
  but exactly four connections are used: virtual threads remove the worker pool limit, not the connection pool limit.
- In steady state, Hibernate ORM and the PostgreSQL driver do not pin a virtual thread on Java 25.
- With 5 connections in each pool, 20 workers and twice the load the connections can serve, from clients that give
  up after 2 s, every model runs queries for requests nobody waits for anymore, and more than half of the database
  work is wasted (`AbandonedQueryTest`, which prints what it measured). After the last client gave up, the database
  stays busy:
  - for under 2 s with workers: a request still waiting for a worker when its client closes the connection never
    runs, because Quarkus REST checks the connection before it invokes the method, so only the 20 requests already
    on a worker keep querying (a proxy that keeps the backend connection open would defeat this);
  - for under 5 s with virtual threads: every request starts at once and Agroal fails a wait for a connection
    after 5 s;
  - for more than 5 s with the reactive client: every request starts at once, and the wait queue of the reactive
    pool has no size limit, only a 30 s connection timeout (`max-wait-queue-size` and `connection-timeout` are
    configurable on Quarkus 4).

  Measured on 4.0.0.Beta1 (3.40.1 within 3 of each number, same tails):

  | Variant | answered | methods run | queries run | database busy after the last client left |
  |---|---|---|---|---|
  | worker | 92 | 313 | 314 | 0.8 s |
  | virtual | 74 | 500 | 382 | 3.1 s |
  | reactive | 75 | 500 | 425 | 8.6 s |

## The default pools are not the same size

Out of the box the JDBC pool has 50 connections and the reactive pool 20. With a 200 ms query that is 250 against
100 queries per second, and the benchmark shows the reactive variant failing at 200 requests per second while the
other two still serve them. That is the default configuration, not the model: with equal pools, `AbandonedQueryTest`
shows all three serving about the same number of requests; they differ in how long they keep working for clients
that left.

## Finding worth knowing

Run in isolation, the very first JDBC call on a virtual thread pinned the carrier once (JFR output):

```
Blocking Operation: Object.wait
Pinned Reason: Waited for initialization of sun.nio.ch.Poller by another thread
```

The first blocking socket read from a virtual thread makes the JDK initialise its I/O poller, and a virtual thread
waiting for that class initialisation is pinned, the same rule as any blocking static initializer. It happens once
per JVM, so `PinningTest` sends a warm-up request before asserting `@ShouldNotPin`. The REST Client module never
shows it, because its virtual threads `await()` the Vert.x client and never touch JDK sockets.

## Run

```shell
make dev MODULE=integrations/postgres      # Dev Services starts PostgreSQL
make test MODULE=integrations/postgres
make it MODULE=integrations/postgres
```
