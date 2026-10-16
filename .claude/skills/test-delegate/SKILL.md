---
name: test-delegate
argument-hint: "[DelegateClassName]"
description: "Write or update the MockK unit test for one CIB seven JavaDelegate in adapter/inbound/cibseven: the inbound port is called with the id from the process business key, and every domain exception the delegate maps becomes the BpmnError code from the generated Errors. Use when the user asks to test a delegate, after create-delegate, or when the mutation gate reports a surviving mutant in a delegate."
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(./gradlew *)
---

# Skill: test-delegate

One test class per delegate, next to its siblings in
`service/app/src/test/kotlin/io/miragon/blueprint/adapter/inbound/cibseven/`. The test proves the glue
in isolation; the process tests in `io.miragon.blueprint.adapter.process` prove the wiring in the
engine. Both are needed: delegates are in the mutation gate.

## Test pattern

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

## Rules

- **Call `execute`, not `executeTask`.** That runs `BaseDelegate` too, as the engine does.
- **Strict mocks.** Stub the port with `every { … } just Runs` (or `returns` for a result), then
  `verify` the call with the exact `ApplicationId` and `confirmVerified(useCase)`. A relaxed mock lets
  a removed call survive the mutation gate.
- **One test per outcome:** the happy path, each `BpmnError` the delegate throws (assert the code from
  the generated `Errors`, never a string literal), and for an unmapped exception that it is rethrown
  unchanged (stub `execution.currentActivityId`, `BaseDelegate` logs it).
- **Variables** other than the business key are stubbed with the names from
  `BikeLeasingProcessProcessApi.FlowNodes.<Node>.Variables`, never raw strings.
- **No Spring, no engine.** Plain JUnit 5 + MockK + AssertJ, like the service tests.
- Match the style of the existing tests: backtick names, `underTest`, no given/when/then comments.

## Instructions

1. **Read** the delegate, its inbound port and, for error paths, the boundary events of its element in
   `docs/use-cases/bike-leasing/process/bike-leasing.bpmn`.
2. **List the outcomes** (happy path, each mapped error, rethrow) and write one test per outcome.
3. **Run** `./gradlew test --tests "*<DelegateName>Test"`, then the mutation check for the class:
   `./gradlew pitest -PmutationTargetClasses=io.miragon.blueprint.adapter.inbound.cibseven.<DelegateName>`.
   A surviving mutant means a missing assertion: add it, do not weaken the gate.
