# 0002 — Hexagonal backend, enforced by ArchUnit and Konsist

- **Status:** Accepted
- **Date:** 2026-10-16
- **Origin:** Consolidated baseline; earlier history in git.

## Context

The backend automates a BPMN process with many collaborators: a process engine, JPA persistence, a
simulated dealer, insurance, notification and contract systems. If controllers, delegates and the
engine client call each other freely, business logic leaks into infrastructure and the process
becomes untestable in isolation. A blueprint has to show a structure that stays sane as this fan-out
grows, and it has to make that structure checkable, so a forker or an AI agent cannot quietly erode
it.

Architecture rules come in two kinds. Rules about the resolved dependency graph (which package uses
which, whether a class is an interface, how many inbound ports a class depends on) are visible in
bytecode. Rules about source shape (how many top-level declarations a file holds, import style) are
erased by the Kotlin compiler and visible only in source.

## Decision

The service is a **hexagon (ports and adapters)** under `io.miragon.blueprint`:

- `domain/`: pure Kotlin value objects and aggregates, no framework imports.
- `application/port/inbound`: one `*UseCase` (state-changing) or `*Query` (read) interface per
  operation. `application/port/outbound`: `*Port`, `*Repository` and `*Process` interfaces.
- `application/service`: one `*Service` per inbound port; it calls no other service and no inbound
  port.
- `adapter/inbound/{rest,cibseven}`: driving adapters (REST controllers, JavaDelegates).
- `adapter/outbound/{db,cibseven,contract,dealer,insurance,notification}`: driven adapters.
- `adapter/process`: the generated process API (bpmn-to-code, see
  [ADR-0011](0011-embedded-cib-seven-with-delegates-and-a-generated-process-api.md)) and engine
  configuration. A technical seam that fits neither side.
- There is no top-level `config` package. Spring configuration sits beside what it configures
  (`OpenApiConfiguration` and `GlobalExceptionConfiguration` in `adapter/inbound/rest`,
  `HistoryCleanupConfiguration` in `adapter/process`).

**The rules are machine-enforced with two tools, each for what only it can see.** ArchUnit checks
bytecode, Konsist checks Kotlin source. Both fail `./gradlew build`.

The Gradle build has two modules:

- `service/app`: the service.
- `service/common-architecture-tests`: the reusable rule suite (package
  `io.miragon.common.architecture`). The blueprint is prepared for more than one service under
  `service/`, each consuming this one module instead of copying the rules. A service wires the whole
  suite with one class: `class ArchitectureTest : ServiceArchitectureTest("<root package>")`.

The suite (`ServiceArchitectureTest` bundles the four parts):

- `HexagonalArchitectureTest` (ArchUnit): the layered dependency graph; domain depends only on the
  language; the application layer depends on no infrastructure; ports are interfaces and independent
  of services; a service implements exactly one inbound port and uses no other service or inbound
  port; adapters are inbound or outbound; outbound adapters never touch inbound ports; inbound
  adapters implement no outbound port; an inbound adapter takes at most one inbound port in its constructor (one use case per
  controller or delegate).
- `NamingConventionArchitectureTest` (ArchUnit): the suffixes per package (`UseCase`/`Query`;
  `Port`/`Repository`/`Process`; `Service`/`Configuration`; `Controller`/`Dto`/`Input`/`Mapper`/
  `Configuration`; `Delegate`/`Worker`/`Listener`; `PersistenceAdapter`/`Adapter`/`Mapper`/`Entity`/
  `Repository`).
- `BasicCodingGuidelinesTest` (ArchUnit): every class in the root package, no package cycles, no
  `println`.
- `KotlinSourceGuidelinesTest` (Konsist): at most one top-level class, interface or object per file,
  and no wildcard imports (except `java.util`).

The generated `adapter.process` package is excluded from the adapter-location rule, the layer
completeness check, the naming rules and the Konsist rules: it is machine-written and does not follow
the hand-written conventions.

## Consequences

- **Positive:** business logic is engine- and framework-agnostic and unit-testable behind ports. The
  structure is a fitness function, not a style guide: an agent gets local feedback from the build.
  Each rule is written against the representation where it is natural.
- **Negative / trade-offs:** more indirection than a layered CRUD app (a port, a service and an
  adapter per operation), and many small classes. Two DSLs to learn and two dependencies to keep
  current; a contributor has to know which tool owns which kind of rule. New structural rules go into
  one of these two tools; we do not add a third architecture framework.
- **Not covered:** today `service/app/src/test/kotlin/io/miragon/blueprint/ArchitectureTest.kt`
  is the only consumer of the rule module; the multi-service layout is prepared, not exercised.
  The generated `adapter/process` is not checked at all. The suite checks production code; test
  classes follow no layer rules.
