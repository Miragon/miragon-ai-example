---
name: create-adr
description: "Write a new Architecture Decision Record under docs/adr/ in this repo's format (Status, Date, Context, Decision, Consequences). Use when the user asks to write an ADR, record an architecture decision, take a sourcing decision (issues 8 to 10), or document why something was done. Numbers it after the highest existing record, copies docs/adr/0000-adr-template.md, marks a superseded ADR, and shows the full draft before writing."
allowed-tools: Read, Write, Edit, Glob, Grep
---

# Skill: create-adr

Write a new ADR following `docs/adr/` (see
[ADR-0001](../../../docs/adr/0001-record-architecture-decisions.md)). Every `.md` under `docs/` is a
designIQ model, so the file stem must be unique under `docs/`.

## Rules

- **An accepted ADR is not rewritten.** A changed decision is a new ADR that supersedes the old one:
  flip the old one's Status to "Superseded by [NNNN](…)" and leave its body as it is. A small factual
  correction (a renamed path) may add one dated note under the Status.
- **Record what is decided, not what is planned.** Code proves an implementation, not the motive:
  a reason the user did not state is marked "(⚠ reconstructed, to be confirmed)".
- Numbers have gaps (removed records live in git); never reuse a number.
- One decision per record, roughly one screenful, present tense.
- Plain-text Markdown: designIQ shows it unrendered, so keep it readable raw.

## Instructions

1. **Glob `docs/adr/*.md`.** Stop and ask if `0000-adr-template.md` is missing; never invent one.
2. **Next number** = highest four-digit prefix + 1, zero-padded.
3. **Slug** = kebab-cased title. Path: `docs/adr/{NNNN}-{slug}.md`.
4. **Read the template** and keep its sections (Status, Date, Deciders, Context, Decision,
   Consequences, optional Implementation notes last).
5. **Gather content** the request does not already answer: context and options, the decision and its
   mechanics, consequences, status (default `Accepted`).
6. **Draft and cross-link** related ADRs by relative path (hexagon → `0002-…`, contract → `0003-…`,
   mutation gate → `0004-…`). Show the full draft and ask: "Write this to `{path}`? (yes / edit /
   cancel)".
7. **Write and report.** Remind the user to commit the ADR with the change it documents. If it takes a
   decision issue (#8–#10), also update `docs/architecture/sourcing-map.owm` and
   `sourcing-decisions.md` and close the issue from the PR; add the ADR to the list in
   `docs/README.md`.
