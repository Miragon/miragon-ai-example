# 0020 — Embedded CIB seven, delegates by expression, a generated process API

- **Status:** Accepted
- **Date:** 2026-10-16
- **Origin:** Consolidated baseline; earlier history in git.

## Context

The service runs the executable model `bike-leasing.bpmn`. Three choices shape how code and model
meet: where the engine runs, how a service task reaches our code, and how code refers to model
elements (ids, messages, variables, errors) without string literals that break silently when the
model changes. CIB seven is the open-source community fork of Camunda 7; buying it rather than
building an engine is recorded in
[sourcing-decisions.md](../architecture/sourcing-decisions.md).

## Decision

**The engine is embedded in the Spring Boot service.** CIB seven 2.2 runs in the service's JVM
through `cibseven-bpm-spring-boot-starter-rest-4` and `-webapp-4` (`cibseven` bundle in
`gradle/libs.versions.toml`). It uses the service's datasource and manages its own `ACT_*` schema.
The service drives it through the outbound port `LeasingProcess` and its adapter
`LeasingProcessAdapter` (`adapter/outbound/cibseven`): start, correlate messages, list and complete
user tasks. The engine REST API (`/engine-rest`) and the webapps (Cockpit, Tasklist) are served by
the same app.

**Service tasks call Spring-bean `JavaDelegate`s by delegate expression** (`#{beanName}`), not
external tasks, `camunda:class` or `${…}` expressions. Each delegate is a `@Component` in
`adapter/inbound/cibseven`, extends `BaseDelegate` and calls exactly one inbound port. A custom
bpmn-to-code validation rule,
`service/app/src/test/kotlin/io/miragon/blueprint/adapter/process/ServiceTaskDelegateExpressionRule.kt`,
run by `BikeLeasingModelValidationTest` together with the built-in rules, fails the build when a
service task uses anything else.

**bpmn-to-code generates the typed process API** from the model at build time. The Gradle task
`generateBpmnModels` (plugin `io.miragon.bpmn-to-code-gradle`, configured in
`service/app/build.gradle.kts`) reads `docs/use-cases/bike-leasing/process/*.bpmn` and writes
`adapter/process`: `BikeLeasingProcessProcessApi` plus the shared `ServiceTasks`, `Messages`,
`ProcessVariables` and `Errors`. It runs before every `compileKotlin`. The generated files are
committed and never edited by hand. Delegates, adapters and tests use these constants, so a renamed
or removed element breaks the compile.

**Process tests assert the walked path as a compile-checked `ProcessPath`** (`hasPassedInOrder` in
`adapter/process/ProcessPathAssertions.kt`), built from the generated `FlowNodes`, instead of
hand-maintained id lists. They are `@SpringBootTest` tests with the CIB seven assertions
(`cibseven-bpm-assert`) on H2.

Reasons:

- Delegates instead of external tasks keep the engine embedded and give every service task the same
  pattern (recorded in [executable-process.md](../use-cases/bike-leasing/process/executable-process.md),
  "Deviations from the source model").
- One deployable with no separate engine or worker process; the service changes domain state and
  completes a user task in one transaction (as the inbox relies on,
  [ADR-0019](0019-back-office-task-ui-own-inbox-in-the-frontend.md)) (⚠ reconstructed, to be
  confirmed).
- Generated constants turn a model change that breaks the code into a compile error instead of a
  runtime failure (⚠ reconstructed, to be confirmed).

## Consequences

- **Positive:** model and code cannot drift silently: the compile, the model validation test and the
  process tests catch it. A delegate is thin glue around one inbound port and fits the hexagon
  ([ADR-0002](0002-hexagonal-architecture-for-the-backend.md)).
- **Negative / trade-offs:** the engine scales with the service; there is no separate worker to scale
  or deploy independently. Delegate logic runs inside the engine transaction, so a slow call holds
  the job. After every model change `./gradlew generateBpmnModels` has to run and its output be
  committed; a designIQ release contains only the `.bpmn`.
- **Not covered:** external task workers. The naming rule still allows a `Worker` suffix in
  `adapter.inbound.cibseven`, but the model validation forbids external tasks today. A remote engine
  or Camunda 8 is not considered here. The generated `adapter/process` is excluded from the
  architecture and mutation rules.
