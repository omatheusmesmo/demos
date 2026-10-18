# demos

Runnable demos that accompany the articles at [blog.omatheusmesmo.dev](https://blog.omatheusmesmo.dev). Each demo
backs the claims of its articles with tests you can run, so every behaviour described in a post can be checked
on your machine.

## Demos

| Area | Demo | What it shows | Articles |
|---|---|---|---|
| Quarkus | [`quarkus/reactive-core`](quarkus/reactive-core) | Who runs your code in Quarkus: event loop, worker and virtual threads across REST, REST Client, PostgreSQL, Kafka, Redis and the Scheduler, proven by tests that assert the thread | Inside the Quarkus reactive core (series) |
| Quarkus | [`quarkus/intro`](quarkus/intro) (submodule) | A first Quarkus application: CRUD API and presentation slides | |
| Quarkus | [`quarkus/cloud-native`](quarkus/cloud-native) (submodule) | Native executables on Knative: cold start, memory and a load benchmark | [Quarkus native on Knative](https://blog.omatheusmesmo.dev/en/posts/quarkus-native-knative-cold-start/) |
| Quarkus LangChain4j | [`quarkus-langchain4j/onthinking`](quarkus-langchain4j/onthinking) (submodule) | `@OnThinking` in a customer support bot | |
| Quarkus LangChain4j | [`quarkus-langchain4j/incident-response-sre`](quarkus-langchain4j/incident-response-sre) (submodule) | An SRE incident response agent: ChatModel, agentic and Quarkus Flow layers | |
| Cloud | [`cloud/floci-aws`](cloud/floci-aws) (submodule) | Local AWS for Quarkus through Floci and Dev Services | |

## Layout

```
demos/
├── tooling/        reusable scripts, independent of any demo (benchmarks, Quarkus upgrades)
├── quarkus/              Quarkus demos
├── quarkus-langchain4j/  Quarkus LangChain4j demos
├── cloud/                cloud provider demos
└── .github/              one workflow per in-tree demo, built on reusable workflows
```

Demos are grouped by area, named after their domain. An area folder appears with its first demo. Each demo builds
and runs from its own folder and has its own README.

Some demos live in their own repository and appear here as git submodules. Clone everything with:

```shell
git clone --recurse-submodules https://github.com/omatheusmesmo/demos.git
```

or, in an existing clone, `git submodule update --init --recursive`.

To add a demo, read [CONTRIBUTING.md](CONTRIBUTING.md).
