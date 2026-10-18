# demos

Runnable demos that accompany the articles at [blog.omatheusmesmo.dev](https://blog.omatheusmesmo.dev). Each demo
backs the claims of its articles with tests you can run, so every behaviour described in a post can be checked
on your machine.

## Demos

| Area | Demo | What it shows | Articles |
|---|---|---|---|
| Quarkus | [`quarkus/reactive-core`](quarkus/reactive-core) | Who runs your code in Quarkus: event loop, worker and virtual threads across REST, REST Client, PostgreSQL, Kafka, Redis and the Scheduler, proven by tests that assert the thread | Inside the Quarkus reactive core (series) |

## Layout

```
demos/
├── tooling/        reusable scripts, independent of any demo (benchmarks, Quarkus upgrades)
├── quarkus/        Quarkus demos
└── .github/        one workflow per demo, built on reusable workflows
```

Demos are grouped by area (`quarkus/`, `java/`, `ai/`, `cloud/`). An area folder appears with its first demo.
Each demo builds and runs from its own folder and has its own README.

To add a demo, read [CONTRIBUTING.md](CONTRIBUTING.md).
