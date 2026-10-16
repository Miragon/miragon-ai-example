# 0004 — Mutation testing as a blocking gate at 80

- **Status:** Accepted
- **Date:** 2026-10-16
- **Origin:** Consolidated baseline; earlier history in git.

## Context

Line coverage answers "was this line executed?", not "would a test notice if the code broke?". That
gap matters most for AI-assisted tests, which reliably chase coverage while writing weak assertions
(assert-not-null instead of assert-a-value). A blueprint that invites agents to write tests needs a
gate that grades assertion strength. Mutation testing is slower than unit tests, so the gate must not
become the thing that stalls merges.

## Decision

We run **PIT (pitest)** as a **blocking gate** with `mutationThreshold = 80`, configured in
`service/app/build.gradle.kts`.

- **On PRs it runs diff-scoped.** `.github/workflows/pre-merge.yml` maps the backend `*.kt` files the
  PR changed to their classes and passes them as `-PmutationTargetClasses`. The PR is blocked on the
  score of the changed classes; a PR without backend changes skips it.
- **Nightly runs the full-module sweep.** `.github/workflows/nightly.yml` mutates all of
  `io.miragon.blueprint.*` and uploads the report.
- **Excluded from mutation:** the generated `adapter.process` package and the application bootstrap
  class `CibsevenBikeLeasingApplication`. Everything else is mutated, the delegates in
  `adapter.inbound.cibseven` included; they have MockK unit tests.
- **Excluded as killing tests:** the process tests (`process.*`) and the architecture tests
  (`architecture.*`). The kill-set is the fast unit tests: MockK, `@WebMvcTest`, `@DataJpaTest`.

**Why 80 and not 100:** PIT mutates JVM bytecode, and Kotlin emits synthetic constructs (null checks
in value classes, safe-call mapping, data-class accessors) that produce equivalent mutants no test
can kill. 100 is unreachable without the commercial arcmutate Kotlin plugin; 80 is the honest bar.

**Why process and architecture tests do not count:** they boot the engine or scan the whole
classpath, which makes every mutation run slow and less deterministic without adding mutation signal.

How to run it and read a surviving mutant is in [CONTRIBUTING.md](../../CONTRIBUTING.md).

## Consequences

- **Positive:** tests are graded on whether they would catch a real fault. Weak assertions surface as
  surviving mutants, at PR time, on the code the PR changed.
- **Negative / trade-offs:** mutation testing is slow, hence the diff-scoped PR run and the separate
  nightly sweep. Gate 80 over a single changed class is stricter than over the module, so a PR that
  touches only an equivalent-mutant-heavy class can fall below 80. The PR job's name
  (`Mutation testing (PIT, gate 80)`) is the branch-protection required check and must not be
  renamed.
- **Not covered:** code that only the process tests exercise (the walk through the model, the BPMN
  wiring) is not graded by mutation; the process tests guard it. The generated process API is not
  mutated. The threshold stays below 100 until a tool filters Kotlin's equivalent mutants.
