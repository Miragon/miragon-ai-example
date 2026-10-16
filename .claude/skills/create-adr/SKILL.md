---
name: create-adr
description: Write a new Architecture Decision Record (ADR) under docs/adr/ in this repo's format (Status, Date, Context, Decision, Consequences). Use when the user asks to "write an ADR", "record an architecture decision", "take decision #n", or "document why we did X". Auto-numbers from the existing records, derives the filename slug from the title, copies docs/adr/0000-adr-template.md, and shows a full draft before writing.
allowed-tools: Read, Write, Glob
---

# Skill: create-adr

Write a new ADR following `docs/adr/` (see
[ADR-0001](../../../docs/adr/0001-record-architecture-decisions.md)). Every `.md` under `docs/` is a
designIQ model, so the file stem must be unique under `docs/`.

## Rules

- **An accepted ADR is not rewritten.** A reversal is a new ADR that supersedes the old one (flip its
  Status to "Superseded by …"). A later change that only makes a detail untrue adds a dated
  "Amended by" bullet under the old one's Status (see ADR-0002 for the form); its body stays.
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
   decision issue (#8–#11), also update `docs/architecture/sourcing-map.owm` and
   `sourcing-decisions.md` and close the issue from the PR; add the ADR to the list in
   `docs/README.md`.
