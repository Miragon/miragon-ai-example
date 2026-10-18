# `.claude/`

Claude Code tooling for this repo. The guidance itself is in the `AGENTS.md` files (root,
`service/`, `frontend/`), which Claude Code reads natively; this folder holds what only Claude Code
uses.

## Skills — `.claude/skills/<name>/SKILL.md`

Workflows, loaded when a task matches their description.

| Skill | When |
|---|---|
| `implement-issue` | take a GitHub issue to a verified change, with a plan approval stop |
| `automate-process` | change the executable BPMN model, wire its glue code, validate it (three nets) |
| `create-rest-controller` | one endpoint with its `@WebMvcTest`, carried through `openapi.json` to the client |
| `create-persistence-adapter` | entity, repository, mapper, adapter, migration and `@DataJpaTest` |
| `create-feature-slice` | one FSD slice with its Vitest test |
| `create-delegate` | one JavaDelegate for a service task, or the unit test for an existing one |
| `review-change` | review a PR or diff, findings by severity with a failure scenario |
| `test-in-browser` | check a flow in the running app with Playwright MCP, screenshots as evidence |
| `create-adr` | record a decision in `docs/adr/` |

The layer skills are small recipes next to the workflows: each shows one production pattern and one
test pattern taken from the code, so the agent writes the test first and matches the existing style.
`implement-issue` and `automate-process` call them.

## Subagents — `.claude/agents/<name>.md`

Read-only reviewers with their own context.

| Agent | Does |
|---|---|
| `review-process` | checks the model against its glue code, tests and modelling conventions |
| `review-vertical-slice` | walks one feature end to end (BPMN → ports → service → controller or delegate → openapi.json → client → FSD slice → Bruno → Playwright → design artifacts) |

## Settings and hooks

- `settings.json` denies edits to generated files (`adapter/process`, `openapi.json`, the orval client),
  allows the build, lint and read-only `gh` commands, and wires two hooks. Personal additions go in
  `settings.local.json` (git-ignored).
- `hooks/block-env-files.js` (PreToolUse, Node) refuses any Read, Edit, Write, Grep or Bash call whose
  target (`file_path`, `path` or `command`) names a `.env` or `.env.*` file; `.env.example` stays
  readable, and text that only mentions `.env` (an edit to the docs) passes.
  Exit 2 blocks the call and tells Claude why. It is an example of a guard a deny rule cannot express
  as well: it also inspects shell commands (`cat .env`, `source ./stack/.env`).
- `hooks/lint-bpmn.sh` (PostToolUse) runs bpmnlint after every edit of an executable `.bpmn` and
  hands the findings back to Claude. It changes nothing.
- `settings.json` also enables two pinned plugins, `bpmn-cli@miragon-bpmn` (skill `bpmn-cli:bpmn`) and
  `operate@miragon` (skill `operate:operate`, `/operate:status`), from marketplaces fixed to a release
  tag. Claude Code installs them once you trust the folder; cloud sessions do not load them. Its `env`
  sets `BPMN_KEEP_FLOW_IDS=1`, so bpmn-cli keeps flow ids stable.
- `../.mcp.json` provides the Playwright MCP server (pinned) for `test-in-browser`.

## What goes where

| Kind | Place | Example |
|---|---|---|
| orientation, boundaries, conventions | `AGENTS.md` of the area | "Spring Data types stop at the adapter" |
| a repeatable workflow | a skill | implementing an issue |
| an independent read-only review | an agent | model ↔ glue consistency |
| a rule a machine can check | a test, linter or CI step | ArchUnit, bpmnlint, drift checks |
| feedback right after an edit | a hook (small, no side effects) | bpmnlint on `.bpmn` |
| a hard no on a path | a deny rule in `settings.json` | editing generated files |
| a hard no that needs logic | a PreToolUse hook (exit 2 blocks) | no `.env` access, also via Bash |

One fact, one place: a skill links to `AGENTS.md` instead of repeating it. Keep skill descriptions
quoted (an unquoted ` #` starts a YAML comment and cuts them off). `.claude/logs/` is local and
git-ignored.
