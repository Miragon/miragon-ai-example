# 0005 — Track the latest stable major versions

- **Status:** Accepted
- **Date:** 2026-10-16
- **Origin:** Consolidated baseline; earlier history in git.

## Context

The stack sits on the newest major of nearly everything: Spring Boot 4, Kotlin 2.x, the CIB seven
Spring-Boot-4 line, React and its toolchain. That has a cost: newest majors have smaller
ecosystems, more breaking-change churn, and sometimes force an integration workaround (for example
the exclusion of `cibseven-webclient-web` in `service/app/build.gradle.kts`, which is not built for
the Spring Boot 4 line). Left unwritten, being on the newest versions is indistinguishable from
drift.

## Decision

We **deliberately track the latest stable major versions** across the whole stack, and accept the
upkeep.

The reason is the blueprint's job: it shows what the current technology makes possible, including
AI-driven development, which the newest tooling plus our guardrails (architecture tests
[ADR-0002](0002-hexagonal-architecture-for-the-backend.md), mutation gate
[ADR-0004](0004-mutation-testing-as-a-blocking-pr-gate.md), the checked-in contract
[ADR-0003](0003-openapi-as-the-checked-in-contract.md)) are meant to enable. A blueprint pinned to
yesterday's versions would misrepresent the starting point we recommend.

Mechanics:

- **Dependabot** proposes updates for Gradle, the dev-stack images, both npm projects, the frontend
  container image and GitHub Actions (`.github/dependabot.yml`). Every version is exact-pinned.
- **Every bump runs the full gate set** (build, architecture, contract drift, mutation, frontend
  verify, Bruno, Playwright), so updates are taken continuously instead of in a big bang.
- **Patch and minor updates auto-merge** once the gates are green
  (`.github/workflows/dependabot-automerge.yml`). **Majors are merged by hand.**
- A red Dependabot PR is handed to Claude (`.github/workflows/dependency-repair.yml`), which tries
  to bring it back to green and never merges.
- **No exceptions in `dependabot.yml`.** When an update cannot be taken yet, a maintainer comments
  `@dependabot ignore this major version` (or `… this minor version`, `… this dependency`) on its
  PR. The ignore stays until the blocker is fixable, then it is lifted again.

## Consequences

- **Positive:** a forker starts modern instead of mid-migration, and AI-assisted development is
  exercised against today's tooling and guardrails.
- **Negative / trade-offs:** a higher maintenance cadence and early-adopter friction (thinner
  community answers, integration workarounds); a long-dormant fork may need an upgrade pass before it
  builds. An ignore set by PR comment is not visible in the repo files.
- **Not covered:** "stable" means releases, not release candidates or pre-releases. The gates, not a
  calendar, decide whether a bump lands; a bump the gates do not exercise (for example a runtime-only
  behaviour change) can still slip through.
