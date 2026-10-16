# 0003 — OpenAPI as the checked-in contract

- **Status:** Accepted
- **Date:** 2026-10-16
- **Origin:** Consolidated baseline; earlier history in git.

## Context

The Kotlin service exposes a REST API, and the React frontend in `frontend/` consumes it; other
clients, documentation tooling and the Bruno scenarios use it too. That boundary can be described
three ways: hand-write a spec and hope the code matches it; let each consumer hand-write its own types
and hope they match; or derive the contract from the code. The first two drift silently, and the
first symptom is a runtime 400 at the caller. We want the contract to be impossible to desync, and
readable without running anything.

## Decision

The **service is the source of truth**, and the contract is **generated but committed**:

1. **springdoc** serves the OpenAPI document from the annotated controllers. Every operation has an
   explicit `@Operation(operationId = …)`, so generated client method names stay stable.
2. `OpenApiSpecExportTest` fetches `/v3/api-docs`, re-serialises it deterministically (keys sorted,
   fixed two-space LF indenter, trailing newline, `servers` block dropped so the random test port
   causes no churn) and writes **`openapi/openapi.json`** at the repo root. It runs inside
   `./gradlew build`.
3. CI runs **`git diff --exit-code -- openapi/openapi.json`** after the build, a drift gate: a
   controller change without a regenerated spec fails the PR.
4. The **frontend's client is generated from the committed file.** orval generates the API client,
   the MSW mocks and the zod schemas into `frontend/src/shared/api/generated` (`npm --prefix frontend
   run api:generate`). CI runs `npm --prefix frontend run api:check`, which regenerates and fails
   when the committed client differs.

So a REST change travels in one PR: controller, `openapi.json`, regenerated frontend client.

**Why generated-but-committed:** against hand-written specs or types, it removes the drift class: the
gate fails the PR, not the caller at runtime. Against generated-at-build, the committed JSON makes
every API change visible in the PR diff, and consumers build without booting the service.

## Consequences

- **Positive:** one contract, gated on both sides; API changes are visible in review; the frontend
  and other consumers never need a running service to build.
- **Negative / trade-offs:** two generated artefacts live in git (`openapi.json` and the frontend
  client). Forgetting to regenerate is an expected failure mode; the gates make it safe, so they must
  not be disabled. Determinism of the export is a hard requirement, or the gate flaps.
- **Not covered:** the spec covers `/api/**` only (`springdoc.paths-to-match`); the engine REST API
  `/engine-rest`, the CIB seven webapps and `/actuator` are not part of the contract. The gates prove
  that code, spec and client agree, not that the API is backwards compatible: a breaking change is
  caught in review, not by a tool.

## Implementation notes

- **Swagger UI and the CIB seven webclient.** The webapp registers its own resource handlers and its
  own `OpenAPI` bean. `OpenApiConfiguration`'s bean is `@Primary`, so springdoc serves ours for
  `/api/**`. `/camunda`, `/webapp`, `/swagger-ui.html` and `/v3/api-docs` coexist on the running app;
  the test profile disables the webapps, so that part is checked by hand. If an upgrade breaks it,
  `springdoc-openapi-starter-webmvc-api` (spec only, no UI) is the fallback; a consumer only needs
  `/v3/api-docs`.
- **Jackson 3 date-time.** Spring Boot 4 ships Jackson 3, which writes dates as timestamps by
  default, and the webclient serves `/api` with its own mapper that ignores global config. springdoc
  types the fields as `string/date-time`, so DTO date fields are pinned with
  `@JsonFormat(shape = STRING)` to keep payload and contract in sync.
