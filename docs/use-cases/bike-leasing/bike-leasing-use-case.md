# Bike leasing: the use case

MiraVelo is a (fictional) bike brand that sells its bikes on a leasing model. This use case follows
one leasing application from the customer's request to an active lease. Start here for the business
view, then go on with "Where to look next".

## The problem

A lease involves the customer, MiraVelo's back office, a contract system, an insurer and a bike
dealer. At every step someone waits for someone else. If no single process owns the case, these
things go wrong (each one is answered by a rule in today's model):

- An application with wrong data reaches the contract. Answer: the application is validated first.
- A contract goes to someone who cannot afford the lease. Answer: the back office checks the credit
  rating before any contract is sent.
- A bike is handed over without insurance. Answer: the policy is issued in parallel with the order,
  and the handover is accepted only when both are done.
- The dealer's system is down and nobody notices. Answer: the order is retried, then an incident is
  raised.
- The customer changes their mind and the case runs on. Answer: the customer can withdraw at any
  time until the handover, and the back office reverses the case.
- Nobody can say where a case stands. Answer: every case has a status, a visible process instance
  and, for the back office, an entry in the inbox when someone has to act.

These points come from the rules in the model. They are not measured incidents.

## Actors

| Actor | Role in the core |
|---|---|
| Customer | submits the leasing request, signs the contract, may withdraw the application |
| Back office | checks the credit rating and reverses withdrawn applications, both in the inbox of the frontend (`/aufgaben`) |
| Dealer | takes the bike order, reports the handover |
| Operations | watches running cases and resolves incidents (CIB seven Cockpit) |

The process uses four external systems. In this repo all four are simulated and only log: the
contract system, the insurer, the notification channel and the dealer system.

The React frontend (`frontend/`, [ADR-0009](../../adr/0009-react-frontend-next-to-the-service.md))
shows both sides of the case: the application list (`/`), the request form (`/antraege/neu`), the
application detail (`/antraege/{id}`) and the back-office inbox (`/aufgaben`). In the demo, the
detail page also plays the customer's and the dealer's part with its "sign contract", "report
handover" and "withdraw" buttons. Bruno and the Swagger UI make the same REST calls. The back office
works in the inbox, not in the CIB seven Tasklist: completing a task there would bypass the domain
([ADR-0010](../../adr/0010-back-office-task-ui-own-inbox-in-the-frontend.md)).

## Trigger, goal and outcomes

- Trigger: the customer submits a leasing request with name, email, age, monthly net income and the
  bike id (`POST /api/bike-leasing`). The process starts at "Leasing request received".
- Goal: a valid, solvent customer has signed the contract, the bike is insured and handed over. The
  lease is active.

| Status | End event | When |
|---|---|---|
| ACTIVE | Leasing active | contract signed, policy issued, bike ordered and handed over |
| REJECTED | Application rejected | the application is invalid, or the customer is not solvent |
| CANCELLED | Application cancelled | the customer withdrew and the back office reversed the application |

While the case runs, its status moves RECEIVED → CONTRACT_SENT → CONTRACT_SIGNED → ORDERED → ACTIVE.
A withdrawal sets WITHDRAWN at once; the reversal ends it as CANCELLED.

## The core flow

1. Leasing request received: the case starts (RECEIVED).
2. Validate application: the applicant must be at least 18 and the bike must be in the dealer's
   catalogue. **Assumption:** the source model, a variant Miragon maintains outside this repo,
   names the step but not its rule; this rule is ours.
3. Check credit rating: the back office decides "solvent" or "not solvent" in the inbox. No rule
   guides the decision yet ([#19] automates it).
4. Send contract: the contract system issues the contract and the customer is asked to sign
   (CONTRACT_SENT).
5. Contract signed: the process waits for the signature, without a time limit (CONTRACT_SIGNED).
6. In parallel: Issue insurance policy, and Order bike from dealer (ORDERED once the dealer took the
   order).
7. Handover reported: the dealer reports that the customer has the bike. It is accepted only after
   both parallel steps are done.
8. Leasing active: the lease is live (ACTIVE).

Variants:

- Application invalid (step 2): "Rejection reason" → "Send rejection" → "Application rejected"
  (REJECTED).
- Not solvent (step 3): the same rejection.
- Withdrawal (any time from RECEIVED to ORDERED): the event sub-process "Application withdrawn"
  stops whatever runs (status WITHDRAWN). The back office does "Reverse application" in the inbox:
  it undoes contract, policy and order outside the system and confirms. The case ends in
  "Application cancelled" (CANCELLED). After the handover, or once the case has ended, a withdrawal
  is refused (409).
- Dealer outage (step 6): the order is retried 3 times, 10 seconds apart, then an incident appears
  in the Cockpit. Operations fixes the cause and retries. The case waits; it does not end.

Demo bikes: `BIKE-900`, `BIKE-800` and `BIKE-600` can be ordered; `BIKE-FAIL` simulates a dealer
outage. Any other bike id makes the application invalid.

An action the current state does not allow (signing twice, reporting the handover too early,
completing a task that is not open) is refused with 409; an unknown application id answers 404.

## Deliberately not in the core

The original model (tag [`original-process-v1`][tag]) did more. Each capability the core lacks is a
feature issue, grouped into stages on the feature map. Every issue is also a designIQ todo, shown on
the process elements it touches.

| Stage | Issue | Feature |
|---|---|---|
| Stage 1 – Customer follow-up | [#18] | Reject the application when the contract stays unsigned for 14 days |
| Stage 1 – Customer follow-up | [#1] | Remind the customer to sign after 7 days |
| Stage 2 – Automation and supply | [#19] | Automate the credit check with the DMN `checkCreditRating` |
| Stage 2 – Automation and supply | [#20] | Clarify an alternative bike with the customer when the dealer cannot deliver |
| Stage 3 – Withdrawal | [#21] | Activate the lease only after a 14-day withdrawal period |
| Stage 3 – Withdrawal | [#3] | Reverse a withdrawn application automatically (compensation) instead of by hand |
| Stage 3 – Withdrawal | [#4] | Cancel a placed bike order at the dealer (return clarification and cancellation costs); needs #3 |
| Stage 4 – Portal | [#7] | Show live dealer availability in the bike catalogue |

The rebase onto the new core delivered [#2] (insurance policy in parallel), [#5] (back-office inbox),
[#6] (application list) and decided [#11] (own inbox, ADR-0010). These issues close with it.

## Open decisions

Make, buy or adapt? Each decision is an issue. The sourcing map and the sourcing decisions file
explain the options.

| Issue | Decision | Today |
|---|---|---|
| [#8] | Contract issuing and e-signature: make, buy or adapt? | simulated contract system; the "sign contract" button stands in for the signature |
| [#9] | Channel for customer notifications | simulated, messages are only logged |
| [#10] | How to integrate the bike dealer | simulated dealer with a fixed catalogue (`BIKE-FAIL` for the outage) |
| [#11] | Back-office task UI | decided: own inbox in the frontend ([ADR-0010](../../adr/0010-back-office-task-ui-own-inbox-in-the-frontend.md)) |

## Where to look next

In the order of the demo: context, sourcing (make, buy or adapt), process and features, architecture,
the executable process, then the issues. The method is iterative: a buy decision can change the
context, so going back a step is part of it.

| What | Where |
|---|---|
| Index of all artifacts and the demo order | [docs/README.md](../../README.md) |
| Shared vocabulary | [glossary.md](../../glossary.md) |
| Wardley map and sourcing decisions | [sourcing-map.owm](../../architecture/sourcing-map.owm), [sourcing-decisions.md](../../architecture/sourcing-decisions.md) |
| t.BPM table-top model of the main path | [bike-leasing-tbpm.bpmn](tbpm/bike-leasing-tbpm.bpmn) |
| BPM feature map (process plus feature stages) | [bike-leasing-feature-map.bpmn](feature-map/bike-leasing-feature-map.bpmn), [feature-map.md](feature-map/feature-map.md) |
| C4 system context and containers | [c4-system-context.md](../../architecture/c4-system-context.md), [c4-container.md](../../architecture/c4-container.md) |
| Executable process, and how it reaches the code | [bike-leasing.bpmn](process/bike-leasing.bpmn), [ADR-0011](../../adr/0011-embedded-cib-seven-with-delegates-and-a-generated-process-api.md) |
| Architecture decisions (ADRs) | [adr/](../../adr/) |
| PNG renders, if designIQ is unavailable | [assets/](../../assets/) |

[tag]: https://github.com/miragon-blueprints/miragon-ai-example/tree/original-process-v1/kotlin-gradle/service/app/src/main/resources
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
[#18]: https://github.com/miragon-blueprints/miragon-ai-example/issues/18
[#19]: https://github.com/miragon-blueprints/miragon-ai-example/issues/19
[#20]: https://github.com/miragon-blueprints/miragon-ai-example/issues/20
[#21]: https://github.com/miragon-blueprints/miragon-ai-example/issues/21
