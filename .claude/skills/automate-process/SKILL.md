---
name: automate-process
argument-hint: "[what should change in the process]"
description: "Change the executable BPMN model and wire its glue code: process API, ports, delegates, process tests, validation. Use when adding or changing a task, event or gateway, or automating a process step. Asks business decisions, never assumes them."
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
- **Edit the model only through bpmn-cli** (skill `bpmn-cli:bpmn`), never as XML. It draws the BPMNDI
  designIQ needs and, with `BPMN_KEEP_FLOW_IDS=1` from `.claude/settings.json`, keeps the flow ids.
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
3. **Edit the model**: read it with `bpmn show`, change it in one `bpmn apply` batch, check it with
   `bpmn validate` (every bpmn-cli write also runs the `.bpmnlintrc`). Then regenerate and lint:
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
   | Delegate | `adapter.inbound.cibseven` | skill `create-delegate` |
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
