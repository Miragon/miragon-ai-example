---
name: create-feature-slice
argument-hint: "[<layer>/<slice-name>]  e.g. features/sign-contract or entities/bike"
description: Scaffold a new Feature-Sliced Design slice under frontend/src honoring the layer rules — a public index.ts, purpose-named segments (ui/model/api), downward-only imports, the generated-import restriction, and the processes ban. Use when the user asks to "add a feature", "create an FSD slice", "add an entity/widget/page", or "scaffold a frontend feature". Leaves steiger, knip, and tsc green.
allowed-tools: Read, Write, Edit, Bash, Glob
---

# Skill: create-feature-slice

Scaffold one Feature-Sliced Design slice under `frontend/src`. The layering is CI-enforced by
**steiger `--fail-on-warnings`**, ESLint `no-restricted-imports` and knip
([ADR-0018](../../../docs/adr/0018-react-frontend-next-to-the-service.md)). Read
`frontend/AGENTS.md` first.

## The layers (import only downward)

`app/` → `pages/` → `widgets/` → `features/` → `entities/` → `shared/`

- **entities/** own reads: a read model and its query hooks (`bike`, `leasing-application`,
  `user-task`).
- **features/** own writes: one mutation, its cache invalidation and a toast
  (`check-credit-rating`, `sign-contract`, `withdraw-application`, …).
- **widgets/** compose features and entities; **pages/** compose widgets; routes in `app/routes/`
  hand plain props to router-free pages.

## Hard rules

- **No `processes/` layer — ever.** "Process" means the BPMN model here.
- **Every slice has a public `index.ts`**; other slices import only through it.
- **Segments are purpose-named:** `ui/`, `model/`, `api/` (plus `lib/`, `config/`).
- **Only `entities/*/api` and `features/*/api` import `@/shared/api/generated`**, e.g.
  ```ts
  // entities/bike/api/queries.ts
  export { useListBikes } from "@/shared/api/generated/endpoints";
  // entities/bike/index.ts
  export { useListBikes } from "./api/queries";
  ```
- Three steiger rules are off on purpose (`insignificant-slice`, `repetitive-naming`,
  `shared-lib-grouping`); the reasons are in `steiger.config.ts`.
- Copy lives in `shared/i18n` in German and English.

## Instructions

1. **Resolve layer + name** (kebab-case) from `$ARGUMENTS`, else ask. Reject `processes/`.
2. **Create the segments** the slice needs under `frontend/src/<layer>/<slice-name>/`.
3. **Write the public `index.ts`** with only the slice's public surface.
4. **Wire it in** from the layer above. If the backend call does not exist yet, run
   [`sync-api-client`](../sync-api-client/SKILL.md) first.
5. **Verify green:**
   ```bash
   npm --prefix frontend run lint:fsd
   npm --prefix frontend run lint:deadcode
   npm --prefix frontend run typecheck
   npm --prefix frontend run test
   ```
   A slice referenced nowhere trips knip. Report the files created and the checks' results.
