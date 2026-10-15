# Bike leasing: the use case

MiraVelo is a (fictional) bike brand that sells its bikes on a leasing model. This use case follows
one leasing application from the customer's request to an active lease. Start here for the business
view, then go on with "Where to look next".

## The problem

A lease involves the customer, MiraVelo's back office, a contract system and a bike dealer. It can
take weeks: the customer has up to 14 days to sign, and after the handover there is a 14-day
withdrawal period. At every step someone waits for someone else. If no single process owns the case,
these things go wrong (each one is answered by a rule in today's model):

- A contract goes to someone who cannot afford the lease. Answer: the credit check comes first.
- A contract is sent, never signed, and the case stays open forever. Answer: a 14-day signature deadline.
- The bike is out of stock and the order silently stalls. Answer: the back office clarifies an
  alternative with the customer, or the contract is cancelled.
- The dealer's system is down and nobody notices. Answer: the order is retried, then an incident is raised.
- The lease goes live before the withdrawal period is over. Answer: activation waits 14 days after handover.
- Nobody can say where a case stands. Answer: every case has a status and a visible process instance.

These points come from the rules in the model. They are not measured incidents.

## Actors

| Actor | Role in the core |
|---|---|
| Customer | submits the leasing request, signs the contract, agrees on an alternative bike |
| Back office | clarifies an alternative bike with the customer (Tasklist form or REST) |
| Dealer | confirms availability, takes the bike order, reports the handover |
| Operations | watches running cases and resolves incidents (Cockpit) |

The process uses three external systems. In this repo all three are simulated and only log: the
contract system, the notification channel and the dealer system. The service is headless, with no
portal yet. In the demo, Bruno or the Swagger UI make the customer's and the dealer's REST calls, and
the back office works in the CIB seven Tasklist.

## Trigger, goal and outcomes

- Trigger: the customer submits a leasing request with name, email, age, monthly net income and the
  bike id (`POST /api/bike-leasing`). The process starts at "Leasing request received".
- Goal: a solvent customer has signed the contract and has the bike, and the withdrawal period is
  over. The lease is active.

| Status | End event | When |
|---|---|---|
| ACTIVE | Leasing active | contract signed, bike handed over, withdrawal period over |
| REJECTED | Application rejected | not solvent, or no signature within 14 days |
| CANCELLED | Contract cancelled | the bike is unavailable and no acceptable alternative was found |

While the case runs, its status moves RECEIVED → ORDERED → HANDED_OVER.

## The core flow

1. Leasing request received: the case starts (RECEIVED).
2. Check credit rating: a decision table. Under 18, over 99, or a monthly net income below 1500
   means not solvent.
3. Send contract: the contract system issues the contract and the customer is asked to sign.
4. Contract signed: the process waits up to 14 days for the signature.
5. Order bike from dealer: the dealer confirms the bike is available and takes the order (ORDERED).
6. Handover reported: the dealer reports that the customer has the bike (HANDED_OVER).
7. Await end of withdrawal period: 14 days.
8. Leasing active: the lease is live (ACTIVE).

Variants:

- Not solvent (step 2): "Rejection reason" → "Send rejection" → "Application rejected" (REJECTED).
- No signature within 14 days (step 4): the same rejection. The contract that was sent is not
  revoked (open decision [#8]).
- Bike unavailable (step 5): the back office runs "Clarify alternative with customer".
  - Alternative found: the alternative bike is ordered (back to step 5).
  - No alternative: "Trigger reversal" cancels the contract → "Send cancellation confirmation" →
    "Contract cancelled" (CANCELLED).
- Dealer outage (step 5): the order is retried 3 times, 10 seconds apart, then an incident appears
  in the Cockpit. Operations fixes the cause and retries. The case waits; it does not end.

Demo bikes: `BIKE-OOS` is out of stock and `BIKE-FAIL` simulates a dealer outage. Every other bike
id is available.

## Deliberately not in the core

The original model did more (see the scope comparison). Each removed capability is a feature issue,
grouped into stages on the feature map. Every issue is also a designIQ todo, shown on the process
elements it touches.

| Stage | Issue | Feature |
|---|---|---|
| Stage 1 – Customer follow-up | [#1] | Remind the customer to sign the contract after 7 days |
| Stage 2 – Insurance | [#2] | Issue the insurance policy alongside the bike order |
| Stage 3 – Withdrawal | [#3] | Let the customer withdraw the application and reverse completed steps |
| Stage 3 – Withdrawal | [#4] | Cancel a placed bike order at the dealer (return clarification and cancellation costs); needs #3 |
| Stage 4 – Portal and back-office views | [#5] | Give the back office an inbox of pending alternative clarifications |
| Stage 4 – Portal and back-office views | [#6] | List leasing applications for the customer portal |
| Stage 4 – Portal and back-office views | [#7] | Offer a bike catalogue with models and live dealer availability |

When both #2 and #3 exist, the reversal also cancels the insurance policy.

## Open decisions

Make, buy or adapt? Each decision is an issue. The sourcing map and the sourcing decisions file
explain the options.

| Issue | Decision | Today |
|---|---|---|
| [#8] | Contract issuing and e-signature: make, buy or adapt? | simulated contract system |
| [#9] | Channel for customer notifications | simulated, messages are only logged |
| [#10] | How to integrate the bike dealer | simulated dealer (BIKE-OOS, BIKE-FAIL) |
| [#11] | Back-office task UI: adapt the CIB seven Tasklist or build an own inbox | both work today: Tasklist form and REST endpoint |

## Where to look next

In the order of the demo: context, sourcing (make, buy or adapt), process and features, architecture,
the executable process, then the issues. The method is iterative: a buy decision can change the
context, so going back a step is part of it.

| What | Where |
|---|---|
| Index of all artifacts and the demo order | [docs/README.md](../../README.md) |
| Shared vocabulary | [glossary.md](../../glossary.md) |
| Wardley map and sourcing decisions | [sourcing-map.owm](../../architecture/sourcing-map.owm), [sourcing-decisions.md](../../architecture/sourcing-decisions.md) |
| t.BPM table-top model of the core | [bike-leasing-tbpm.bpmn](tbpm/bike-leasing-tbpm.bpmn) |
| BPM feature map (process plus feature stages) | [bike-leasing-feature-map.bpmn](feature-map/bike-leasing-feature-map.bpmn), [feature-map.md](feature-map/feature-map.md) |
| C4 system context and containers | [c4-system-context.md](../../architecture/c4-system-context.md), [c4-container.md](../../architecture/c4-container.md) |
| Executable process and credit decision | [bike-leasing.bpmn](process/bike-leasing.bpmn), [checkCreditRating.dmn](process/checkCreditRating.dmn) |
| How it is built and deployed, element list | [executable-process.md](process/executable-process.md) |
| Original model and what changed | [original-bike-leasing.bpmn](original/original-bike-leasing.bpmn), [original-cancel-bike-order.bpmn](original/original-cancel-bike-order.bpmn), [scope-comparison.md](original/scope-comparison.md) |
| Architecture decisions (ADRs); why this core: ADR-0015 | [adr/](../../adr/), [0015](../../adr/0015-reduce-the-executable-process-to-a-core.md) |
| Live demo script | [demo-script.md](../../demo/demo-script.md) |
| PNG renders, if designIQ is unavailable | [assets/](../../assets/) |

[#1]: https://github.com/miragon-blueprints/miragon-ai-example/issues/1
[#2]: https://github.com/miragon-blueprints/miragon-ai-example/issues/2
[#3]: https://github.com/miragon-blueprints/miragon-ai-example/issues/3
[#4]: https://github.com/miragon-blueprints/miragon-ai-example/issues/4
[#5]: https://github.com/miragon-blueprints/miragon-ai-example/issues/5
[#6]: https://github.com/miragon-blueprints/miragon-ai-example/issues/6
[#7]: https://github.com/miragon-blueprints/miragon-ai-example/issues/7
[#8]: https://github.com/miragon-blueprints/miragon-ai-example/issues/8
[#9]: https://github.com/miragon-blueprints/miragon-ai-example/issues/9
[#10]: https://github.com/miragon-blueprints/miragon-ai-example/issues/10
[#11]: https://github.com/miragon-blueprints/miragon-ai-example/issues/11
