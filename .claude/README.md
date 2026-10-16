# `.claude/`

AI tooling for this repo. The authoritative guidance is the root `AGENTS.md` (imported by
`CLAUDE.md`, see ADR-0005); the frontend has its own `frontend/AGENTS.md`.

## Skills — `.claude/skills/<name>/SKILL.md`

Task recipes an agent can follow.

| Skill | When |
|---|---|
| `automate-process` | adjust the executable BPMN model and wire its glue code |
| `create-rest-controller` | add a REST endpoint (inbound port → service → controller) |
| `create-delegate` | add a JavaDelegate for a BPMN service task |
| `create-persistence-adapter` | add an outbound JPA persistence adapter |
| `sync-api-client` | propagate a backend REST change to `openapi.json` and the frontend client |
| `create-feature-slice` | scaffold a new FSD slice in `frontend/src` |
| `verify-model-visually` | lint and visually review a BPMN model after editing it |
| `create-adr` | record an architecture decision in `docs/adr/` |

## Subagents — `.claude/agents/<name>.md`

| Agent | Does |
|---|---|
| `review-process` | checks BPMN model ↔ glue-code consistency |
| `review-vertical-slice` | walks a feature end to end (BPMN → port → service → controller → openapi.json → generated client → FSD slice → Bruno → Playwright → design artifacts) and reports gaps |

`.claude/logs/` is local and git-ignored.
