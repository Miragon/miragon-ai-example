# 0018 — React frontend next to the service

- **Status:** Accepted
- **Date:** 2026-10-16
- **Deciders:** Marco (@emaarco)

## Context

The example was headless: customers and the back office were played by Bruno and the CIB seven
Tasklist. That hides the most visible part of a process application, and it leaves the
OpenAPI contract ([ADR-0003](0003-openapi-as-the-checked-in-contract.md)) without a real consumer.
[miragon-blueprints/fullstack-example](https://github.com/miragon-blueprints/fullstack-example)
already has a frontend built for the same MiraVelo domain, with a toolchain and guardrails we use in
projects.

## Decision

We add a **React frontend in `frontend/`**, ported from the fullstack example and cut to this
process. It is a separate npm project; Gradle does not build it, so the Kotlin build stays the one
project at the root ([ADR-0014](0014-one-kotlin-gradle-stack-at-the-repo-root.md)).

- **Stack:** React, Vite, TypeScript, TanStack Router and Query, Tailwind with the Miragon theme,
  radix-based `shared/ui`, react-hook-form and zod, German and English copy.
- **Contract:** orval generates the API client, MSW mocks and zod schemas from
  `openapi/openapi.json`. `npm run api:check` fails when the generated client drifts from the
  contract.
- **Structure:** Feature-Sliced Design, checked by steiger and ESLint import rules; knip finds dead
  code.
- **Tests:** Vitest with MSW for units and pages, Playwright end to end against the running stack.
- **Dev serving:** Vite on port **5173** proxies `/api`, `/v3/api-docs`, `/engine-rest` and
  `/camunda` to the service on 8081, so the browser sees one origin and the service needs no CORS.

## Consequences

- **Positive:** the demo shows the customer's and the back office's view of the same process. A
  contract change reaches the UI through a regenerated client, and the gates catch drift.
- **Negative / trade-offs:** a second toolchain (Node 24, npm) and two more CI jobs (`frontend`,
  `e2e`). Dependabot watches one more ecosystem.
- **Neutral:** a JS dev server now exists, so portless could wrap it. The backend and Postgres ports
  still collide across worktrees, so [ADR-0006](0006-fixed-ports-for-v1-portless-as-the-upgrade.md)
  holds: fixed ports, `nonconcurrent`.
