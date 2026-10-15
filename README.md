# CIB seven Bike-Leasing Blueprint

> [!NOTE]
> **🚧 Work in progress.** A **solution template** to fork and build on — not a product that ships.
> Expect it to keep evolving.

A ready-to-fork **starting point** for automating a business process on
[CIB seven](https://cibseven.org) (the community fork of Camunda 7) with an **embedded engine** and
Spring Boot — one complete, runnable, production-shaped BPMN service.

## 🚲 The scenario

**MiraVelo** is a (fictional) bike brand that sells on a **leasing model**. This service automates the
**core** of a leasing application, from the first request to an active lease: credit check, contract
with a signature deadline, bike order with a clarification when the bike is out of stock, handover and
withdrawal period. Along the way it uses the BPMN elements you meet in real processes, not just a
happy-path service task:

![The bike-leasing process](docs/assets/bike-leasing.png)

- **message start event**, **service tasks** (JavaDelegates) and a **DMN business-rule task** for the
  credit check
- an **event-based gateway** that races the customer's signature against a **14-day timer**
- an **error boundary event** on the bike order and a **user task with a Camunda Form**, completable in
  the Tasklist or via REST, that loops back to a re-order
- a **compensation** handler that revokes the contract when no alternative bike is found
- a **task listener**, wired as a Spring bean like the delegates, and a **retry cycle** that turns a
  dealer outage into an incident

The executable model is [`docs/use-cases/bike-leasing/process/bike-leasing.bpmn`](docs/use-cases/bike-leasing/process/bike-leasing.bpmn).
The original, larger model (signature reminder, insurance policy, withdrawal, order cancellation) is
kept for reference in [`docs/use-cases/bike-leasing/original/`](docs/use-cases/bike-leasing/original/);
those capabilities come back as features.

## 🚀 Run it

You need **JDK 21** and **Docker** (or Podman).

**1. Start Postgres**

```bash
docker compose -f stack/docker-compose.yml up -d
```

**2. Start the service** on :8081

```bash
./gradlew bootRun
```

**3. Use it** — open the Cockpit / Tasklist at <http://localhost:8081/camunda> or the new CIB seven
webclient at <http://localhost:8081/webapp> (both admin/admin), the Swagger UI at
<http://localhost:8081/swagger-ui.html>, or drive the whole process over REST:

```bash
cd bruno && npx --yes @usebruno/cli@4.0.0 run . --env local -r
```

## 📂 What's where

- [`openapi/`](openapi/openapi.json) — the checked-in, drift-gated OpenAPI contract
- [`bruno/`](bruno/README.md) — the REST scenarios, the two ways to complete a user task, the incident demo
- [`stack/`](stack/docker-compose.yml) — the Postgres dev stack
- [`docs/`](docs/README.md) — the Architecture Decision Records: why the repo is shaped this way
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — setup, ports, containers and the PR workflow

## 🧰 Commands

Run them from the repo root. Postgres comes from `docker compose -f stack/docker-compose.yml up -d`.

| Task | Command |
|---|---|
| Run the service on :8081 | `./gradlew bootRun` |
| Full build (arch + unit + process + model validation + spec export) | `./gradlew build` |
| Mutation testing (gate 80) | `./gradlew pitest` |
| Regenerate the typed process API after editing a `.bpmn` | `./gradlew generateBpmnModels` |
| Build the OCI image | `./gradlew bootBuildImage` |

## 📂 Layout

```
src/main/kotlin/io/miragon/blueprint/   the service (one Gradle project at the repo root)
  adapter/inbound/rest        REST controllers + OpenAPI / problem-details config
  adapter/inbound/cibseven    JavaDelegates + listeners for the BPMN service tasks
  adapter/outbound/cibseven   drives the engine (RuntimeService / TaskService)
  adapter/outbound/db         JPA persistence (leasing applications)
  adapter/outbound/…          simulated dealer / contract / notification adapters
  adapter/process             generated *ProcessApi (bpmn-to-code) + engine config
  application/{port,service}  use-case ports and their services
  domain/{leasing,bike}       pure domain model
src/main/resources/forms      the Camunda Forms
src/main/resources/db/migration   Flyway versioned schema migrations
src/test/kotlin/io/miragon/common/architecture/   reusable ArchUnit + Konsist rule suite
docs/use-cases/bike-leasing/process/   the executable BPMN + DMN models (packaged onto the classpath)
```

## 🧱 How it is built

- **Hexagonal architecture.** Domain and use cases never depend on CIB seven, so the business logic is
  testable and the engine replaceable. The `io.miragon.common.architecture` test suite enforces it
  with **ArchUnit** (bytecode: layering, dependency direction, naming) and **Konsist** (source: one
  declaration per file, no wildcard imports); a service opts in with
  `class ArchitectureTest : ServiceArchitectureTest(...)`.
- **Generated process API.** The `bpmn-to-code` Gradle plugin turns each `.bpmn` into a typed,
  node-centric `*ProcessApi` object, so element ids, messages, timers, variables and the paths the
  process tests walk are compile-checked. Never hand-edit `adapter/process`.
- **Unit tests** (JUnit 5 + MockK) cover every domain type, service and adapter — controllers via
  `@WebMvcTest`, persistence via `@DataJpaTest`.
- **Process tests** (`cibseven-bpm-assert`) drive the deployed model deterministically: timers and async
  continuations are fired and messages correlated by hand, and the walked path is asserted as a
  compile-checked `ProcessPath`.
- **Model validation** (`bpmn-to-code-testing`) checks the models at build time, including a custom rule
  that every service task uses a delegate expression.
- **Mutation testing** (PIT, gate 80) grades assertion strength — diff-scoped on pull requests, full
  sweep nightly.
- **OpenAPI contract.** A test exports the springdoc spec to [`openapi/openapi.json`](openapi/openapi.json);
  CI fails on drift.

## 🤝 Contributing

Contributions are welcome. Open an issue before a substantial change, keep the CI gates green and use
[Conventional Commits](https://www.conventionalcommits.org). The details are in
[`CONTRIBUTING.md`](CONTRIBUTING.md).

## 📄 License

Licensed under the [MIT License](./LICENSE).
