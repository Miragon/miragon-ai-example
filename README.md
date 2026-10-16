# MiraVelo bike leasing

[![Pre-Merge Checks](https://github.com/miragon-blueprints/miragon-ai-example/actions/workflows/pre-merge.yml/badge.svg)](https://github.com/miragon-blueprints/miragon-ai-example/actions/workflows/pre-merge.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An example of how we at Miragon automate a business process when AI agents do much of the work:
one BPMN model that runs on an embedded [CIB seven](https://cibseven.org) engine, a Kotlin service
and a React frontend around it, and the design artifacts that explain it, all in one repository.
Guardrails decide what a change may do, and the open features are issues anchored to the model.

![The frontend: the back-office inbox with an open credit check](docs/assets/frontend-inbox.png)

## Why

An agent can write a delegate, a controller or a React page in seconds. Whether the result is right
depends on what the repository tells it, and on what the repository refuses. This example shows the
setup we use for that:

- **The model is the source.** The executable BPMN lives next to the use case and the glossary. Gradle
  generates a typed process API from it, so code that references a removed element no longer compiles.
- **The rules are machine-checked.** ArchUnit and Konsist enforce the hexagonal architecture,
  mutation testing (gate 80) catches tests that assert nothing, bpmnlint checks every model, and the
  OpenAPI contract and the generated frontend client are drift-gated in CI.
- **One file of instructions.** [AGENTS.md](AGENTS.md) is what humans and agents read first;
  `.claude/` adds skills for recurring work and review agents.
- **Work is anchored to the process.** Every feature issue starts with an anchor block that pins it
  to BPMN elements, so [designIQ](docs/README.md) shows it as a todo on the model.

## The process

![The executable bike-leasing process](docs/assets/bike-leasing-core.png)

A customer applies to lease a bike. The application is validated, the back office checks the credit
rating, and a solvent customer receives the contract. Once it is signed, the insurance policy is
issued and the bike is ordered from the dealer in parallel. When the dealer reports the handover,
the leasing is active. The customer can withdraw at any time; the back office then reverses the
application.

The model is stage 3 of the [MiraVelo reference models](https://github.com/Miragon/miravelo-reference)
(`03-bike-leasing-event-subprocess-simplified.bpmn`). It deliberately leaves out what the
[original process](docs/use-cases/bike-leasing/original/scope-comparison.md) could do. That gap is
the roadmap below.

## Quick start

Requires JDK 21, Node 24 and Docker or Podman.

```bash
docker compose -f stack/docker-compose.yml up -d   # PostgreSQL on :5432
./gradlew bootRun                                  # service + embedded engine on :8081
npm --prefix frontend ci && npm --prefix frontend run dev   # frontend on :5173
```

| What | URL | Login |
|---|---|---|
| Frontend (applications, new application, back-office inbox) | <http://localhost:5173> | |
| CIB seven Cockpit and Tasklist | <http://localhost:8081/camunda> | admin / admin |
| Swagger UI | <http://localhost:8081/swagger-ui.html> | |

Demo cases in every state: `cd bruno && npx --yes @usebruno/cli@4.0.0 run 00-demo-data --env local`.
Everything else (tests, containers, reset) is in [CONTRIBUTING.md](CONTRIBUTING.md).

## What's inside

| Path | Content |
|---|---|
| [`src/`](src/main/kotlin/io/miragon/blueprint/) | Spring Boot 4 service in Kotlin, hexagonal, with JavaDelegates on the embedded engine |
| [`frontend/`](frontend/AGENTS.md) | React app (Vite, TanStack, Tailwind) with a client generated from the OpenAPI contract |
| [`docs/`](docs/README.md) | the designIQ workspace: use case, glossary, t.BPM, feature map, the executable model, C4, Wardley Map, ADRs |
| [`openapi/`](openapi/openapi.json) | the checked-in REST contract |
| [`bruno/`](bruno/README.md) | REST scenarios and demo data |
| [`.claude/`](.claude/README.md) | skills and review agents for Claude Code |

## Roadmap

What the original process could do and this one cannot yet, ordered by stage
([feature map](docs/use-cases/bike-leasing/feature-map/feature-map.md)):

| Stage | Feature |
|---|---|
| 1 – Customer follow-up | [#18 Reject the application when the contract stays unsigned for 14 days](https://github.com/miragon-blueprints/miragon-ai-example/issues/18) |
| 1 – Customer follow-up | [#1 Remind the customer to sign the contract after 7 days](https://github.com/miragon-blueprints/miragon-ai-example/issues/1) |
| 2 – Automation and supply | [#19 Automate the credit check with the DMN checkCreditRating](https://github.com/miragon-blueprints/miragon-ai-example/issues/19) |
| 2 – Automation and supply | [#20 Clarify an alternative bike when the dealer cannot deliver](https://github.com/miragon-blueprints/miragon-ai-example/issues/20) |
| 3 – Withdrawal | [#21 Activate the leasing only after the 14-day withdrawal period](https://github.com/miragon-blueprints/miragon-ai-example/issues/21) |
| 3 – Withdrawal | [#3 Reverse a withdrawn application automatically instead of by hand](https://github.com/miragon-blueprints/miragon-ai-example/issues/3) |
| 3 – Withdrawal | [#4 Cancel a placed bike order at the dealer](https://github.com/miragon-blueprints/miragon-ai-example/issues/4) |
| 4 – Portal | [#7 Show live dealer availability in the bike catalogue](https://github.com/miragon-blueprints/miragon-ai-example/issues/7) |

Open sourcing decisions (make, buy or adapt; [sourcing decisions](docs/architecture/sourcing-decisions.md)):
[#8 contract and e-signature](https://github.com/miragon-blueprints/miragon-ai-example/issues/8),
[#9 customer notifications](https://github.com/miragon-blueprints/miragon-ai-example/issues/9),
[#10 dealer integration](https://github.com/miragon-blueprints/miragon-ai-example/issues/10).

## Documentation

- [AGENTS.md](AGENTS.md): structure, rules, commands and how to implement a feature issue
- [docs/README.md](docs/README.md): every design artifact, the demo order and the ADR list
- [Executable process](docs/use-cases/bike-leasing/process/executable-process.md): element ids,
  what differs from the reference model, how a model change reaches the code
- [Demo script](docs/demo/demo-script.md): the live demo, step by step
- [frontend/AGENTS.md](frontend/AGENTS.md): the frontend's structure and rules

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE) © Miragon GmbH.
