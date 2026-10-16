# 0008 — `docs/` is the designIQ workspace

- **Status:** Accepted
- **Date:** 2026-10-16
- **Deciders:** Thomas (@Hafflgav)
- **Origin:** Consolidated baseline; earlier history in git.

## Context

designIQ is Miragon's Git-native modelling workspace. It connects to a GitHub repository through a
GitHub App and reads its models from exactly one folder, which a root `designiq.yml` names. It shows
BPMN, DMN and Wardley Maps in visual modelers and Markdown as plain text. Live edits stay in a shared
workspace until a release opens a pull request.

The demo walks from the business picture to the running process: glossary, use case, t.BPM, feature
map, C4, Wardley Map, ADRs, and finally the executable model. All of it should sit in one place, and
the process designIQ shows must be the one the engine runs.

## Decision

The root `designiq.yml` contains `models: docs`. **`docs/` is the designIQ workspace, and the
executable models live in it.**

```
docs/
  README.md, glossary.md
  use-cases/bike-leasing/  use case, tbpm/, feature-map/, process/ (executable), original/ (frozen)
  architecture/            C4 (Markdown + Mermaid), Wardley Map (.owm), sourcing decisions
  adr/                     these records
  demo/                    demo script, rehearsal log
  assets/                  PNG renders
```

- **One source for the process.** The executable models live in
  `docs/use-cases/bike-leasing/process/` (today `bike-leasing.bpmn`). Gradle packages them from there
  onto the classpath (`processResources`: `*.bpmn` to `bpmn/`, `*.dmn` to `dmn/`),
  `generateBpmnModels` reads them there, and bpmnlint lints them there. What designIQ shows is what
  runs.
- **`original/` is frozen.** It holds byte-identical reference copies of the original models and the
  decision `checkCreditRating.dmn`. They are not deployed, not generated and not linted, and never
  edited.
- **A DMN's file stem equals its decision id** (`checkCreditRating`), so designIQ links a business
  rule task to its decision.
- **designIQ releases go through CI.** A release is a pull request into `main`, so `pre-merge.yml`
  runs on it like on any other change.
- **designIQ todos are GitHub issues** labelled `todo` and `process:bike-leasing` (the BPMN file
  stem), with a `bpmiq:todo v1` anchor block at the top of the body that pins the issue to elements
  of the executable model. The format is in `AGENTS.md`.

**Constraints we accept:**

- designIQ reads the default branch only. A model changed in a pull request appears after the merge.
- It reads exactly one folder.
- Every `.md` under `docs/` becomes a Markdown model, and file stems must be unique per notation. So
  there is no second `README.md` under `docs/`, and the frozen originals are prefixed `original-`.
- Every BPMN file needs complete BPMNDI, or the visual editor breaks.
- Markdown is plain text in designIQ, and Mermaid is not rendered, so C4 shows as raw text. GitHub
  renders both, and `docs/assets/` holds PNG fallbacks.
- The validator has no exemption for compensation handlers: it reports them as unreachable and dead
  ends. Today only the frozen original contains them. Under the default `lint: "block"` gate,
  designIQ refuses a full MCP or REST save of a model with such errors. The repo's own checks
  (bpmnlint, `BikeLeasingModelValidationTest`, process tests) stay authoritative.
- designIQ has no properties panel for engine attributes (delegate expressions, async flags, retry
  cycle). They are edited in the XML or in Camunda Modeler, and the model validation test fails the
  build if a service task loses its delegate expression.

## Alternatives considered

- **Keep the executable models in `src/main/resources`.** designIQ would show either only those
  models, without the glossary, t.BPM, C4 and Wardley Map next to them, or a docs copy that drifts
  from what runs.
- **Make the whole repository the models folder (`models: .`).** Every Markdown file would become a
  model (README, AGENTS, CONTRIBUTING, the Bruno README), the `README` stems would collide, and the
  workspace would be noisy for a business audience.
- **A separate documentation repository.** What designIQ shows would no longer be what runs.

## Consequences

- **Positive:** one source of truth for the process, readable by the business in designIQ. A model
  change made in designIQ becomes a tested pull request. The same files read well on GitHub.
- **Negative / trade-offs:** the build depends on a docs path: moving
  `docs/use-cases/bike-leasing/process/` means updating `service/app/build.gradle.kts` and
  `package.json`. Markdown and C4 are raw text in designIQ. The unique-stem rule constrains naming.
- **Not covered:** the designIQ validator runs locally, not in CI. A designIQ release contains only
  the `.bpmn`; the regenerated process API has to be committed to the same pull request by hand.
  [`docs/README.md`](../README.md) is the index of the workspace and gives the viewing order.
