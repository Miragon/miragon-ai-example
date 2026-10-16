---
name: automate-process
argument-hint: "[what should change in the process]"
description: "Change the executable CIB seven BPMN model (docs/use-cases/bike-leasing/process/bike-leasing.bpmn) and wire its glue code across the hexagon: regenerate the typed process API, add ports, services, delegates and process-adapter methods, cover the new paths with process tests and validate the model with bpmnlint, the designIQ validator and a visual check. Use when the user asks to automate a process step, add or change a task, event or gateway, or review the diagram after an edit. Business decisions are asked, never assumed."
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Skill, Agent
---

# Skill: automate-process

The model is the business's artifact; this skill changes it technically. Source root of the glue:
`service/app/src/main/kotlin/io/miragon/blueprint/`.

## Rules

- **Only `docs/use-cases/bike-leasing/process/bike-leasing.bpmn` changes.** `original/` is frozen, the
  t.BPM model and the feature map are documentation.
- **Business decisions are questions.** A new gateway condition, deadline, error path or user task
  owner comes from the issue or the original model; otherwise ask before editing.
- **Keep element ids stable**; a rename updates the feature map, glossary, `executable-process.md` and
  the issues' anchor blocks in the same change.
- **Complete BPMNDI** for every new node and flow (standard sizes: task 100×80, event 36×36,
  gateway 50×50), or designIQ breaks.
- **Process variables stay minimal:** ids, not copies of domain data. The engine is not a database.
- **Engine limits:** a service task is a delegate by expression (`#{beanName}`, enforced by
  `ServiceTaskDelegateExpressionRule`); asynchronous continuations (`asyncBefore`) are where retries
  and incidents happen; a timer cycle restarts on redeploy.
- **Constants come from the generated API** (`BikeLeasingProcessProcessApi.FlowNodes.<Node>`, with
  `.ELEMENT_ID`, `Variables`, `Next`; the shared `Messages`, `Errors`, `ServiceTasks`, and
  `Escalations` once the model has one), never raw strings. `adapter/process` is generated: never
  edit it.

## Steps

1. **Review first.** Run the agent `review-process` on the current model and report blocking findings
   before changing anything.
2. **Agree the change.** Restate it in business terms (which element, which path, which outcome) with
   the open questions. Wait for the answer.
3. **Edit the model**, then regenerate and lint (the PostToolUse hook lints every edit too):
   ```bash
   ./gradlew generateBpmnModels
   npm run lint:bpmn
   ```
4. **Glue code**, skipping what exists:

   | Layer | Package (`io.miragon.blueprint.…`) | What |
   |---|---|---|
   | Domain | `domain.{leasing,bike}` | a value object per new variable (`ApplicationId`, `BikeId`) |
   | Inbound port | `application.port.inbound` | one `*UseCase`/`*Query` per delegate or endpoint |
   | Service | `application.service` | one `*Service` per inbound port |
   | Delegate | `adapter.inbound.cibseven` | skills `create-delegate` and `test-delegate` |
   | Outbound port | `application.port.outbound` | `LeasingProcess`: one method per message or user-task completion |
   | Process adapter | `adapter.outbound.cibseven` | `LeasingProcessAdapter`, using `ProcessEngineExtensions.kt` |

5. **Process tests.** Derive the scenarios from the model's paths: every new outgoing flow of a
   gateway, every boundary event, every event subprocess gets a test in
   `service/app/src/test/kotlin/io/miragon/blueprint/adapter/process/` that asserts the walked path as a
   `ProcessPath` (see `BikeLeasingProcessTest`). The job executor is off in tests:
   `executeJobFor` in `adapter/process/JobExecutionUtils.kt` fires async continuations; a timer needs
   its own helper querying `managementService.createJobQuery().timers()`, and a due date is asserted
   against a fixed `ClockUtil` time.
   ```bash
   ./gradlew test --tests "io.miragon.blueprint.adapter.process.*"
   ```
6. **Validate the model** with the three nets in [references/visual-check.md](references/visual-check.md).
7. **Documentation in the same change:** a row in `executable-process.md` for every difference from the
   source model, the feature map and glossary if elements changed, the PNG in `docs/assets/`.
8. **Report** files created or skipped, the scenarios tested, the validation verdict and the
   questions still open. Run `review-process` once more on the result.
