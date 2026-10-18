# AGENTS.md

Guidance for AI agents (and humans) working in this repo. Agent instructions live only in `AGENTS.md`
files: this one, [service/AGENTS.md](service/AGENTS.md) and [frontend/AGENTS.md](frontend/AGENTS.md).
There is no `CLAUDE.md`; Claude Code reads these files natively (ADR-0001).

## Project Overview

An example of Miragon's way of automating a process with AI agents: the **MiraVelo bike-leasing**
process as an executable BPMN model on an embedded CIB seven engine, behind an enforced hexagonal
architecture, with a React frontend for customers and the back office. What the process covers and
what it leaves out is in the [use case](docs/use-cases/bike-leasing/bike-leasing-use-case.md) and the
[feature map](docs/use-cases/bike-leasing/feature-map/feature-map.md); the missing features are
GitHub issues (see the [README roadmap](README.md#roadmap)).

- **Backend** (`service/`, Gradle, Kotlin, Spring Boot 4, CIB seven 2.2): `service/app` is the service,
  `service/common-architecture-tests` the reusable rule suite. Service tasks call Spring-bean
  `JavaDelegate`s by expression, not external task workers. Package root `io.miragon.blueprint`.
- **Frontend** (`frontend/`, npm, React, Vite, TanStack, Tailwind, Feature-Sliced Design). The back
  office works both user tasks in the frontend inbox, not in the Tasklist.
- **The contract** is `openapi/openapi.json`: generated from the controllers, committed, drift-gated in
  CI; orval generates the frontend client from it.
- **The design artifacts** live in `docs/`, the one folder designIQ reads, next to the executable model.

## Repository Map

```
service/                      the backend (see service/AGENTS.md)
frontend/                     the frontend (see frontend/AGENTS.md)
openapi/openapi.json          the checked-in, drift-gated REST contract
docs/                         designIQ workspace: README.md (index + ADR list), glossary.md
  use-cases/bike-leasing/     use case, tbpm/, feature-map/, process/ (EXECUTABLE)
  architecture/               C4, Wardley Map, sourcing decisions
  adr/                        Architecture Decision Records
bruno/                        REST scenarios + 00-demo-data
stack/                        Postgres dev stack, full container stack
.github/                      pre-merge + nightly pipelines, Dependabot
.githooks/                    pre-commit: bpmnlint on staged .bpmn, lint-staged in frontend/
.claude/                      skills, review agents, settings, plugins and hooks (see .claude/README.md)
.operate.json                 operate's engine profile: the local engine on :8081
```

Setup, ports and the full command list are in [CONTRIBUTING.md](CONTRIBUTING.md).

## Hard Rules

- **Generated files are never hand-edited:** `service/app/.../adapter/process/*` (except
  `HistoryCleanupConfiguration.kt`), `openapi/openapi.json`, `frontend/src/shared/api/generated/`.
  Regenerate them (see Verify). `.claude/settings.json` denies edits to them.
- **Executable models live only in `docs/use-cases/bike-leasing/process/`.** Gradle packages them onto
  the classpath; `service/app/src/main/resources` holds no `.bpmn` or `.dmn`.
- **Keep element ids stable.** Issues, the feature map and the glossary refer to them; a rename
  updates every reference in the same change.
- **A DMN's file stem is its decision id** (`checkCreditRating`); never rename one without the other.
- **Derive, never invent.** A feature's details come from its issue and the original model (tag
  [`original-process-v1`](https://github.com/miragon-blueprints/miragon-ai-example/tree/original-process-v1/kotlin-gradle/service/app/src/main/resources));
  mark anything else as an assumption and ask.
- **Ask before deciding business logic.** Gateways, deadlines, error paths and copy are business
  decisions; propose, don't pick.

## designIQ

- designIQ reads only `docs/`, only on the default branch. Every `.md` under `docs/` becomes a model,
  so `docs/` has exactly one `README.md` and no `AGENTS.md`.
- File stems are unique under `docs/`, across notations.
- Every flow node, sequence flow, pool, lane and group has its BPMNDI shape or edge.
- The t.BPM model and the feature map keep `bpmiq:mode="workshop"` and the `bpmiq` namespace.
- Markdown and Mermaid must read well as plain text; refresh the PNG in `docs/assets/` when a model's
  picture changes.
- The executable model validates without findings; the only findings under `docs/` are the feature
  map's 2 workshop warnings (open stickies, the 7±2 rule). A change adds no other finding.
- designIQ save, release and todo tools write to the live workspace or GitHub: use them only when
  asked. Release pull requests from designIQ are reviewed like any other PR.
- Todos are GitHub issues with the anchor block described in
  [CONTRIBUTING.md](CONTRIBUTING.md#reporting-bugs--requesting-features).

## Implementing a Feature

A feature follows its acceptance criteria through every layer: model → regenerated process API →
code → tests → `openapi.json` → regenerated frontend client and UI → Bruno and Playwright → feature
map (`bike-leasing-feature-map.bpmn`, `feature-map.md`) and glossary. The PR closes the issue
(`Closes #n`). Skill `implement-issue` walks this.

A sourcing decision (#8–#10) is a new ADR plus updates to `sourcing-map.owm` and
`sourcing-decisions.md`.

## Verify (targeted, after each step)

| Changed | Run |
|---|---|
| Backend class | `./gradlew test --tests "*<Name>Test"` (more in service/AGENTS.md) |
| `.bpmn` | `./gradlew generateBpmnModels`, `npm run lint:bpmn`, `./gradlew test --tests "io.miragon.blueprint.adapter.process.*"` |
| REST contract | `./gradlew test --tests "io.miragon.blueprint.adapter.inbound.rest.OpenApiSpecExportTest"`, then `npm --prefix frontend run api:generate` and `npm --prefix frontend run api:check` |
| Frontend | `npm --prefix frontend run verify` |
| Design artifact | `npx --yes @miragon/design-iq-validator@5.3.0 --root . <model-id>` (id = file stem) |
| Before a PR | `./gradlew build`, `./gradlew pitest -PmutationTargetClasses=<changed classes>` (as CI), `npm --prefix frontend run verify`, `npm run lint:bpmn`, the validator without a model id (expected: 0 errors, only the feature map's 2 workshop warnings) |

CI runs the build, bpmnlint, both drift checks (contract, process API), the diff-scoped mutation gate
(80), the frontend gates, Bruno and Playwright on every PR.

## Skills and Agents

`.claude/skills/` and `.claude/agents/` (Claude Code; other agents can read them as recipes):

| Use | When |
|---|---|
| `implement-issue` | take a GitHub issue from plan (with approval) to a verified change |
| `automate-process` | change the executable model and its glue code, validate it |
| `create-rest-controller`, `create-persistence-adapter`, `create-feature-slice`, `create-delegate` | one building block of a layer, each with its production and test pattern |
| `review-change` | review a diff or PR, findings by severity |
| `test-in-browser` | check a flow in the running app with Playwright MCP, with screenshots |
| `create-adr` | record a decision in `docs/adr/` |
| agents `review-process`, `review-vertical-slice` | read-only audits of the model's glue and of one feature end to end |

Two CLIs come as Claude Code plugins (`.claude/settings.json`) and pinned in the root `package.json`:

- **bpmn-cli** (`npx bpmn`, skill `bpmn-cli:bpmn`) for every change to a `.bpmn`: semantic edits, no
  XML by hand. `BPMN_KEEP_FLOW_IDS=1` keeps flow ids stable. The workshop models are not lint
  targets: their bpmnlint findings in its output are expected.
- **operate** (`npx operate`, skill `operate:operate`, `/operate:status`) for the local engine. Run
  `npx operate guide` before using it. It reads and repairs (`inspect --business-key <applicationId>`,
  `status`, `incident`, history, `retry`). It never deploys (the service deploys the models from its
  classpath, a hand deploy is a version the generated process API does not know) and never moves a case
  (user tasks, messages): the app's API does that, or the domain data and the engine drift apart.

## Working with GitHub

Use the `gh` CLI. Write everything (issues, PRs, commits, docs, models) in **English**. Conventional
Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`, `ci:`, `build:`). Commit, push,
open a PR or change an issue only when asked.

## ADRs

Decisions are in `docs/adr/`, listed in [docs/README.md](docs/README.md). Read them before proposing a
structural change. An accepted ADR is not rewritten; a change gets a new ADR that supersedes it
(skill `create-adr`).

## Personality

You are a knowledgeable colleague, not someone who passively takes orders. If something proposed
doesn't look right, suggest corrections, ask critical questions, and push back where needed.
Challenge ideas that could benefit from further improvement or iterative refinement rather than just
accepting them at face value.
