---
name: automate-process
argument-hint: "[<path-to-ProcessApi-or-BPMN-file>]"
description: Change the executable CIB seven (Camunda 7) BPMN model and generate the hexagonal glue code across all layers — inbound JavaDelegates, inbound/outbound ports, application services, and the process adapter that drives the engine. Use when the user asks to "automate a process", "add a task to the process", "generate glue code for a BPMN process", or "scaffold delegates and adapters". Skips files that already exist; offers to run review-process first.
allowed-tools: Read, Write, Edit, Bash, Glob
---

# Skill: automate-process

Wire the **CIB seven** BPMN model (embedded engine) to the application. Service tasks are
JavaDelegates invoked by expression; the engine is driven through `RuntimeService`/`TaskService`. See
[ADR-0002](../../../docs/adr/0002-hexagonal-architecture-for-the-backend.md). Source root:
`src/main/kotlin/io/miragon/blueprint/`.

## What this creates

| Layer | Package (`io.miragon.blueprint.…`) | What |
|---|---|---|
| Inbound delegates | `adapter.inbound.cibseven` | one `*Delegate : BaseDelegate` per service task |
| Outbound adapter | `adapter.outbound.cibseven` | `LeasingProcessAdapter`, one method per message / user task |
| Inbound ports | `application.port.inbound` | one `*UseCase`/`*Query` per delegate or endpoint |
| Outbound port | `application.port.outbound` | `LeasingProcess` (start + one method per message / user task) |
| Application services | `application.service` | one `*Service` per inbound port |

## IMPORTANT

- **The model lives in `docs/use-cases/bike-leasing/process/bike-leasing.bpmn`.** Edit it there and
  nowhere else; `src/main/resources` holds no `.bpmn`. `original/` is frozen.
- **Keep element ids stable.** The issues' anchor blocks, the feature map, `executable-process.md` and
  the glossary refer to them; a rename or removal updates every reference in the same change.
- **Complete BPMNDI**: every new node and flow needs its shape or edge, or designIQ breaks.
- **String constants come from the generated API**, never raw literals: `FlowNodes.<Node>.Variables`,
  the shared `Messages`/`Errors`, `FlowNodes.<UserTask>.ELEMENT_ID`. Regenerate with
  `./gradlew generateBpmnModels` and commit `adapter/process` (never hand-edited).
- Process tests assert the walked path as a `ProcessPath` over `FlowNodes` (see
  `BikeLeasingProcessTest`), never as a hand-written element-id list.

## Instructions

### Step 0 — Offer the review first
Ask whether to run the [`review-process`](../../agents/review-process.md) subagent first. If yes, stop
here; the user restarts this skill afterwards.

### Step 1 — Change the model (if asked) and regenerate
Edit the `.bpmn`, then:
```bash
./gradlew generateBpmnModels
npm run lint:bpmn
```
Extract from `BikeLeasingProcessProcessApi.kt`: `PROCESS_ID`, `FlowNodes.*` (ids, `Variables`,
`Next`), and the shared `Messages.*` / `ServiceTasks.*` / `Errors.*`.

### Step 2 — Domain types
Scan `domain/{leasing,bike}/` for a value object per process variable (`APPLICATION_ID` →
`ApplicationId`, `BIKE_ID` → `BikeId`). Create a missing one before generating ports. Ask if a name
does not map to a sensible type.

### Step 3 — Ports, services, delegates, adapter
- Inbound port per service task / endpoint, domain-typed params. Skip existing.
- Outbound `LeasingProcess`: add one method per new message or user-task completion.
- One `@Service` per inbound port (ArchUnit: exactly one), body `TODO("Add implementation")`.
- Delegates per [`create-delegate`](../create-delegate/SKILL.md).
- Adapter: `override` each new method in `LeasingProcessAdapter`, using the typed constants and the
  business-key helpers in `ProcessEngineExtensions.kt`.

### Step 4 — Report and hand off
List every file created/skipped. Remind the user to fill the service `TODO`s, run
[`review-process`](../../agents/review-process.md), add tests, and run:
```bash
./gradlew test --tests "io.miragon.blueprint.process.*"
npx --yes @miragon/design-iq-validator@5.3.0 --root . bike-leasing
```
If elements changed, update `executable-process.md`, the feature map and the glossary in the same
change, and refresh the PNG in `docs/assets/`.
