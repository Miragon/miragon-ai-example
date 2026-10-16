---
name: review-change
argument-hint: "[PR number | branch | empty for the working tree against origin/main]"
description: "Review a PR, branch or diff for business correctness, architecture, security and missing counterparts; findings by severity with file:line and a failure scenario. Use when asked for a review. Read-only."
allowed-tools: Read, Glob, Grep, Bash(git *), Bash(gh pr view *), Bash(gh pr diff *), Bash(gh pr checks *), Bash(gh issue view *), Bash(gh api repos/*/pulls/*/comments), Agent
---

# Skill: review-change

A review that only ticks boxes is worthless; one finding with a reproducible scenario beats ten
style notes. The machine gates (ArchUnit/Konsist, drift checks, steiger, knip, mutation 80) already
run in CI; do not restate them, check what they cannot see. Generic code review is what the built-in
review tooling does; this skill adds what only this repo knows.

## 1. Collect the context

- Diff: `gh pr diff <n>` (open or merged), `git diff origin/main...<branch>`, `git show <sha>` for a
  single commit, or `git diff origin/main...` plus `git diff` for the working tree.
- Intent: the PR body and the linked issue (`gh issue view <n>`), its acceptance criteria.
- `gh pr checks <n>` if it is a PR. Leave existing review comments for step 5, so they don't bias you:
  `gh pr view <n> --comments` for the conversation, `gh api repos/{owner}/{repo}/pulls/<n>/comments`
  for inline comments.

## 2. Read the change in full

Read every changed file, not only the hunks, plus the callers and tests of changed code. Ask of each
change: is it what the issue asks for, and nothing else? Does it hold for the process's other paths
(withdrawal at any time, the error boundary, a 409 because the case moved on)?

## 3. Check the counterparts

| The change touches | It also needs |
|---|---|
| a JPA entity | a new `V<n>__*.sql` migration; no edited migration |
| a controller or DTO | `@Operation(operationId)`, the re-exported `openapi.json`, the regenerated client |
| a domain field shown to users | DTO, mapper, `openapi.json`, client, FSD entity, Bruno assertion |
| `bike-leasing.bpmn` | regenerated `adapter/process`, complete BPMNDI, a row in `executable-process.md`, feature map and glossary for changed ids; run the agent `review-process` |
| a delegate | its unit test (`create-delegate`) and a process test for the path |
| a service throwing what a delegate maps to `BpmnError` | `noRollbackFor` on `@Transactional` |
| a user-visible flow | copy in `de.ts` and `en.ts`, a Playwright spec, the Bruno scenario |
| a whole feature | run the agent `review-vertical-slice` |
| an accepted ADR | no rewrite: a new ADR supersedes it, a factual fix gets a dated note |
| Gradle modules, build or CI paths | working directories, relative paths in tests and CI, artifact and image names |
| a model in `original/` or a generated file edited by hand | a blocker |

## 4. Security and robustness

Input reaching the domain unvalidated, problem details leaking internals, secrets or credentials in
code or config, new dependencies without a pinned version, process variables carrying personal data
the engine does not need.

## 5. Report

Severity is absolute, not relative to this PR:

- **Blocker:** wrong business result, data loss, broken path, security hole, red gate.
- **Major:** a missing counterpart or test that lets a regression through, or a broken repo rule
  (ADR policy, frozen models) that loses information.
- **Minor:** maintainability, naming, a weak assertion.
- **Nit:** optional.

When unsure between two levels, pick the lower one and say why. Per finding: severity, `file:line`,
what goes wrong in which scenario, a suggested fix. Then: questions for the author, what looks good,
and whether existing review comments are already addressed.

Post to GitHub only when asked, and show the text first.
