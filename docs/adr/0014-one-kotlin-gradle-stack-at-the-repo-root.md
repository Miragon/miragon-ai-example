# 0014 — One Kotlin/Gradle stack at the repo root

- **Status:** Accepted
- **Date:** 2026-10-15
- **Deciders:** Thomas (@Hafflgav)
- **Supersedes:** [ADR-0013](0013-two-stack-variants-side-by-side-on-main.md)
- **Amended by [ADR-0018](0018-react-frontend-next-to-the-service.md) (2026-10-16):** `frontend/` is a
  second, npm-only project next to the Gradle build; the Kotlin service is still the one Gradle
  project at the root.

## Context

This repository is the base of a designIQ live demo. It came with two equivalent variants
([ADR-0013](0013-two-stack-variants-side-by-side-on-main.md)): `kotlin-gradle/` and `java-maven/`.
Each was a two-module build (`service:common-architecture-tests`, `service:app`) with its own copy
of the BPMN, DMN, forms, migrations and `application.yaml`. A parity workflow ("Blueprint Checks"),
variant markers and `scripts/create-starter.sh` kept the two in step.

For the demo that shape costs more than it gives:

- Every change to the service is made twice.
- designIQ reads one folder and keeps one model per file stem
  ([ADR-0016](0016-docs-is-the-designiq-workspace.md)). With two copies of every model, an edit in
  designIQ lands in one variant and the parity gate fails.
- The architecture-test module has exactly one consumer, but it needs its own subproject and build
  file.

The briefing asked to "remove Spring Boot". Asked about it, Thomas clarified that this meant the
Java/Maven variant. Spring Boot stays, because the demo uses what it brings: the CIB seven webapps
(Cockpit and Tasklist at `/camunda`, webclient at `/webapp`), `/engine-rest`, the REST API with its
OpenAPI contract, and Actuator.

## Decision

We keep **one Kotlin/Gradle project at the repo root** and drop the Java/Maven variant with
everything that only existed to keep two variants in step.

- `settings.gradle.kts` holds `rootProject.name = "miragon-ai-example"` and nothing else. There are
  no subprojects. Sources are in `src/main/kotlin` and `src/test/kotlin`, with the package root
  `io.miragon.blueprint` unchanged.
- The rule suite of `service/common-architecture-tests` moves into the test sources
  (`src/test/kotlin/io/miragon/common/architecture`). The service's `ArchitectureTest` runs it, and
  ArchUnit and Konsist are `testImplementation` dependencies. The rules themselves stay as they
  are.
- Spring Boot 4 stays with the CIB seven starters, springdoc, JPA, Flyway and Actuator.
- Removed: `java-maven/` and the Maven wrapper; the workflows `blueprint.yml`,
  `pre-merge-java-maven.yml` and `nightly-java-maven.yml`; `scripts/create-starter.sh`;
  `docs/starter.md`; every `variant:` marker. Also removed: kapt and the unused mockito-inline,
  mockito-kotlin, cibseven-bpm-junit5 and JGiven libraries.
- CI is `pre-merge.yml` and `nightly.yml`. Job names stay the same, including the required check
  `Mutation testing (PIT, gate 80)`.
- Commands lose their module prefix: `./gradlew build`, `bootRun`, `pitest`, `bootBuildImage`,
  `generateBpmnModels`.

Where the process models live is a separate decision
([ADR-0016](0016-docs-is-the-designiq-workspace.md)).

## Alternatives considered

- **Keep both variants (ADR-0013).** Every demo change would be made twice, and the duplicate models
  clash with designIQ's one-folder, one-stem rule.
- **Also remove Spring Boot** and run a plain embedded engine without REST. This was analysed and
  rejected once the briefing was clarified: the demo would lose Cockpit, Tasklist, the REST API and
  the Bruno scenarios.
- **Keep `kotlin-gradle/` and its two modules, only delete `java-maven/`.** That is the smaller
  diff, but with one variant left, the extra directory level, the `:service:app:` task prefixes
  and a module with one consumer buy nothing.

## Consequences

- **Positive:** one build, one copy of every resource, and shorter commands. Three fewer workflows
  run. designIQ sees each model once.
- **Negative / trade-offs:** teams and trainings bound to Java + Maven lose the in-tree variant.
  The last tree with both variants is commit `40da36e` (tag `original-process-v1`), which also
  exists in the upstream blueprint `miragon-blueprints/cibseven-embedded-example`. Changes made
  here are not ported to Java. The rule suite is no longer a module another build can depend on; a
  sibling repository copies the package instead.
- **Neutral:** older ADRs still name the paths they were written against (`service/app`,
  `kotlin-gradle/`, `service/common-architecture-tests`, `:service:app:` tasks). Each one carries an
  "Amended by ADR-0014" note instead of being rewritten.
