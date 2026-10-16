# Documentation

This folder is the designIQ workspace of the repository. The root `designiq.yml` says
`models: docs` ([ADR-0008][a08]). It holds the business and architecture material for the MiraVelo
bike-leasing use case, the decisions, the demo, and the executable process model that the service
deploys.

Building and running the service is covered in [README.md][root], the dev loop in
[CONTRIBUTING.md][contrib] and the rules for agents in [AGENTS.md][agents].

How to view, in the tables below:

- **designIQ modeler**: the visual editor in designIQ.
- **designIQ text**: the plain-text view in designIQ.
- **GitHub**: the file as GitHub renders it.

## Context and glossary

| Artifact                                 | File              | Notation / how to view                   |
|------------------------------------------|-------------------|------------------------------------------|
| Glossary: terms of models, code and docs | [glossary.md][gl] | Markdown · designIQ text · GitHub        |
| Project overview and how to run it       | [README.md][root] | Markdown · GitHub only (outside `docs/`) |

[gl]: glossary.md
[root]: ../README.md
[contrib]: ../CONTRIBUTING.md
[agents]: ../AGENTS.md

## Use case: bike leasing

| Artifact                                | File                                     | Notation / how to view            |
|-----------------------------------------|------------------------------------------|-----------------------------------|
| Use case: problem, actors, goal, scope  | [bike-leasing-use-case.md][uc]           | Markdown · designIQ text · GitHub |
| t.BPM table-top model of the core       | [bike-leasing-tbpm.bpmn][tbpm]           | t.BPM (BPMN) · designIQ modeler   |
| BPM feature map: core and stages 1 to 4 | [bike-leasing-feature-map.bpmn][fm]      | t.BPM (BPMN) · designIQ modeler   |
| Feature table: issues, stages, elements | [feature-map.md][fmmd]                   | Markdown · designIQ text · GitHub |
| Executable process (deployed)           | [bike-leasing.bpmn][bpmn]                | BPMN · designIQ modeler           |
| Notes on the executable process         | [executable-process.md][exec]            | Markdown · designIQ text · GitHub |
| Original process (frozen, not deployed) | [original-bike-leasing.bpmn][orig]       | BPMN · designIQ modeler           |
| Original called process (frozen)        | [original-cancel-bike-order.bpmn][origc] | BPMN · designIQ modeler           |
| Credit-rating decision (frozen, #19)    | [checkCreditRating.dmn][dmn]             | DMN · designIQ modeler            |
| Scope: original vs. core vs. issues     | [scope-comparison.md][scope]             | Markdown · designIQ text · GitHub |
| Open feature and decision issues        | [issues labelled todo][issues]           | GitHub · designIQ todos           |

The executable model is the simplified core ([executable-process.md][exec]). It and the frozen original both show the title "MiraVelo Bike Leasing" (process
id `bikeLeasingProcess`). Tell them apart by the model id, which is the file name: `bike-leasing` is
the one that runs, `original-bike-leasing` is the 43-node reference. The decision table
`checkCreditRating.dmn` is not deployed; it waits in `original/` for feature #19.

[uc]: use-cases/bike-leasing/bike-leasing-use-case.md
[tbpm]: use-cases/bike-leasing/tbpm/bike-leasing-tbpm.bpmn
[fm]: use-cases/bike-leasing/feature-map/bike-leasing-feature-map.bpmn
[fmmd]: use-cases/bike-leasing/feature-map/feature-map.md
[bpmn]: use-cases/bike-leasing/process/bike-leasing.bpmn
[dmn]: use-cases/bike-leasing/original/checkCreditRating.dmn
[exec]: use-cases/bike-leasing/process/executable-process.md
[orig]: use-cases/bike-leasing/original/original-bike-leasing.bpmn
[origc]: use-cases/bike-leasing/original/original-cancel-bike-order.bpmn
[scope]: use-cases/bike-leasing/original/scope-comparison.md
[issues]: https://github.com/miragon-blueprints/miragon-ai-example/issues?q=is%3Aissue%20label%3Atodo

## Architecture

| Artifact                                  | File                          | Notation / how to view                 |
|-------------------------------------------|-------------------------------|----------------------------------------|
| System context (C4 level 1)               | [c4-system-context.md][c4ctx] | Mermaid · designIQ text (raw) · GitHub |
| Containers and components (C4 level 2, 3) | [c4-container.md][c4con]      | Mermaid · designIQ text (raw) · GitHub |
| Sourcing map: make, buy or adapt          | [sourcing-map.owm][owm]       | Wardley Map (OWM) · designIQ modeler   |
| Sourcing decisions and open decisions     | [sourcing-decisions.md][srcd] | Markdown · designIQ text · GitHub      |

[c4ctx]: architecture/c4-system-context.md
[c4con]: architecture/c4-container.md
[owm]: architecture/sourcing-map.owm
[srcd]: architecture/sourcing-decisions.md

## Decisions

An Architecture Decision Record (ADR) captures one decision. It is dated and numbered, and it is
never renumbered. The collection is a baseline as of 2026-10-16; earlier records are in git. An
accepted ADR is not rewritten: a changed decision gets a new ADR that supersedes it, and a small
factual correction may add a dated note under the status. New ADRs are copied from
[0000-adr-template.md][a00]. All ADRs are Markdown (designIQ text, GitHub). Procedures are not
decisions, so they live in CONTRIBUTING.md and the AGENTS.md files.

| ADR         | Decision                                                             | Status   |
|-------------|----------------------------------------------------------------------|----------|
| [0001][a01] | Record decisions in ADRs, instructions for agents in AGENTS.md       | Accepted |
| [0002][a02] | Hexagonal backend, enforced by ArchUnit and Konsist                  | Accepted |
| [0003][a03] | OpenAPI as the checked-in contract                                   | Accepted |
| [0004][a04] | Mutation testing as a blocking gate at 80                            | Accepted |
| [0005][a05] | Track the latest stable major versions                               | Accepted |
| [0006][a06] | Flyway owns the database schema                                      | Accepted |
| [0007][a07] | Operability and delivery: OCI images, probes and metrics             | Accepted |
| [0008][a08] | `docs/` is the designIQ workspace                                    | Accepted |
| [0009][a09] | React frontend next to the service                                   | Accepted |
| [0010][a10] | Back-office task UI: own inbox in the frontend                       | Accepted |
| [0011][a11] | Embedded CIB seven, delegates by expression, a generated process API | Accepted |

[a00]: adr/0000-adr-template.md
[a01]: adr/0001-record-architecture-decisions.md
[a02]: adr/0002-hexagonal-architecture-for-the-backend.md
[a03]: adr/0003-openapi-as-the-checked-in-contract.md
[a04]: adr/0004-mutation-testing-as-a-blocking-pr-gate.md
[a05]: adr/0005-track-the-latest-major-versions.md
[a06]: adr/0006-flyway-for-database-migrations.md
[a07]: adr/0007-build-and-deployment-approach.md
[a08]: adr/0008-docs-is-the-designiq-workspace.md
[a09]: adr/0009-react-frontend-next-to-the-service.md
[a10]: adr/0010-back-office-task-ui-own-inbox-in-the-frontend.md
[a11]: adr/0011-embedded-cib-seven-with-delegates-and-a-generated-process-api.md

## Demo

| Artifact                                 | File                    | Notation / how to view            |
|------------------------------------------|-------------------------|-----------------------------------|
| Demo script: the live walk-through       | [demo-script.md][demo]  | Markdown · designIQ text · GitHub |
| Rehearsal log: what the rehearsal showed | [rehearsal-log.md][log] | Markdown · designIQ text · GitHub |

[demo]: demo/demo-script.md
[log]: demo/rehearsal-log.md

## Viewing in designIQ

- **Visual in designIQ:** BPMN (including the t.BPM workshop mode with its stickies and groups),
  the frozen DMN and the Wardley Map. Open them in the designIQ modeler.
- **Text only in designIQ:** Markdown, shown raw without a preview. The Mermaid diagrams of the C4
  views are raw text there too.
- **Rendered by GitHub:** Markdown and Mermaid. GitHub does not render BPMN, DMN or OWM; it shows
  their source.
- **PNG fallbacks** in `assets/` cover what one side cannot show, or a designIQ outage:
  `bike-leasing-core.png` (executable process), `bike-leasing-original.png`,
  `bike-leasing-tbpm.png`, `bike-leasing-feature-map.png`, `sourcing-map.png`,
  `c4-system-context.png`, `c4-container.png`.
- designIQ reads the default branch (`main`) only. A model changed in a pull request appears after
  the merge. A change made in designIQ reaches Git as a pull request when it is released, and CI
  tests it like any other.
- Open issues labelled `todo` and `process:bike-leasing` appear in designIQ as todos on the
  executable process, pinned to the element they extend.
- Running instances of the executable process show in the CIB seven Cockpit of the running
  service (http://localhost:8081/webapp/#/seven/auth/start, admin/admin). Customers and the back office work in the
  frontend (http://localhost:5173), the back office in its inbox ([ADR-0010][a10]).

Recommended demo order:

1. **Context and glossary**: [bike-leasing-use-case.md][uc] and [glossary.md][gl].
2. **Wardley Map**: [sourcing-map.owm][owm], what to make, buy or adapt.
3. **t.BPM and feature map**: [bike-leasing-tbpm.bpmn][tbpm], then
   [bike-leasing-feature-map.bpmn][fm].
4. **C4**: [c4-system-context.md][c4ctx] and [c4-container.md][c4con] (the diagrams render on
   GitHub).
5. **Executable process**: [bike-leasing.bpmn][bpmn], then the same model in the Cockpit.
6. **Live run in the frontend**: submit an application, decide the credit check in the inbox, sign,
   report the handover; withdraw a case and confirm the reversal; optionally an invalid application
   and the `BIKE-FAIL` incident in the Cockpit.
7. **From a todo to a feature**: the todos on the executable process, or the [issue list][issues],
   for example #19 on "Check credit rating".
8. **How an agent implements it**: [AGENTS.md][agents], the skills and review agents in `.claude/`,
   and the gates that refuse a wrong change.

The step-by-step script is [demo/demo-script.md][demo].
