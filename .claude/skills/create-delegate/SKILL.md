---
name: create-delegate
argument-hint: "[<path-to-ProcessApi-or-BPMN-file>] [element-id-or-task-name]"
description: Create or update a CIB seven (Camunda 7) JavaDelegate — or an execution/task listener — for a BPMN service task or listener hook, wired as a Spring @Component and invoked by expression (#{beanName}). Use when the user asks to "create a delegate", "wire a service task to code", "add a JavaDelegate", or "add an execution/task listener". The delegate extends BaseDelegate, calls exactly one inbound port, and reads process variables via the typed ProcessApi.
allowed-tools: Read, Write, Glob
---

# Skill: create-delegate

Generate one `@Component` **JavaDelegate** (or listener) in
`service/app/src/main/kotlin/io/miragon/blueprint/adapter/inbound/cibseven/` for a BPMN element. CIB seven runs
**embedded**; service tasks call **JavaDelegates by expression**, not Zeebe job workers. See
[ADR-0002](../../../docs/adr/0002-hexagonal-architecture-for-the-backend.md).

## Pattern (this repo)

```kotlin
@Component
class ValidateApplicationDelegate(
    private val useCase: ValidateApplicationUseCase,
) : BaseDelegate() {

    override fun executeTask(execution: DelegateExecution) {
        try {
            useCase.validate(ApplicationId.of(execution.processBusinessKey))
        } catch (e: ApplicationInvalidException) {
            throw BpmnError(Errors.APPLICATION_INVALID.code, e.message)
        }
    }
}
```

- **Extend `BaseDelegate`** and override `executeTask`; never implement `JavaDelegate.execute`.
- The **application id is the process business key** (`execution.processBusinessKey`), wrapped into
  `ApplicationId.of(...)`. Any other variable name comes from
  `BikeLeasingProcessProcessApi.FlowNodes.<Node>.Variables.…`, never a raw string.
- A **listener** is a plain `@Component` implementing `ExecutionListener` or `TaskListener`, without
  `BaseDelegate`. The model has none today.

## IMPORTANT — the rules the build enforces

- The delegate injects **exactly one** inbound port (`*UseCase`/`*Query`).
- Suffixes in `adapter.inbound.cibseven`: **`Delegate`**, **`Worker`**, **`Listener`** only.
- The BPMN references the bean as `camunda:delegateExpression="#{beanName}"` (class name, lowercase
  first letter); the model test `ServiceTaskDelegateExpressionRule` rejects any other form.
- Domain exceptions become `BpmnError(errorCode, message)` where the model has an error boundary event;
  the code comes from the generated `Errors`.
- The service behind the delegate joins the engine's job transaction. If it is `@Transactional` and
  throws the exception the delegate turns into a `BpmnError`, declare it in `noRollbackFor`, or the
  commit fails with `UnexpectedRollbackException` and the error path never runs.

## Instructions

1. **Load** `docs/use-cases/bike-leasing/process/bike-leasing.bpmn` and
   `adapter/process/BikeLeasingProcessProcessApi.kt`. The BPMN is the source of truth for the element,
   its `delegateExpression`, its variables and its boundary events; the ProcessApi supplies the typed
   constants.
2. **Identify the element** by the id/name in `$ARGUMENTS`, else list the service tasks and ask.
3. **Find the inbound port** (`Glob service/app/src/main/kotlin/**/application/port/inbound/*{UseCase,Query}.kt`).
   If missing, offer to generate it or run [`automate-process`](../automate-process/SKILL.md).
4. **Generate or update** the delegate; for an existing file add only what is missing.
5. **Report and hand off:** confirm the expression matches the bean name (the
   [`review-process`](../../agents/review-process.md) subagent audits this), add a process test in
   `service/app/src/test/kotlin/io/miragon/blueprint/process/` (the `cibseven` package is excluded from pitest on
   purpose: thin glue, covered by the process tests), and run
   `./gradlew test --tests "io.miragon.blueprint.process.*"`.
