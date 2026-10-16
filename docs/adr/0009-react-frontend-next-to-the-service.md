# 0009 — React frontend next to the service

- **Status:** Accepted
- **Date:** 2026-10-16
- **Deciders:** Marco (@emaarco)
- **Origin:** Consolidated baseline; earlier history in git.

## Context

A process application has customers and a back office who need a UI. Without one, they are played by
Bruno and the CIB seven Tasklist, which hides the most visible part of the application.
[miragon-blueprints/fullstack-example](https://github.com/miragon-blueprints/fullstack-example)
has a frontend for the same MiraVelo domain, with a toolchain and guardrails we use in projects.

## Decision

We have a **React frontend in `frontend/`**, ported from the fullstack example and cut to this
process. It is a separate npm project; Gradle does not build it.

- **Stack:** React, Vite, TypeScript, TanStack Router and Query, Tailwind with the Miragon theme,
  radix-based `shared/ui`, react-hook-form and zod, German and English copy.
- **API client:** generated from the checked-in contract
  ([ADR-0003](0003-openapi-as-the-checked-in-contract.md)).
- **Structure:** Feature-Sliced Design, checked by steiger and ESLint import rules; knip finds dead
  code.
- **Tests:** Vitest and Testing Library with MSW for units, widgets and pages; Playwright end to end
  against the running stack.
- **Dev serving:** the Vite dev server proxies `/api`, `/v3/api-docs`, `/engine-rest`, `/camunda`
  and `/webapp` to the service, so the browser sees one origin and the service needs no CORS.
- **Gates:** `npm --prefix frontend run verify` (format, ESLint, steiger, knip, typecheck, Vitest)
  and the build run in CI on every PR, Playwright runs against the started stack.

The frontend's own rules are in [`frontend/AGENTS.md`](../../frontend/AGENTS.md).

## Consequences

- **Positive:** the demo shows the customer's and the back office's view of the same process. A
  contract change reaches the UI through a regenerated client, and the gates catch drift.
- **Negative / trade-offs:** a second toolchain (Node, npm) and more CI jobs (frontend verify,
  Playwright). Dependabot watches more ecosystems.
- **Not covered:** no authentication or roles: anyone who opens the frontend can act as customer,
  dealer or back office. The frontend is not served by the service; in containers it is its own
  nginx image ([ADR-0007](0007-build-and-deployment-approach.md)).
