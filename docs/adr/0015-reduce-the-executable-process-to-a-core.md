# 0015 — Reduce the executable process to a core

- **Status:** Superseded by [ADR-0017](0017-rebase-onto-the-simplified-event-subprocess-process.md)
- **Date:** 2026-10-15
- **Deciders:** Thomas (@Hafflgav)

## Context

The original executable model `bikeLeasingProcess` has 43 flow nodes and calls a second process,
`cancelBikeOrder`. Besides the lease itself, it contains:

- a "Conclude contract" sub-process with an escalation;
- a 7-day signature reminder;
- a parallel insurance branch with policy compensation;
- a withdrawal event sub-process that reverses every completed step;
- an order cancellation with return clarification and cancellation costs.

The REST side adds an application list, a back-office inbox and a bike catalogue.

That is too much to explain in a live demo. designIQ's validator already flags the original for 14
activities against the 7±2 rule. The demo needs a process that a business audience finds credible,
that runs end to end today, and that leaves visible room for the next features.

## Decision

The executable `bikeLeasingProcess` is reduced to a **core: credit check, contract with a 14-day
deadline, and bike order including the bike-unavailable clarification**. It has 23 flow nodes.

- Credit check: `Check credit rating` (DMN `checkCreditRating`) leads to `Solvent?`.
- Contract: `Send contract`, then the process waits for `Contract signed` or `14 days`. A customer
  who is not solvent, or who has not signed after 14 days, goes to `Rejection reason`, gets
  `Send rejection` and ends in `Application rejected`.
- Order: `Order bike from dealer`. If the dealer reports the bike out of stock, the error
  `Bike unavailable` opens the user task `Clarify alternative with customer` (Tasklist form or
  REST). If an alternative is found, the bike is ordered again. If not, `Trigger reversal`
  compensates `Send contract` with `Cancel contract`, then `Send cancellation confirmation` and
  `Contract cancelled`.
- Lease: `Handover reported`, `Await end of withdrawal period` (14 days), `Leasing active`.
- The model is flattened. There is no "Conclude contract" sub-process and no escalation: the
  deadline timer leads straight to `Rejection reason`.
- Every surviving element keeps its original id, and only three sequence flows are new
  (`flow_deadlineToRejection`, `flow_signedToBikeSource`, `flow_orderedToHandover`). The
  configuration changes only where the cut forces it. `Compensate contract` hangs on
  `Send contract`, because its sub-process is gone. `Order bike from dealer` loses its audit
  listener. `Contract cancelled` is a plain end event instead of a terminate end, because no
  parallel token is left to stop.
- The code follows the model: use cases, ports, adapters, REST endpoints and Bruno scenarios of the
  removed capabilities are gone. The bike model, the status `WITHDRAWN` and the technical
  `BikeOrderAuditListener` are gone too. Flyway `V2__drop_bike_portfolio.sql` drops the catalogue
  table.

Every removed capability is a GitHub feature issue. Its body says what the original did, and its
anchor pins it to the core element it would extend (the application list is anchored to the
process as a whole). The stages are the ones on the feature map.

- Stage 1 – Customer follow-up:
  - [#1](https://github.com/miragon-blueprints/miragon-ai-example/issues/1)
    Remind the customer to sign the contract after 7 days
- Stage 2 – Insurance:
  - [#2](https://github.com/miragon-blueprints/miragon-ai-example/issues/2)
    Issue the insurance policy alongside the bike order
- Stage 3 – Withdrawal:
  - [#3](https://github.com/miragon-blueprints/miragon-ai-example/issues/3)
    Let the customer withdraw the application and reverse completed steps
  - [#4](https://github.com/miragon-blueprints/miragon-ai-example/issues/4)
    Cancel a placed bike order at the dealer (return clarification and cancellation costs);
    depends on the withdrawal
- Stage 4 – Portal and back-office views:
  - [#5](https://github.com/miragon-blueprints/miragon-ai-example/issues/5)
    Give the back office an inbox of pending alternative clarifications
  - [#6](https://github.com/miragon-blueprints/miragon-ai-example/issues/6)
    List leasing applications for the customer portal
  - [#7](https://github.com/miragon-blueprints/miragon-ai-example/issues/7)
    Offer a bike catalogue with models and live dealer availability

The simulated systems behind the core raise open sourcing decisions:
[#8](https://github.com/miragon-blueprints/miragon-ai-example/issues/8) contract and e-signature,
[#9](https://github.com/miragon-blueprints/miragon-ai-example/issues/9) customer notification,
[#10](https://github.com/miragon-blueprints/miragon-ai-example/issues/10) dealer integration and
[#11](https://github.com/miragon-blueprints/miragon-ai-example/issues/11) back-office task UI.

The original is preserved in two places:

- The tag `original-process-v1` points to commit
  [`40da36e`](https://github.com/miragon-blueprints/miragon-ai-example/tree/40da36ef7cc20423a7312ca29d5a1c493043ca9c).
  The tag is not moved; a later baseline gets a new tag.
- Frozen byte-for-byte copies:
  [original-bike-leasing.bpmn](../use-cases/bike-leasing/original/original-bike-leasing.bpmn) and
  [original-cancel-bike-order.bpmn](../use-cases/bike-leasing/original/original-cancel-bike-order.bpmn).
  They are not deployed, not generated and not linted.
  [`scope-comparison.md`](../use-cases/bike-leasing/original/scope-comparison.md) maps the
  original's capabilities to the core and to the issues.

**Why this cut:**

- **It tells the demo story.** One picture shows a decision table, a deadline, a human task, an
  error raised by the domain and one compensation. The order task's retries also give the incident
  demo (`BIKE-FAIL`).
- **It is business-credible.** A customer who never signs is rejected after 14 days instead of
  waiting forever, and a bike that is out of stock is a real exception the back office solves with
  the customer.
- **It keeps the original ids.** The generated process API, the delegates, the process tests and
  the remaining Bruno scenarios carry over. Core and original can be compared element by element,
  and the feature issues anchor on elements that exist in both.

## Alternatives considered

- **A straight-through core without a user task**: credit check, contract, order, handover,
  activation. It is the smallest model, but without a deadline a customer who never signs waits
  forever. With no human task, neither the Tasklist nor a form appears.
- **The deadline core without the clarification**, keeping "Conclude contract" and its escalation.
  It is credible, but an escalation inside a sub-process is abstract for a business audience, and
  it still has no human task.
- **Keep everything.** It is complete, but too large to explain live, and it leaves nothing to add
  next in the demo.

## Consequences

- **Positive:** the process fits on one screen, and every element has a business meaning. Each
  removed capability is an issue anchored to the model, so designIQ shows it as a todo on the
  canvas. The original is one click away.
- **Negative / trade-offs:** the service does less than before.
  - A customer cannot withdraw during the withdrawal period the process waits for
    ([#3](https://github.com/miragon-blueprints/miragon-ai-example/issues/3)).
  - The API change is breaking: four endpoints and the `bikeModel` field are gone.
  - A rejection after the 14-day deadline does not revoke the contract that was sent. The core
    inherited this gap from the original. It is decided in
    [#8](https://github.com/miragon-blueprints/miragon-ai-example/issues/8).
  - Without the sub-process, `Compensate contract` hangs on `Send contract`. A withdrawal would
    therefore also revoke an unsigned contract. This is an open question in
    [#3](https://github.com/miragon-blueprints/miragon-ai-example/issues/3).
- **Neutral:** bringing a capability back means implementing its issue (model, code, tests). The
  process API and the OpenAPI contract are regenerated from the model and the controllers.
