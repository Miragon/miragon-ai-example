# 0016 — `docs/` is the designIQ workspace

- **Status:** Accepted
- **Date:** 2026-10-15
- **Deciders:** Thomas (@Hafflgav)

## Context

designIQ is Miragon's Git-native modelling workspace. It connects to a GitHub repository through a
GitHub App and reads its models from exactly one folder, which a root `designiq.yml` names. It
shows BPMN, DMN and Wardley Maps in visual modelers and Markdown as plain text. Live edits stay in a
shared workspace until a release opens a pull request.

The demo walks from the business picture to the running process: glossary, use case, t.BPM, feature
map, C4, Wardley Map, ADRs, and finally the executable model. All of it should sit in one place, and
the process designIQ shows must be the one the engine runs. Until now the executable models lived in
`src/main/resources` (per variant), where designIQ could only show them without the material that
explains them.

## Decision

The root `designiq.yml` contains `models: docs`. **`docs/` is the designIQ workspace**, and the
executable models live in it.

```
docs/
  README.md, glossary.md
  use-cases/bike-leasing/  use case, tbpm/, feature-map/, process/ (executable), original/ (frozen)
  architecture/            C4 (Markdown + Mermaid), Wardley Map (.owm), sourcing decisions
  adr/                     these records
  demo/                    demo script, rehearsal log
  assets/                  PNG renders
```

- **One source for the process.** `bike-leasing.bpmn` and `checkCreditRating.dmn` live in
  `docs/use-cases/bike-leasing/process/`. Gradle packages them from there onto the classpath
  (`processResources`: `bpmn/`, `dmn/`), `generateBpmnModels` reads them there, and bpmnlint lints
  them there. What designIQ shows is what runs. Forms stay in `src/main/resources/forms`, because
  designIQ does not know `.form` files.
- **The DMN file stem equals the decision id** (`checkCreditRating`), so designIQ links the business
  rule task to its decision.
- **designIQ releases go through CI.** A release is a pull request into `main`, so `pre-merge.yml`
  runs on it: bpmnlint, `./gradlew build` with model validation and process tests, contract drift,
  mutation testing and the Bruno scenarios.
- **designIQ todos are GitHub issues** with the labels `todo` and `process:bike-leasing` (the BPMN
  file stem). The anchor block at the top of the body pins an issue to elements of the executable
  model:

  ```
  <!-- bpmiq:todo v1
  process: bike-leasing
  file: docs/use-cases/bike-leasing/process/bike-leasing.bpmn
  element: <element id> | <element name>
  -->
  ```

**Constraints we accept:**

- designIQ reads the default branch only. A model changed in a pull request appears after the merge.
- It reads exactly one folder.
- Every `.md` under `docs/` is a Markdown model, and file stems must be unique per notation. So
  there is no second `README.md` under `docs/`, and the frozen originals are prefixed `original-`.
- Every BPMN file needs a complete BPMNDI section, or the visual editor breaks.
- Markdown is plain text in designIQ, and Mermaid is not rendered, so C4 shows as raw text. GitHub
  renders both, and `docs/assets/` holds PNG fallbacks.
- The validator has no exemption for compensation handlers. On `bike-leasing.bpmn` it reports
  three false errors: `serviceTask_cancelContract` is unreachable and a dead end, and
  `event_compensateContract` is a dead end. Under the default `lint: "block"` gate designIQ
  therefore refuses full MCP and REST saves of this model (`save_bpmn_xml`, `save_model_content`;
  they need `lint: "warn"`), while `edit_process` refuses only a change that adds an error. Canvas
  edits and releases are not gated.
  The repo's own checks (bpmnlint, `BikeLeasingModelValidationTest`, process tests) stay
  authoritative.
- designIQ has no properties panel for engine attributes (delegate expressions, async flags, retry
  cycle, form reference). They are edited in the XML or in Camunda Modeler, and the model validation
  test fails the build if a service task loses its delegate expression.

## Alternatives considered

- **Keep the executable models in `src/main/resources`.** designIQ would either show only those
  models, without the glossary, t.BPM, C4 and Wardley Map next to them, or a docs copy that drifts
  from what runs.
- **Make the whole repository the models folder (`models: .`).** Every Markdown file would become a
  model (README, AGENTS, CLAUDE, CONTRIBUTING, the Bruno README), the `README` stems would collide
  (the first by path wins), and the workspace would be noisy for a business audience.
- **A separate documentation repository.** Code and models would be decoupled, but what designIQ
  shows would no longer be what runs.

## Consequences

- **Positive:** one source of truth for the process, readable by the business in designIQ. A model
  change made in designIQ becomes a tested pull request. The same files read well on GitHub, which
  renders the Markdown and Mermaid.
- **Negative / trade-offs:** the build depends on a docs path. Moving
  `docs/use-cases/bike-leasing/process/` means updating `build.gradle.kts` and `package.json`.
  Markdown and C4 are raw text in designIQ. The unique-stem rule constrains naming.
- **Neutral:** [`docs/README.md`](../README.md) is the index of the workspace and gives the
  recommended viewing order.
