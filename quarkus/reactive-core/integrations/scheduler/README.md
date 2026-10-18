# integrations/scheduler

Three jobs scheduled every second, one per model. The method signature picks the thread, as in Quarkus REST.

| Job | Signature | Thread |
|---|---|---|
| `worker` | `@Scheduled void` | worker |
| `event-loop` | `@Scheduled Uni<Void>` | event loop |
| `virtual` | `@Scheduled @RunOnVirtualThread void` | virtual thread |

`GET /jobs/{worker|event-loop|virtual}` returns how many times the job ran and the thread of its last run.

## What the tests prove

- Each job runs on the thread its signature promises.
- A virtual thread job does not pin.

## Pitfall met while writing it

`JobsResource` first read the job logs as fields of the injected `ScheduledJobs`. The jobs ran, but the resource
always saw zero executions: it holds a client proxy of the `@ApplicationScoped` bean, and a field read on the proxy
returns the proxy's own field. The logs are now read through methods, which the proxy delegates to the real bean.

## Run

```shell
make dev MODULE=integrations/scheduler
make test MODULE=integrations/scheduler
make it MODULE=integrations/scheduler
```
