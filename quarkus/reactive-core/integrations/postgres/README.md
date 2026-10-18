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

## Finding worth knowing

Run in isolation, the very first JDBC call on a virtual thread pinned the carrier once:

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
