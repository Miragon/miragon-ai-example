# 0017 — Rebase onto the simplified event-subprocess process

- **Status:** Accepted
- **Date:** 2026-10-16
- **Deciders:** Marco (@emaarco)
- **Supersedes:** [ADR-0015](0015-reduce-the-executable-process-to-a-core.md)

## Context

[ADR-0015](0015-reduce-the-executable-process-to-a-core.md) cut the original process to a 23-node
core: a DMN credit check, an event-based signature deadline, a bike-unavailable clarification loop
with compensation and a 14-day withdrawal period. That core still reads as complex. It has a
compensation throw, two timers, an error-driven loop and its only human task hides behind an
out-of-stock bike. The example also had no UI, so the human work happened in the CIB seven Tasklist
and in Bruno.

The example should show how Miragon automates a process with AI in the loop: a process a business
audience understands in one look, a UI on top of it, and visible room for the next features.
Miragon already maintains the MiraVelo reference models in
[Miragon/miravelo-reference](https://github.com/Miragon/miravelo-reference/tree/541f8e0dda1672aad18b5d84b45cdfae807efe8b/bpmn/bike-leasing).
Its stage `03-bike-leasing-event-subprocess-simplified.bpmn` is exactly such a process.

## Decision

The executable `bikeLeasingProcess` is the reference model
`03-bike-leasing-event-subprocess-simplified.bpmn` (20 flow nodes):

- `Leasing request received` → `Validate application` (error `Application invalid` → rejection)
  → `Check credit rating` (**user task**) → `Solvent?`.
- Solvent: `Send contract` → `Contract signed` → `Issue insurance policy` and
  `Order bike from dealer` in parallel → `Handover reported` → `Leasing active`.
- Not solvent or invalid: `Send rejection` → `Application rejected`.
- An interrupting event subprocess `Application withdrawn` lets the customer withdraw at any time;
  the back office does `Reverse application` (**user task**) → `Application cancelled`.

The flow-node ids of the reference are kept. The executable copy differs only where the engine or
this repo's gates require it, and `executable-process.md` lists each difference:

- `Order bike from dealer` is a delegate (`#{orderBikeDelegate}`) instead of an external task, so
  the engine stays embedded and every service task follows the same pattern. It gets `asyncBefore`
  and the retry cycle `R3/PT10S` for the incident demo.
- The boundary event `boundary_applicationInvalid` is named `event_applicationInvalid`, and the
  sequence flows get descriptive `flow_*` ids, as `@miragon/rules/element-id-naming` requires.
- `historyTimeToLive` is `P180D`, and the rejection merge is named `Rejection reason`.

The rule behind `Validate application` is not in the reference. **Assumption:** an application is
valid when the applicant is at least 18 and the bike is in the dealer's catalogue.

The DMN `checkCreditRating` is no longer deployed. It moves to `original/` as frozen reference,
next to the original models, which stay the target picture. Every capability of the original that
the new core lacks is a GitHub feature issue (see `scope-comparison.md` and the feature map).

## Consequences

- **Positive:** the picture fits a slide and every element has a business meaning. Both human tasks
  are on the happy path or one click away, so the new frontend inbox
  ([ADR-0019](0019-back-office-task-ui-own-inbox-in-the-frontend.md)) is part of every demo. The
  model is one Miragon already teaches with, and the reference is linked by commit.
- **Negative / trade-offs:** the REST contract and the element ids change again, so every issue
  anchor is rewritten. The credit check is manual, a customer who never signs waits forever, and a
  withdrawal is reversed by hand. These are the first feature issues.
- **Neutral:** the original stays frozen in `original/`, and the tag `original-process-v1` is not
  moved.
