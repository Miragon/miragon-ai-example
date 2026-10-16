---
name: create-delegate
argument-hint: "[<path-to-ProcessApi-or-BPMN-file>] [element-id-or-task-name]"
description: "Create, update or test a CIB seven JavaDelegate or listener for a BPMN element, with its MockK unit test. Use when wiring a service task to code, testing a delegate, or fixing a surviving mutant in one."
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(./gradlew *), Skill
---

# Skill: create-delegate

Generate one `@Component` **JavaDelegate** (or listener) in
`service/app/src/main/kotlin/io/miragon/blueprint/adapter/inbound/cibseven/` for a BPMN element. CIB seven runs
**embedded**; service tasks call **JavaDelegates by expression**, not external task workers. The
backend rules are in `service/AGENTS.md`.

## Production pattern

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

## Test pattern

One test class per delegate in `service/app/src/test/kotlin/io/miragon/blueprint/adapter/inbound/cibseven/`.
The happy path comes first; each mapped `BpmnError` adds one test.

```kotlin
class ValidateApplicationDelegateTest {

    private val useCase = mockk<ValidateApplicationUseCase>()
    private val execution = mockk<DelegateExecution>()
    private val underTest = ValidateApplicationDelegate(useCase)

    private val applicationId = ApplicationId.new()

    @BeforeEach
    fun givenTheApplicationIsTheBusinessKey() {
        every { execution.processBusinessKey } returns applicationId.value.toString()
    }

    @Test
    fun `validates the application of the business key`() {
        every { useCase.validate(applicationId) } just Runs

        underTest.execute(execution)

        verify { useCase.validate(applicationId) }
        confirmVerified(useCase)
    }

    @Test
    fun `turns an invalid application into the applicationInvalid BPMN error`() {
        every { useCase.validate(applicationId) } throws
            ApplicationInvalidException(applicationId, listOf("customer is underage"))

        assertThatThrownBy { underTest.execute(execution) }
            .isInstanceOf(BpmnError::class.java)
            .extracting("errorCode")
            .isEqualTo(Errors.APPLICATION_INVALID.code)
    }
}
```

- **Call `execute`, not `executeTask`.** That runs `BaseDelegate` too, as the engine does.
- **Strict mocks.** Stub the port with `every { … } just Runs` (or `returns` for a result), then
  `verify` the call with the exact `ApplicationId` and `confirmVerified(useCase)`. A relaxed mock lets
  a removed call survive the mutation gate.
- **One test per outcome:** always the happy path; then each `BpmnError` the delegate throws (assert
  the code from the generated `Errors`, never a string literal), and for an unmapped exception that it
  is rethrown unchanged (stub `execution.currentActivityId`, `BaseDelegate` logs it).
- **Variables** other than the business key are stubbed with the names from
  `BikeLeasingProcessProcessApi.FlowNodes.<Node>.Variables`, never raw strings.
- **No Spring, no engine.** Plain JUnit 5 + MockK + AssertJ; backtick names, `underTest`, no
  given/when/then comments.

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
4. **Generate or update** the delegate; for an existing file add only what is missing. To only test an
   existing delegate, skip to step 5.
5. **Test it twice:** the unit test after the test pattern above, one test per outcome, then
   `./gradlew test --tests "*<DelegateName>Test"` and the mutation check
   `./gradlew pitest -PmutationTargetClasses=io.miragon.blueprint.adapter.inbound.cibseven.<DelegateName>`
   (a surviving mutant means a missing assertion: add it, do not weaken the gate); and the path
   through the engine with a process test in
   `service/app/src/test/kotlin/io/miragon/blueprint/adapter/process/`:
   `./gradlew test --tests "io.miragon.blueprint.adapter.process.*"`.
6. **Report:** confirm the expression matches the bean name (the agent
   [`review-process`](../../agents/review-process.md) audits this) and list the files written.
