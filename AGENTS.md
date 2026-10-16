# AGENTS.md

Guidance for AI agents (and humans) working in this repo. This is the real file; `CLAUDE.md` just
imports it (see [ADR-0005](docs/adr/0005-agents-md-as-the-single-source.md)).

## Project Overview

An example of Miragon's way of automating a process with AI agents: the **MiraVelo bike-leasing**
process as an executable BPMN model on an embedded CIB seven engine, behind an enforced hexagonal
architecture, with a React frontend for customers and the back office. The process is the reference
model `03-bike-leasing-event-subprocess-simplified` from
[Miragon/miravelo-reference](https://github.com/Miragon/miravelo-reference)
([ADR-0017](docs/adr/0017-rebase-onto-the-simplified-event-subprocess-process.md)). The design
artifacts that explain it (use case, glossary, t.BPM model, feature map, C4, Wardley Map, ADRs) are
versioned under `docs/`, the one folder designIQ reads. What the original process could do and this
one cannot yet is GitHub issues.

- **Backend**: a single Kotlin project built with Gradle at the repo root. It uses Spring Boot 4, a
  hexagonal structure and the embedded CIB seven 2.2 engine. Service tasks call Spring-bean
  `JavaDelegate`s by expression (the engine's interface, implemented in Kotlin), **not** external
  task workers. Package root `io.miragon.blueprint`.
- **Frontend**: `frontend/`, a separate npm project (React, Vite, TanStack Router/Query, Tailwind,
  Feature-Sliced Design), ported from
  [miragon-blueprints/fullstack-example](https://github.com/miragon-blueprints/fullstack-example)
  ([ADR-0018](docs/adr/0018-react-frontend-next-to-the-service.md)). Its rules are in
  [frontend/AGENTS.md](frontend/AGENTS.md). The back office works both user tasks in the frontend
  inbox, not in the Tasklist ([ADR-0019](docs/adr/0019-back-office-task-ui-own-inbox-in-the-frontend.md)).
- **The contract** is `openapi/openapi.json`. springdoc generates it from the controllers, it is
  **committed and drift-gated** in CI, and orval generates the frontend client from it
  ([ADR-0003](docs/adr/0003-openapi-as-the-checked-in-contract.md)). A REST change means: re-export
  the spec, then `npm run api:generate` in `frontend/`.
- **The design artifacts** live in `docs/` (`designiq.yml`: `models: docs`). The executable model
  lives there too, next to the use case, and Gradle packages it onto the classpath. See
  [ADR-0014](docs/adr/0014-one-kotlin-gradle-stack-at-the-repo-root.md) (one Kotlin/Gradle project)
  and [ADR-0016](docs/adr/0016-docs-is-the-designiq-workspace.md) (`docs/` as the designIQ workspace).

## Repository Map

The service, a single Gradle project at the repo root (`build.gradle.kts`, `settings.gradle.kts`,
`gradle/`):

```
src/main/kotlin/io/miragon/blueprint/   the bike-leasing service (hexagonal)
  adapter/inbound/rest        REST controllers + OpenAPI / problem-details config
  adapter/inbound/cibseven    JavaDelegates for the service tasks
  adapter/outbound/cibseven   drives the engine: start, correlate, complete and list user tasks
  adapter/outbound/db         JPA persistence (leasing applications)
  adapter/outbound/…          simulated contract / insurance / notification / dealer adapters
  adapter/process             generated *ProcessApi (bpmn-to-code) + engine config
  application/{port,service}  use-case ports and their services
  domain/{leasing,bike}       pure domain model
src/main/resources/db/migration   Flyway versioned schema migrations
src/test/kotlin/io/miragon/blueprint/          tests (architecture, domain, services, adapters, process)
src/test/kotlin/io/miragon/common/architecture/   reusable ArchUnit + Konsist rule suite
```

The frontend (`frontend/`, npm, see [frontend/AGENTS.md](frontend/AGENTS.md)):

```
frontend/src/app        router, routes, styles
frontend/src/pages      application-list, application-detail, submit-application, task-inbox
frontend/src/widgets    app-shell, application table/summary/actions, leasing-progress, task-inbox-list
frontend/src/features   one write each: submit, sign, handover, withdraw, credit check, reversal
frontend/src/entities   reads: leasing-application, bike, user-task
frontend/src/shared     generated API client (orval), ui kit, i18n (de/en)
frontend/e2e            Playwright scenarios against the running stack
```

The designIQ folder:

```
docs/
  README.md                   index of every artifact, the demo order and the ADR list
                              (the only README.md under docs/)
  glossary.md                 domain terms, named as in code and models
  demo/                       demo-script.md, rehearsal-log.md
  use-cases/bike-leasing/
    bike-leasing-use-case.md  the use case: problem, actors, core scope vs. features
    tbpm/                     bike-leasing-tbpm.bpmn: t.BPM table-top model of the core
    feature-map/              bike-leasing-feature-map.bpmn + feature-map.md: features on the process
    process/                  EXECUTABLE: bike-leasing.bpmn,
                              executable-process.md (how it is built, element ids)
    original/                 FROZEN original models + checkCreditRating.dmn + scope-comparison.md
  architecture/               c4-system-context.md, c4-container.md (Mermaid),
                              sourcing-map.owm (Wardley Map), sourcing-decisions.md
  adr/                        Architecture Decision Records
  assets/                     PNG renders, the fallback when designIQ is unavailable
```

Around it:

```
designiq.yml                  models: docs (designIQ reads exactly this folder, on main only)
bruno/                        REST scenarios + 00-demo-data (demo cases for a live demo)
openapi/                      the checked-in, drift-gated OpenAPI contract (openapi.json)
stack/                        Postgres dev stack (docker compose)
.github/                      pre-merge + nightly pipelines + Dependabot
.githooks/                    pre-commit hook (bpmnlint on staged .bpmn, lint-staged in frontend/)
.claude/                      Claude Code skills and review agents (see .claude/README.md)
package.json / .bpmnlintrc    bpmnlint config + git-hook installer (npm run lint:bpmn)
```

## Development Setup

Three commands to a running stack: Postgres, the backend + engine on :8081, the frontend on :5173.

```bash
docker compose -f stack/docker-compose.yml up -d
```

```bash
./gradlew bootRun
```

```bash
npm --prefix frontend ci && npm --prefix frontend run dev
```

### Ports (one source of truth: keep README, CONTRIBUTING, this file and `.conductor/settings.toml` in sync)

| What | Port |
|---|---|
| Frontend (Vite dev server, proxies `/api`, `/v3/api-docs`, `/engine-rest`, `/camunda` to 8081) | 5173 |
| Frontend in a container (nginx, `stack/docker-compose.full.yml`) | 8090 |
| Postgres | 5432 |
| Backend (REST + `/engine-rest`) | 8081 |
| CIB seven Cockpit / Tasklist / Admin | 8081/camunda (admin/admin) |
| CIB seven webclient (new UI) | 8081/webapp (admin/admin) |
| OpenAPI spec · Swagger UI | 8081/v3/api-docs · 8081/swagger-ui.html |
| Actuator (health/liveness/readiness · prometheus) | 8081/actuator |

Under Conductor the ports are fixed and the workspace runs `nonconcurrent`
(see [ADR-0006](docs/adr/0006-fixed-ports-for-v1-portless-as-the-upgrade.md)). Keep this table,
`frontend/vite.config.ts` and `frontend/playwright.config.ts` in sync.

## Build Commands

All from the repo root:

| Area | Command |
|---|---|
| Backend (arch + unit + process + model validation + spec export) | `./gradlew build` |
| Mutation testing (gate 80) | `./gradlew pitest` |
| Regenerate the typed BPMN process API (after editing a `.bpmn`) | `./gradlew generateBpmnModels` |
| Regenerate the OpenAPI contract | `./gradlew test --tests "io.miragon.blueprint.openapi.OpenApiSpecExportTest"` |
| Verify the OpenAPI contract after regenerating it | `git diff --exit-code openapi/openapi.json` |
| Backend OCI image (`miravelo/cibseven-embedded-example`) | `./gradlew bootBuildImage` |
| Frontend gates (format, lint, FSD, dead code, types, unit tests) | `npm --prefix frontend run verify` |
| Regenerate the frontend API client after a contract change | `npm --prefix frontend run api:generate` |
| Check the generated client against the contract | `npm --prefix frontend run api:check` |
| Frontend end-to-end (running stack + dev server) | `npm --prefix frontend run e2e` |
| BPMN lint (executable models) | `npm run lint:bpmn` |
| designIQ validation (all models under `docs/`) | `npx --yes @miragon/design-iq-validator@5.3.0 --root .` |
| designIQ validation (one model, id = file stem) | `npx --yes @miragon/design-iq-validator@5.3.0 --root . <model-id>` |
| Demo data (running stack) | `cd bruno && npx --yes @usebruno/cli@4.0.0 run 00-demo-data --env local` |
| API scenarios (running stack) | `cd bruno && npx --yes @usebruno/cli@4.0.0 run . --env local -r` |

The OCI image decision is [ADR-0011](docs/adr/0011-build-and-deployment-approach.md); the how-to is
CONTRIBUTING "Run it in containers".

## Process Models

- **Executable models live in `docs/use-cases/bike-leasing/process/`** (today only
  `bike-leasing.bpmn`). Gradle packages them onto the classpath as `bpmn/*.bpmn` and `dmn/*.dmn`.
  Edit them there and nowhere else; `src/main/resources` holds no `.bpmn` or `.dmn` files.
- **The executable model derives from the reference model** `03-bike-leasing-event-subprocess-simplified`.
  Every difference is listed in `executable-process.md`; add a row when you add one.
- **After editing a `.bpmn`**, run `./gradlew generateBpmnModels` and commit the regenerated
  `adapter/process` files. Then run `npm run lint:bpmn` and the `process.*` tests.
- **Keep element ids stable.** The issues' anchor blocks, the feature map, `executable-process.md` and
  the glossary refer to them. If you rename or remove an element, update every reference in the same
  change.
- **A DMN's file stem is its decision id** (`checkCreditRating`). That is how designIQ links a
  business rule task to the decision, so never rename one without the other. The DMN is not
  deployed today; it waits in `original/` for its feature issue.
- **`docs/use-cases/bike-leasing/original/` is frozen.** It holds reference copies of the tag
  `original-process-v1` (the target picture for the roadmap), is not deployed and is never edited.
- **The t.BPM model and the feature map are documentation**, not deployed: Gradle packages only
  `process/`.

## designIQ Rules

- **One folder.** designIQ reads only `docs/`, and only on the default branch. Every `.md` under
  `docs/` becomes a model.
- **File stems are unique per notation under `docs/`.** No second `README.md`, no two `.bpmn` with
  the same stem. designIQ silently skips a duplicate. Keep stems unique across notations too (the
  use case is `bike-leasing-use-case.md`, not `bike-leasing.md`), so one model id names one file.
- **Complete BPMNDI.** Every flow node, sequence flow, pool, lane and group needs its DI shape or edge,
  or the designIQ editor breaks.
- **t.BPM files keep workshop mode.** `bike-leasing-tbpm.bpmn` and `bike-leasing-feature-map.bpmn`
  keep `xmlns:bpmiq="https://bpmiq.io/schema/1.0/bpmiq"` and `bpmiq:mode="workshop"` on
  `bpmn:definitions`. Stickies are `<bpmiq:sticky id text x y kind width height/>` (kind `note`,
  `question`, `decision` or `role`) inside `bpmn:process/bpmn:extensionElements`. Feature clusters are
  `bpmn:group`s whose `categoryValue` has a `value`, so the group shows a label.
- **Plain-text Markdown.** designIQ shows Markdown and Mermaid as raw text. Keep docs readable without
  rendering, and refresh the PNG in `docs/assets/` when a model's picture changes.
- **Validate** every changed model with the validator (see Build Commands), or with the dry-run
  `validate_model` MCP tool. The executable model validates without errors or warnings; keep it
  that way. The frozen original shows 9 known errors (unreachable / dead-end compensation handlers
  and the call activity) and 2 warnings; a change must add no other finding.
- **Todos are GitHub issues** labelled `todo` and `process:bike-leasing`, with this anchor block at the
  very top of the body (one `element:` line per element; none for a process-level todo):

  ```
  <!-- bpmiq:todo v1
  process: bike-leasing
  file: docs/use-cases/bike-leasing/process/bike-leasing.bpmn
  element: <element id> | <element name>
  -->
  ```

  `process` is the file stem, not the BPMN process id (`bikeLeasingProcess`). The element ids must
  exist in the executable model. The marker `bpmiq:todo v1` is frozen: never rewrite it.
- **Live workspace.** designIQ save, release and todo tools write to the shared live workspace or to
  GitHub. Use them only when the user asks. Edits made in designIQ come back as release pull
  requests: review them like any other PR.

## Features and Decisions

| # | Feature or decision | Stage |
|---|---|---|
| [#18](https://github.com/miragon-blueprints/miragon-ai-example/issues/18) | Signature deadline (14 days) | Stage 1 – Customer follow-up |
| [#1](https://github.com/miragon-blueprints/miragon-ai-example/issues/1) | Signature reminder (depends on #18) | Stage 1 – Customer follow-up |
| [#19](https://github.com/miragon-blueprints/miragon-ai-example/issues/19) | Credit check by DMN | Stage 2 – Automation and supply |
| [#20](https://github.com/miragon-blueprints/miragon-ai-example/issues/20) | Alternative bike when unavailable | Stage 2 – Automation and supply |
| [#21](https://github.com/miragon-blueprints/miragon-ai-example/issues/21) | Withdrawal period before activation | Stage 3 – Withdrawal |
| [#3](https://github.com/miragon-blueprints/miragon-ai-example/issues/3) | Automatic reversal (compensation) | Stage 3 – Withdrawal |
| [#4](https://github.com/miragon-blueprints/miragon-ai-example/issues/4) | Bike order cancellation (depends on #3) | Stage 3 – Withdrawal |
| [#7](https://github.com/miragon-blueprints/miragon-ai-example/issues/7) | Live dealer availability | Stage 4 – Portal |
| [#8](https://github.com/miragon-blueprints/miragon-ai-example/issues/8) | Decision: contract & e-signature | – |
| [#9](https://github.com/miragon-blueprints/miragon-ai-example/issues/9) | Decision: customer notification | – |
| [#10](https://github.com/miragon-blueprints/miragon-ai-example/issues/10) | Decision: dealer integration | – |

Delivered by the rebase, to be closed with its merge: #2 (insurance policy), #5 (back-office inbox),
#6 (application list) and the decision #11 (task UI, [ADR-0019](docs/adr/0019-back-office-task-ui-own-inbox-in-the-frontend.md)).

- **Implementing a feature** means following its acceptance criteria through every layer: model,
  regenerated process API, code, tests, `openapi.json`, the regenerated frontend client and the UI
  where the feature is visible, Bruno and Playwright. In the same PR, update the feature map
  (`bike-leasing-feature-map.bpmn` and `feature-map.md`), the glossary and, if elements change,
  `executable-process.md`. Close the issue from the PR (`Closes #n`).
- **Taking a decision (#8–#10)** means writing a new ADR in `docs/adr/` (copy the template), updating
  `sourcing-map.owm` and `sourcing-decisions.md`, and closing the issue.
- **Derive, never invent.** A feature's details come from its issue and the original model in
  `original/`. Mark anything else as an assumption.

## Architecture: the rules are machine-enforced

The backend's hexagonal rules live in `src/test/kotlin/io/miragon/common/architecture` (ArchUnit,
plus Konsist for the rules bytecode cannot express) and **fail the build** (see
[ADR-0007](docs/adr/0007-two-architecture-test-tools-archunit-and-konsist.md)). Read
`HexagonalArchitectureTest.kt` and `NamingConventionArchitectureTest.kt` before writing code. The hard
rules:

- **One inbound port per controller.** `onlyFulfilOneUseCase` counts constructor params in
  `application.port.inbound` and fails at >1. A read and a completion are two controllers.
- **No new top-level `config` package.** The containment rule ignores only *direct* members of the
  root package, so `io.miragon.blueprint.config` would fail. Cross-cutting `@Configuration` (OpenAPI,
  error handling) goes in `adapter.inbound.rest`, where the `Configuration` suffix is whitelisted.
- **`adapter/process` is generated.** Never hand-edit `*ProcessApi` or the shared
  `ServiceTasks`/`Messages`/`ProcessVariables`/`Errors` files (and `Escalations` once a model defines
  one); edit the `.bpmn` and regenerate.
- **Suffixes:** inbound port `UseCase|Query`; outbound `Port|Repository|Process`; service
  `Service|Configuration`; `adapter.inbound.rest` `Controller|Dto|Input|Mapper|Configuration`;
  `adapter.inbound.cibseven` `Delegate|Worker|Listener`; `adapter.outbound`
  `PersistenceAdapter|Adapter|Mapper|Entity|Repository`.
- **Spring Data types stop at the adapter.** Ports own their own `Filter`/`Page`/`Criteria` types.

## BPMN Quality Gates

- `bpmn-to-code` generates typed process constants from the models at build time. A custom model
  test requires every service task to use a delegate expression (`#{beanName}`).
- Since bpmn-to-code 6 the API is node-centric: `<Process>ProcessApi.FlowNodes.<Node>` carries the
  element (`.id`, `ELEMENT_ID`), its `Variables` and its successors (`Next`). Process tests assert
  the walked path as a compile-checked `ProcessPath` (`process/util/ProcessPathAssertions.kt`)
  instead of hand-maintained element-id lists.
- `bpmnlint` checks `docs/use-cases/bike-leasing/process/*.bpmn` in CI and on staged `.bpmn` via
  `.githooks/pre-commit` (install: `npm run hooks:install`).
- The designIQ validator checks every model under `docs/` (see designIQ Rules). It runs locally, not
  in CI.

## Testing

TDD. Match the test style to the layer:

| Layer | Test style |
|---|---|
| domain | plain unit tests |
| application service | unit tests with mocked ports (MockK) |
| `adapter.inbound.rest` | `@WebMvcTest` with the use case mocked |
| `adapter.outbound.db` | `@DataJpaTest` |
| process end-to-end | CIB seven process tests, paths asserted via `ProcessPath` |
| frontend units, widgets, pages | Vitest + Testing Library, API mocked with the generated MSW handlers |
| whole stack | Bruno (REST) and Playwright (UI) against the running service |

**Mutation testing gates PRs at 80**: a test that executes without asserting will fail CI. Coverage
says a line ran; mutation says a test would have noticed. The PR gate runs **diff-scoped** (only the
classes the PR changed, still blocking); the **full-module** gate-80 sweep runs nightly. See
[ADR-0004](docs/adr/0004-mutation-testing-as-a-blocking-pr-gate.md).

## Verify After Each Task (targeted, not a full build)

From the repo root:

- Backend service/controller: `./gradlew test --tests "*<Name>Test"`
- Architecture only: `./gradlew test --tests "io.miragon.blueprint.architecture.*"`
- Contract changed: regenerate the spec, then `git diff --exit-code openapi/openapi.json`
- Process changed: `./gradlew generateBpmnModels`, `npm run lint:bpmn`, then
  `./gradlew test --tests "io.miragon.blueprint.process.*"`
- Design artifact changed: `npx --yes @miragon/design-iq-validator@5.3.0 --root . <model-id>`
- Frontend changed: `npm --prefix frontend run verify`; after a contract change also
  `npm --prefix frontend run api:generate` and `npm --prefix frontend run build`

## Skills and Review Agents

`.claude/` holds Claude Code skills for recurring work (automate a process step, add a delegate, a
REST controller, a persistence adapter or a frontend feature slice, sync the API client, write an
ADR, check a model visually) and two review agents (`review-process`, `review-vertical-slice`).
[.claude/README.md](.claude/README.md) lists them. Use a review agent before you open a PR that
changes the model or a vertical slice.

## Working with GitHub

Use the `gh` CLI. Write everything (issues, PRs, commit messages, docs, models) in **English**. Use
**Conventional Commits** (`feat:`, `fix:`, `test:`, `chore:`, `docs:`, `ci:`, `build:`). When you edit
a feature or decision issue, keep its designIQ anchor block at the top of the body.

## ADRs

Architecture decisions are recorded in `docs/adr/`. Read them to understand *why* the repo is shaped
this way before proposing structural changes. An accepted ADR is not rewritten. A reversal gets a new
ADR that supersedes the old one. A later change that only makes a detail untrue adds a dated
"Amended by" note under the status (see ADR-0002 for the form); the body stays as it was.

## Personality

You are a knowledgeable colleague, not someone who passively takes orders. If something proposed
doesn't look right, suggest corrections, ask critical questions, and push back where needed.
Challenge ideas that could benefit from further improvement or iterative refinement rather than just
accepting them at face value.
