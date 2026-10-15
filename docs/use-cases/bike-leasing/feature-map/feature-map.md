# BPM feature map: bike leasing

The feature map shows the whole business scope of the original bike-leasing process at t.BPM
granularity. The core that runs today is the backbone. Every planned feature sits where the
process needs it, so no feature loses its process context.

- Model: [bike-leasing-feature-map.bpmn](bike-leasing-feature-map.bpmn) (t.BPM workshop mode, not executable)
- The core alone: [bike-leasing-tbpm.bpmn](../tbpm/bike-leasing-tbpm.bpmn). It has the same steps, names and ids.
- What runs: [bike-leasing.bpmn](../process/bike-leasing.bpmn), the executable model
- Where the features come from: [scope-comparison.md](../original/scope-comparison.md) (capabilities C1 to C19)

## How to read the map

- **Backbone (middle rows).** This is the core that runs today. White steps run today; steps filled
  orange are planned.
- **Dashed boxes (groups).** Each box is one feature or core area, and its label names it.
  - Green, "Core · runs today: ...": what the executable process already does.
  - Orange, "#N <feature> · Stage N": a planned feature, with its GitHub issue number and stage.
- **Where the features attach.**
  - #1, above "Send contract": a reminder after 7 days. The wait for the signature continues. The
    timer on "Send contract" is workshop shorthand: an automated task ends at once, so the engine
    needs another shape. #1 names the two options, and a note sticky says so.
  - #2, above the order: the insurance policy is issued in parallel with the bike order (split
    and join).
  - #3, at the bottom: "Customer withdraws" is an event sub-process. It can start at any time while
    the case runs.
  - #4, inside #3: it runs only when the withdrawal hits a bike that was already ordered.
- **Stickies.**
  - Note: one per feature, with issue and stage. #5, #6 and #7 have no steps of their own, so their
    note sits on the step they serve. #6 sits on the pool corner, which means the whole process.
    One more note, under #1, marks the 7-day timer as shorthand.
  - Decision: an open make, buy or adapt decision (#8 to #11), on the step it affects.
  - Question: one open question inside #4.
- The designIQ validator reports 0 errors and 2 warnings: "13 stickies remain (1 open question)"
  and "15 activities". Both are expected for a feature map.

## Stages

| Stage | Features | Why in this order (derived from the issues) |
|---|---|---|
| Core (runs today) | apply and check credit; conclude the contract within 14 days; reject; order the bike and clarify an alternative; cancel if there is no alternative; hand over and activate | The smallest process that leases a bike end to end, including the one human task. |
| Stage 1 – Customer follow-up | [#1] | The smallest change: one timer and one message. It needs only the core and saves applications that would be rejected only because the contract email was overlooked. |
| Stage 2 – Insurance | [#2] | Every leased bike is insured by the handover. It needs only the core and adds no lead time, because it runs in parallel with the order. It comes before Stage 3 so that the withdrawal can reverse the policy from the start. |
| Stage 3 – Withdrawal | [#3], [#4] | The customer can withdraw while the case runs, including the 14-day withdrawal period after the handover. #4 needs #3: only a withdrawal undoes a placed order. With #2 in place, the reversal also cancels the policy. |
| Stage 4 – Portal and back-office views | [#5], [#6], [#7] | Views on data the process already produces; no new process steps. #5 builds on the core task "Clarify alternative with customer". #7 supplies the bike model names for #5 and #6. Settle #11 before #5. |

## Features

The issue anchors are the designIQ todo anchors in the executable model
([bike-leasing.bpmn](../process/bike-leasing.bpmn)). Some of them, such as `event_contractSigned`,
exist only there. On the map, the "yes" of "Signed within 14 days?" stands for "Contract signed".

| Feature | Stage | Issue | Steps on the map (element ids) | designIQ todo anchor in the executable model | Original capability ([scope comparison](../original/scope-comparison.md)) | Depends on |
|---|---|---|---|---|---|---|
| Core: apply and check credit | Core | — | `startEvent_leasingRequestReceived`, `businessRuleTask_checkCreditRating`, `gateway_isSolvent` | — | C1, C2 | — |
| Core: conclude the contract | Core | — | `serviceTask_sendContract`, `gateway_awaitSignature` | — | C4, C6 | — |
| Core: reject the application | Core | — | `gateway_rejectionJoin`, `serviceTask_sendRejection`, `endEvent_applicationRejected` | — | C3, C6 | — |
| Core: order the bike, clarify alternatives | Core | — | `gateway_bikeSourceJoin`, `serviceTask_orderBike`, `tbpm_gateway_bikeAvailable`, `userTask_clarifyAlternative`, `gateway_alternativeFound` | — | C8, C9, C18 | — |
| Core: cancel when no alternative is found | Core | — | `serviceTask_cancelContract`, `serviceTask_confirmContractCancellation`, `endEvent_contractCancelled` | — | C11 | — |
| Core: hand over and activate | Core | — | `event_handoverReported`, `event_withdrawalPeriodElapsed`, `endEvent_leasingActive` | — | C16, C17 | — |
| #1 Signature reminder | 1 | [#1] | `event_signatureReminder` (7 days), `serviceTask_sendReminderMail`, `endEvent_prospectReminded` | `serviceTask_sendContract`, `gateway_awaitSignature` | C5 | core |
| #2 Insurance policy | 2 | [#2] | `gateway_fork`, `serviceTask_issueInsurancePolicy`, `gateway_join` | `event_contractSigned`, `serviceTask_orderBike` | C7 | core; with #3, the reversal also cancels the policy |
| #3 Withdrawal | 3 | [#3] | `subProcess_applicationWithdrawn` (Customer withdraws), `startEvent_applicationWithdrawn`, `event_reverseApplication`, `serviceTask_sendCancellationConfirmation`, `endEvent_applicationCancelled` | `event_handoverReported`, `event_withdrawalPeriodElapsed`, `serviceTask_cancelContract` | C12 | core |
| #4 Bike order cancellation | 3 | [#4] | `fmap_gateway_bikeOrdered`, `callActivity_cancelBikeOrder` (Cancel bike order) with `startEvent_cancellationRequired`, `serviceTask_requestCancellation`, `gateway_cancellationPossible`, `userTask_clarifyReturn`, `serviceTask_bookCosts`, `cbo_gateway_join`, `endEvent_orderReversed`; `fmap_gateway_orderCancellationJoin` | `serviceTask_orderBike` | C13, C14, C15 | #3 |
| #5 Back-office inbox | 4 | [#5] | note on `userTask_clarifyAlternative` | `userTask_clarifyAlternative` | C10 (inbox) | the core task; #7 for bike model names; settle #11 first |
| #6 Application list | 4 | [#6] | note on the pool (whole process) | none (process-level todo) | C10 (list) | none; #7 adds bike models, #3 adds the status WITHDRAWN |
| #7 Bike catalogue | 4 | [#7] | note on `serviceTask_orderBike` | `serviceTask_orderBike`, `userTask_clarifyAlternative` | C10 (catalogue) | none |

Notes on the ids:

- All core ids and all feature ids are reused from the original or from the t.BPM model.
- `cbo_gateway_join` is the join of the original "Cancel Bike Order" process. It got the prefix
  `cbo_` because #2 already uses `gateway_join`.
- `fmap_gateway_bikeOrdered` ("Bike already ordered?") and `fmap_gateway_orderCancellationJoin`
  exist only on the map. They express the original rule that the reversal cancels an order only if
  one was actually placed. In the original, that rule was BPMN compensation.
- Not drawn: "Cancel policy" (original `serviceTask_cancelPolicy`). Whenever a reversal runs and #2
  exists, the policy is cancelled too. The original also ended the no-alternative path with a
  terminate end event, so that no insurance branch kept waiting. Both are in the scope of [#2].

## Open decisions on the map

| Decision | Issue | Sticky on | Anchored elements in the executable model |
|---|---|---|---|
| #8 Contract & e-signature: make, buy or adapt? | [#8] | Send contract | `serviceTask_sendContract`, `event_contractSigned`, `serviceTask_cancelContract` |
| #9 Customer notification channel? | [#9] | Send rejection | `serviceTask_sendContract`, `serviceTask_sendRejection`, `serviceTask_confirmContractCancellation` |
| #10 Dealer integration? | [#10] | Order bike from dealer | `serviceTask_orderBike`, `event_handoverReported` |
| #11 Back-office task UI? | [#11] | Clarify alternative with customer | `userTask_clarifyAlternative` |

The options, criteria and the Wardley map are in
[sourcing-decisions.md](../../../architecture/sourcing-decisions.md) and
[sourcing-map.owm](../../../architecture/sourcing-map.owm).

One open question is on the map as well, at "Cancellation possible?" in #4: which answer leads to
"Clarify return with dealer" and "Book cancellation costs"? In the original, the branch labels
contradict their conditions, so the two branches are deliberately unlabelled here (open question 2
in the [scope comparison](../original/scope-comparison.md)).

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
