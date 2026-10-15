# MiraVelo Bike Leasing: a designIQ demo

> [!TIP]
> **Presenting this demo?** Follow the [demo script](docs/demo/demo-script.md). It lists every step,
> the artifact to open, the key message and what the audience should see.

This repository shows how **designIQ** links business context, design decisions, architecture,
process models and feature issues for one application, using a single example that also runs.

The example is the **bike-leasing process of MiraVelo**, a fictional bike brand that leases its
bikes. The process is an executable BPMN model on an embedded [CIB seven](https://cibseven.org) engine
(the community fork of Camunda 7), in a Spring Boot service written in Kotlin. It is **deliberately
reduced to a core** so the whole story fits into one demo. The capabilities that were removed come
back as feature issues. **Other Miragon AI components are deliberately left out for now.**

Every design artifact is a versioned file under [`docs/`](docs/README.md), the folder designIQ reads.

![The core bike-leasing process](docs/assets/bike-leasing-core.png)

The process started out larger (see [the original process](docs/assets/bike-leasing-original.png)).
That state is frozen under the tag
[`original-process-v1`](https://github.com/miragon-blueprints/miragon-ai-example/tree/original-process-v1).
Copies of the original models and a
[scope comparison](docs/use-cases/bike-leasing/original/scope-comparison.md) are in
`docs/use-cases/bike-leasing/original/`.

## ✂️ Simplified scope

**The core (runs today):**

- A customer submits a leasing request for one bike over REST.
- **Check credit rating** (a DMN decision on age and monthly net income) either rejects the
  application or lets it continue.
- A solvent customer receives a contract and has **14 days** to sign it. Otherwise the application is
  rejected and the customer is notified.
- Once the contract is signed, the bike is **ordered from the dealer**.
- If the bike is unavailable, the **back office clarifies an alternative** with the customer (in the
  Tasklist or over REST). If they find one, the alternative is ordered. If not, the contract is
  reversed and the customer gets a cancellation confirmation.
- The dealer reports the **handover**. After the 14-day withdrawal period the **leasing is active**.
- If the dealer system is down, the order is retried and then raises an **incident** in Cockpit.

**Planned features**, taken from the original process and ordered by stage
([feature map](docs/use-cases/bike-leasing/feature-map/feature-map.md)):

| Stage | Feature issue |
|---|---|
| Stage 1 – Customer follow-up | [#1 Remind the customer to sign the contract after 7 days](https://github.com/miragon-blueprints/miragon-ai-example/issues/1) |
| Stage 2 – Insurance | [#2 Issue the insurance policy alongside the bike order](https://github.com/miragon-blueprints/miragon-ai-example/issues/2) |
| Stage 3 – Withdrawal | [#3 Let the customer withdraw the application and reverse completed steps](https://github.com/miragon-blueprints/miragon-ai-example/issues/3) |
| Stage 3 – Withdrawal | [#4 Cancel a placed bike order at the dealer](https://github.com/miragon-blueprints/miragon-ai-example/issues/4) (depends on #3) |
| Stage 4 – Portal and back-office views | [#5 Give the back office an inbox of pending alternative clarifications](https://github.com/miragon-blueprints/miragon-ai-example/issues/5) |
| Stage 4 – Portal and back-office views | [#6 List leasing applications for the customer portal](https://github.com/miragon-blueprints/miragon-ai-example/issues/6) |
| Stage 4 – Portal and back-office views | [#7 Offer a bike catalogue with models and live dealer availability](https://github.com/miragon-blueprints/miragon-ai-example/issues/7) |

**Open decisions** (make, buy or adapt; see the
[sourcing decisions](docs/architecture/sourcing-decisions.md)):

- [#8 Contract issuing and e-signature](https://github.com/miragon-blueprints/miragon-ai-example/issues/8)
- [#9 Channel for customer notifications](https://github.com/miragon-blueprints/miragon-ai-example/issues/9)
- [#10 How to integrate the bike dealer](https://github.com/miragon-blueprints/miragon-ai-example/issues/10)
- [#11 Back-office task UI: adapt the CIB seven Tasklist or build an own inbox](https://github.com/miragon-blueprints/miragon-ai-example/issues/11)

For each original capability, [scope-comparison.md](docs/use-cases/bike-leasing/original/scope-comparison.md)
says whether it is in the core or in an issue.

## 📖 Key terms

- **Customer**: the person who applies for a lease.
- **Back office**: MiraVelo staff who agree an alternative bike with the customer.
- **Dealer**: the bike dealer. Checks availability, takes the order and reports the handover
  (simulated here).
- **Leasing application**: the case for one request, identified by its application id. Its status is
  RECEIVED, ORDERED, HANDED_OVER, ACTIVE, REJECTED or CANCELLED.
- **Credit rating**: the automated `checkCreditRating` decision. A customer is solvent at an age of
  18 to 99 and a monthly net income of at least 1500.
- **Signature deadline**: the 14 days the customer has to sign the contract.
- **Alternative bike**: another bike the back office agrees with the customer when the requested one
  is unavailable.
- **Reversal**: undoing steps that are already done. In the core, that means revoking the contract
  when no alternative is found.
- **Handover**: the dealer hands the bike to the customer and reports it.
- **Withdrawal period**: the 14 days after the handover before the leasing becomes active.

All terms, with where each one appears in code and models: [docs/glossary.md](docs/glossary.md).

## 🧰 Prerequisites

- **JDK 21**
- **Docker** (or Podman) with Compose, for the PostgreSQL database
- **Node.js 22+**, for the Bruno scenarios and demo data, bpmnlint and the designIQ validator

## 🚀 Build, test, start

Run everything from the repo root.

```bash
./gradlew build                                    # all tests + model validation + OpenAPI export (uses H2, no Docker needed)
docker compose -f stack/docker-compose.yml up -d   # PostgreSQL on :5432
./gradlew bootRun                                  # the service and the embedded engine on :8081
```

| What | URL | Login |
|---|---|---|
| CIB seven Cockpit and Tasklist | <http://localhost:8081/camunda> | admin / admin |
| CIB seven webclient (new UI) | <http://localhost:8081/webapp> | admin / admin |
| Swagger UI (REST API) | <http://localhost:8081/swagger-ui.html> | |
| Health | <http://localhost:8081/actuator/health> | |

## 🎬 Demo data and reset

With the service running:

```bash
cd bruno && npx --yes @usebruno/cli@4.0.0 run 00-demo-data --env local && cd ..   # four demo cases + Tasklist filter
cd bruno && npx --yes @usebruno/cli@4.0.0 run . --env local -r && cd ..           # optional: every scenario end to end
```

`00-demo-data` leaves four cases in Cockpit: one waiting for the signature, one rejected by the credit
check, one waiting on *Clarify alternative with customer* in the Tasklist, and one waiting for the
handover. It also creates the Tasklist filter "All tasks"; without a filter the Tasklist shows "No
filter available" and no task. Running it again is safe. [bruno/README.md](bruno/README.md)
describes every scenario.

**Reset** to an empty engine:

1. Stop the service (Ctrl+C).
2. Run `docker compose -f stack/docker-compose.yml down -v` to delete the database.
3. Start again with `docker compose -f stack/docker-compose.yml up -d` and `./gradlew bootRun`, then
   load the demo data again. The reset also deletes the Tasklist filter, and `00-demo-data` creates
   it again.

## 🧭 designIQ

- **One folder.** The root [`designiq.yml`](designiq.yml) contains `models: docs`, so designIQ reads
  everything under [`docs/`](docs/README.md) and nothing else.
- **What it shows.** The BPMN models (the executable process, the t.BPM model, the feature map, the
  frozen original) open in the process modeler. `checkCreditRating.dmn` opens in the decision modeler
  and is linked from *Check credit rating*. `sourcing-map.owm` opens in the Wardley modeler. Markdown
  files are shown as text.
- **Todos.** Open issues labelled `todo` and `process:bike-leasing` show up as todos on the
  executable process. An anchor block at the top of each issue body pins it to the BPMN elements it
  affects. All eleven issues above are todos.
- **Main branch only.** designIQ reads the default branch. A change on a branch shows up after it is
  merged. Edits made in designIQ come back as a release pull request.
- **Connection.** The designIQ GitHub App must be installed for `miragon-blueprints/miragon-ai-example`.
  Without it, designIQ cannot see the repo. **Open on 2026-10-15:** the App was not installed and no
  designIQ workspace listed the repo, so the hosted designIQ steps of the demo are not rehearsed yet
  (see the [rehearsal log](docs/demo/rehearsal-log.md)). Until then, the demo script's local designIQ
  fallback and the PNG renders in [`docs/assets/`](docs/assets/) stand in.
- **Known limits.** Markdown, and with it the Mermaid C4 diagrams, appears as plain text (GitHub
  renders it). The designIQ validator wrongly flags the compensation handler *Cancel contract* as
  unreachable. The engine, bpmnlint and the model tests accept it. Details are in the
  [docs index](docs/README.md) and in [ADR-0016](docs/adr/0016-docs-is-the-designiq-workspace.md).
- **Validate** the models locally with `npx --yes @miragon/design-iq-validator@5.3.0 --root .`.

## 🗺️ Demo order

The [demo script](docs/demo/demo-script.md) tells the story in this order:

1. **Context and glossary**: the problem and the words for it.
   [Use case](docs/use-cases/bike-leasing/bike-leasing-use-case.md) · [glossary](docs/glossary.md)
2. **Wardley Map**: what MiraVelo builds, buys or adapts.
   [sourcing-map.owm](docs/architecture/sourcing-map.owm) · [sourcing decisions](docs/architecture/sourcing-decisions.md)
3. **t.BPM and feature map**: the process as a team lays it out on the table, and where each
   feature fits into it. [t.BPM model](docs/use-cases/bike-leasing/tbpm/bike-leasing-tbpm.bpmn) ·
   [feature map](docs/use-cases/bike-leasing/feature-map/bike-leasing-feature-map.bpmn) ·
   [feature table](docs/use-cases/bike-leasing/feature-map/feature-map.md)
4. **C4**: which systems carry the process.
   [System context](docs/architecture/c4-system-context.md) · [containers](docs/architecture/c4-container.md)
5. **Executable process**: what runs today, live in Cockpit.
   [bike-leasing.bpmn](docs/use-cases/bike-leasing/process/bike-leasing.bpmn) ·
   [checkCreditRating.dmn](docs/use-cases/bike-leasing/process/checkCreditRating.dmn) ·
   [how it is built](docs/use-cases/bike-leasing/process/executable-process.md)
6. **Feature issues**: from a todo on a process element to its GitHub issue.
   [Open todos](https://github.com/miragon-blueprints/miragon-ai-example/issues?q=is%3Aissue+is%3Aopen+label%3Atodo)

The steps that were actually tried in a rehearsal, and their limits, are in the
[rehearsal log](docs/demo/rehearsal-log.md).

## 📂 What's where

- [`designiq.yml`](designiq.yml): points designIQ at `docs/`
- [`docs/`](docs/README.md): every design artifact and the index to them
  - [`use-cases/bike-leasing/`](docs/use-cases/bike-leasing/bike-leasing-use-case.md): use case, t.BPM model,
    feature map, executable process (`process/`), frozen original (`original/`)
  - [`architecture/`](docs/architecture/sourcing-decisions.md): C4 diagrams, Wardley Map, sourcing decisions
  - [`adr/`](docs/adr/): Architecture Decision Records
  - [`demo/`](docs/demo/demo-script.md): demo script and rehearsal log
  - [`assets/`](docs/assets/): PNG renders of the models
- [`src/`](src/main/kotlin/io/miragon/blueprint/): the Spring Boot service in Kotlin, hexagonal
  ([AGENTS.md](AGENTS.md) explains the structure and its rules)
- [`bruno/`](bruno/README.md): REST scenarios and demo data
- [`openapi/`](openapi/openapi.json): the checked-in, drift-gated REST contract
- [`stack/`](stack/docker-compose.yml): PostgreSQL for local runs

## 🤝 Contributing

Contributions are welcome. Start from an issue, keep the CI gates green and use
[Conventional Commits](https://www.conventionalcommits.org). Setup, ports, ground rules and the PR
checklist are in [CONTRIBUTING.md](CONTRIBUTING.md). The working rules for humans and AI agents are
in [AGENTS.md](AGENTS.md).

## 📄 License

Licensed under the [MIT License](LICENSE).
