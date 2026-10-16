---
name: implement-issue
argument-hint: "<issue number or URL>"
description: "Take a GitHub issue from its acceptance criteria to a verified change along the feature chain, with a plan-approval stop. Use when asked to implement or work on issue #n. Never commits, pushes or opens a PR unless asked."
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Skill, Agent
---

# Skill: implement-issue

The human owns the requirement and the plan; the agent owns the legwork. Two stops are mandatory:
the **plan approval** (step 3) and any **business decision** the issue leaves open.

## 1. Read the issue

```bash
gh issue view <n>
gh issue view <n> --json comments --jq '.comments[].body'
```

Collect the acceptance criteria, the anchor block's element ids, linked issues and the matching part
of the original model in `docs/use-cases/bike-leasing/original/` (read-only). An open **feature**
issue this one depends on blocks the work: say so and stop. An open **decision** issue (#8–#10) does
not block; its question becomes an open question of the plan. Read
`docs/use-cases/bike-leasing/process/executable-process.md` and the feature's row in
`docs/use-cases/bike-leasing/feature-map/feature-map.md`.

## 2. Separate facts from assumptions

List every detail the plan needs. Each one is **derived** (cite issue, original model or code) or an
**assumption**. Business assumptions (deadlines, gateway conditions, error paths, copy, who does a
user task) become questions, not choices.

## 3. Plan, then stop

If the issue changes the model, run steps 1–2 of skill `automate-process` now (review, restate the
change, collect the business questions); its editing steps come after the approval.

Write the plan as a checklist along the chain, skipping hops the issue does not touch:

1. Model change in `docs/use-cases/bike-leasing/process/bike-leasing.bpmn` (skill `automate-process`)
2. Domain and application: ports, services, domain rules
3. Delegates and their tests (skill `create-delegate`)
4. REST and persistence (skills `create-rest-controller`, `create-persistence-adapter`), including
   the regenerated contract and client
5. Frontend (skill `create-feature-slice`)
6. Bruno scenario; a Playwright spec when users see the change
7. Design artifacts: a row in `executable-process.md` for every difference from the source model,
   the feature map (`.bpmn` + `.md`), the t.BPM model if the core changes, the glossary, the use case,
   `scope-comparison.md` and the README roadmap when a capability lands, anchor blocks of issues whose
   element ids change, the PNGs in `docs/assets/`

Per item: the files, the test that proves it, the acceptance criterion it serves. Show the plan with
the open questions and **wait for approval**. Do not write code before it.

## 4. Implement test-first

One item at a time: failing test, code, green test, the targeted check from the Verify table in
`AGENTS.md`. Follow `service/AGENTS.md` and `frontend/AGENTS.md`. When the plan turns out wrong,
stop and say what changed instead of drifting.

## 5. Verify

Run the "Before a PR" row of the Verify table. A targeted run proves nothing about tests you did not
touch. If the stack runs, check user-visible changes with skill `test-in-browser`. Then let the agent
`review-vertical-slice` walk the feature and fix what it reports.

## 6. Report

| Acceptance criterion | Evidence (test, check, screenshot) | Status |
|---|---|---|

Then: files changed, checks run with their result, assumptions made, what was not verified and why.

## Only when asked

- **Commit:** stage files by name (never `git add -A`), Conventional Commit, `Closes #<n>` in the
  body. Never stage `.context/` or local settings.
- **PR:** `gh pr create --base main`, title = the commit subject, body: what, why, how it was verified,
  `Closes #<n>`; offer to attach screenshots from `.context/<issue>/`.
- **Issue:** comment or change labels only on request; keep the anchor block at the top of the body.
