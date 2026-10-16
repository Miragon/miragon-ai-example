---
name: sync-api-client
description: Propagate a backend REST change all the way to a clean frontend. Regenerate the OpenAPI contract from the Kotlin controllers, run orval to regenerate the TanStack Query hooks / MSW handlers / zod schemas, fix the resulting TypeScript across the FSD slices, and leave both drift gates (git diff on openapi.json and api:check) green. Use after adding or changing a controller or its request/response types, or when the frontend types are stale, api:check fails, or the user asks to "sync the api client" or "regenerate the client".
allowed-tools: Read, Write, Edit, Bash, Glob, Grep
---

# Skill: sync-api-client

The backend is the single source of truth; the contract is **generated but committed** and
**drift-gated on both sides** ([ADR-0003](../../../docs/adr/0003-openapi-as-the-checked-in-contract.md),
[ADR-0018](../../../docs/adr/0018-react-frontend-next-to-the-service.md)). Do the hops **in order**.

`Kotlin controllers → openapi/openapi.json → orval → frontend/src/shared/api/generated/ → FSD slices`

## Instructions

### Step 1 — Regenerate the OpenAPI spec (repo root)

```bash
./gradlew test --tests "io.miragon.blueprint.openapi.OpenApiSpecExportTest"
git diff --stat -- openapi/openapi.json
```

The test boots the app and writes `openapi/openapi.json` deterministically. **Never hand-edit it.** An
endpoint missing from the spec usually lacks `@Operation(operationId = …)` — fix the controller (see
[`create-rest-controller`](../create-rest-controller/SKILL.md)) and rerun.

### Step 2 — Regenerate the TypeScript client

```bash
npm --prefix frontend run api:generate
```

orval reads `../openapi/openapi.json` and rewrites `frontend/src/shared/api/generated/` (hooks in
`endpoints.ts`, MSW handlers, `model/`, `zod.ts`). Generated and committed — never hand-edit it.

### Step 3 — Fix the TypeScript across the slices

```bash
npm --prefix frontend run typecheck
```

- Only `entities/*/api` and `features/*/api` import `@/shared/api/generated`; update the re-export
  there, not the call sites.
- `features/submit-leasing-request/model/submit-form-schema.ts` extends the generated zod schema.
  Adjust its messages if a field changed, but **never** add `age >= 18` or a catalogue check: those are
  the process's *Validate application* rule, and duplicating them hides the rejection path.
- Widgets and pages consume entity/feature hooks through each slice's `index.ts`.

### Step 4 — Prove both gates are green

```bash
git diff --exit-code -- openapi/openapi.json   # after committing: the backend gate CI runs
npm --prefix frontend run api:check            # orval + git diff on generated/
npm --prefix frontend run verify               # format, eslint, steiger, knip, tsc, vitest
```

### Step 5 — Report

Summarise the spec diff, the generated files that moved, and the slices you edited. Remind the user to
commit `openapi/openapi.json`, the regenerated client and the controller change together, and to
update the Bruno scenario if the request shape changed.
