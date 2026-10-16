# 0001 — Record decisions in ADRs, instructions for agents in AGENTS.md

- **Status:** Accepted
- **Date:** 2026-10-16
- **Origin:** Consolidated baseline; earlier history in git.

## Context

This repository is a blueprint, meant to be forked and made your own. Its value is not only the
working code but the reasoning behind its shape: why the backend is hexagonal, why mutation testing
gates every PR, why the OpenAPI spec is committed. That reasoning normally lives in people's heads
and pull-request threads, where a new contributor (human or AI agent) cannot find it.

Agents also need the working rules of the repo: build commands, architecture rules, conventions.
Every AI coding tool can read its own instruction file, and maintaining the same rules in several
files makes them drift. `AGENTS.md` is a cross-tool convention that Claude Code, Codex and other
agents read.

## Decision

**Decisions are ADRs.** We record architecturally significant decisions as Architecture Decision
Records in Michael Nygard's format: one Markdown file per decision under `docs/adr/`, numbered,
copied from [`0000-adr-template.md`](0000-adr-template.md), never renumbered. Each record has Status,
Context, Decision and Consequences, and it is committed in the same commit as the change it explains.

- The collection is a **baseline as of 2026-10-16**: each ADR describes today's decision. Earlier
  records and their history are in git.
- From now on an accepted ADR is not rewritten. A change of decision gets a new ADR that supersedes
  the old one. A small factual correction may add a dated note under the status.
- Procedures (how to run, test, release) are not decisions; they live in `CONTRIBUTING.md` and the
  `AGENTS.md` files.

**Instructions for agents are `AGENTS.md` files.** The root `AGENTS.md` holds the rules for the whole
repo, area files hold the rules of one area: `service/AGENTS.md` for the backend and
`frontend/AGENTS.md` for the frontend. There is no `CLAUDE.md` anywhere in the repo.

- Claude Code (v2.1.277 and later) reads `AGENTS.md` natively: the root file at start, and a
  subdirectory's `AGENTS.md` when it works there, as long as no `CLAUDE.md` or `CLAUDE.local.md`
  exists in the path. Codex and other agents read the same files.
- Claude-specific recipes live in `.claude/skills` and `.claude/agents`; Claude Code settings and
  hooks in `.claude/settings.json`.

## Consequences

- **Positive:** a forker inherits the why, not just the what. Agents read the constraints before they
  touch code, and every agent reads the same rules. Decisions are reviewed in PRs like any other
  change.
- **Negative / trade-offs:** a real decision means writing a record. Contributors must not add a
  `CLAUDE.md` or `CLAUDE.local.md`: one in the path switches Claude Code back to reading only
  `CLAUDE.md`, and the `AGENTS.md` rules silently stop applying.
- **Not covered:** an agent that reads neither `AGENTS.md` nor its area files needs to be pointed at
  them by hand. The ADRs record decisions, not how-tos; a reader looking for a procedure goes to
  `CONTRIBUTING.md`.
