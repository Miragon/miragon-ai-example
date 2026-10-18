# BPM feature map: bike leasing

The feature map shows the business scope of the bike-leasing process at t.BPM granularity: the core
that runs today, and every open feature issue where the process needs it, so no feature loses its
process context.

- Model: [bike-leasing-feature-map.bpmn](bike-leasing-feature-map.bpmn) (t.BPM workshop mode, not executable)
- The main path of the core alone: [bike-leasing-tbpm.bpmn](../tbpm/bike-leasing-tbpm.bpmn). It has
  the steps, names and ids of this map's main path. Against the executable model two ids differ, see
  the notes on the ids: "Application valid?" and "Contract signed". The withdrawal is not in it; it
  is on this map and in the executable model.
- What runs: [bike-leasing.bpmn](../process/bike-leasing.bpmn), the executable model
- Where the features come from: the issues, and the original model (tag [`original-process-v1`][tag])
- Picture, if designIQ is unavailable: [bike-leasing-feature-map.png](../../../assets/bike-leasing-feature-map.png)

## How to read the map

- **Backbone.** The white steps are the core that runs today. Steps filled orange are planned.
- **Dashed boxes (groups).** Each box is one core area or one feature, and its label names it.
  - Green, "Core · runs today: ...": what the executable process already does.
  - Orange, "#N <feature>": a planned feature that adds steps, with its GitHub issue number. The
    stage is on the feature's note sticky and in the tables below.
- **Where the features attach.**
  - #18, after "Send contract": the signature wait races a 14-day timer; the "14 days" path joins
    "Rejection reason".
  - #1, above "Send contract": a reminder after 7 days. The timer on "Send contract" is workshop
    shorthand (an automated task ends at once); in the executable model it sits on the receive task
    "Contract signed", and a note sticky says so.
  - #20, on the order branch: "Bike available?" → "Clarify alternative with customer" → re-order,
    or reverse and end with "Contract cancelled" (a terminate end, so the insurance branch does not
    keep waiting).
  - #21, between "Handover reported" and "Leasing active": the 14-day withdrawal period.
  - #3, at the bottom right: "Application withdrawn (automatic reversal)" replaces the core's manual
    event sub-process at the bottom left. The map draws both: the core as it runs, and the target.
  - #4, inside #3: it runs only when the withdrawal hits a bike that was already ordered.
- **Stickies.**
  - Note: one per feature, with issue and stage. #19 and #7 have no steps of their own, so their
    note sits on the step they change. One more note marks the 7-day timer of #1 as shorthand, and
    one records that #11 is decided.
  - Decision: an open make, buy or adapt decision (#8 to #10), on the step it affects.
  - Question: an open question of a feature, inside it (#18 and #4).
- The designIQ validator reports 0 errors and 2 warnings: "15 stickies remain (2 open questions)"
  and "17 activities". Both are expected for a feature map.

## Stages

| Stage | Features | Why in this order (derived from the issues) |
|---|---|---|
| Core (runs today) | apply, validate and check credit by hand; conclude the contract; reject; insure and order in parallel; hand over; withdraw and reverse by hand | The simplified event-subprocess model ([bike-leasing.bpmn](../process/bike-leasing.bpmn)), with a frontend and an inbox for both human tasks. |
| Stage 1 – Customer follow-up | [#18], [#1] | A sent contract no longer waits forever. #18 and #1 each need only the core: both are timers on the receive task "Contract signed". |
| Stage 2 – Automation and supply | [#19], [#20] | #19 needs only the core and takes the routine credit checks out of the inbox. #20 brings back the business outcome "out of stock"; its "no alternative" path reuses the compensation handlers of #3, so it lands after #3 or brings them itself. |
| Stage 3 – Withdrawal | [#21], [#3], [#4] | #21 needs only the core; a withdrawal during the period works with today's manual reversal. #3 makes the reversal automatic. #4 needs #3: only the automatic reversal compensates a placed order. |
| Stage 4 – Portal | [#7] | No new process steps: live availability in the catalogue the frontend already shows. It shares the dealer's availability check with #20; whichever lands first adds it. |

## Features

The anchors are the designIQ todo anchors in the executable model
([bike-leasing.bpmn](../process/bike-leasing.bpmn)), copied from the `element:` lines of each issue.

| Stage | Issue | Title | Steps on the map (element ids) | Anchored elements in the executable model | Depends on |
|---|---|---|---|---|---|
| Core | — | Apply, validate and check credit | `startEvent_leasingRequestReceived`, `serviceTask_validateApplication`, `tbpm_gateway_applicationValid`, `userTask_checkCreditRating`, `gateway_isSolvent` | — | — |
| Core | — | Conclude the contract | `serviceTask_sendContract`, `event_contractSigned` | — | — |
| Core | — | Reject the application | `gateway_rejectionJoin`, `serviceTask_sendRejection`, `endEvent_applicationRejected` | — | — |
| Core | — | Insure and order in parallel (delivered [#2]) | `gateway_fork`, `serviceTask_issueInsurancePolicy`, `serviceTask_orderBike`, `gateway_join` | — | — |
| Core | — | Hand over and activate | `event_handoverReported`, `endEvent_leasingActive` | — | — |
| Core | — | Withdraw, reverse by hand (inbox: delivered [#5], decided [#11]) | `subProcess_applicationWithdrawn`, `startEvent_applicationWithdrawn`, `userTask_reverseApplication`, `endEvent_applicationCancelled` | — | — |
| 1 | [#18] | Signature deadline (14 days) | `gateway_awaitSignature` (event-based), `event_signatureDeadline` (14 days), `flow_deadlineToRejection` | `receiveTask_contractSigned`, `gateway_rejectionJoin` | core; #8 for the open question |
| 1 | [#1] | Signature reminder (7 days) | `event_signatureReminder` (7 days), `serviceTask_sendReminderMail`, `endEvent_prospectReminded` | `receiveTask_contractSigned` | core |
| 2 | [#19] | Credit check by DMN | note on `userTask_checkCreditRating` (it becomes the business rule task `businessRuleTask_checkCreditRating`) | `userTask_checkCreditRating`, `gateway_isSolvent` | core |
| 2 | [#20] | Alternative bike when the dealer cannot deliver | `gateway_bikeSourceJoin`, `gateway_bikeAvailable`, `userTask_clarifyAlternative`, `gateway_alternativeFound`, `event_triggerReversal`, `serviceTask_confirmContractCancellation`, `endEvent_contractCancelled` (terminate) | `serviceTask_orderBike` | #3 (its compensation handlers); shares the availability check with #7 |
| 3 | [#21] | Withdrawal period before activation | `event_withdrawalPeriodElapsed` (14 days) | `event_handoverReported`, `endEvent_leasingActive` | core |
| 3 | [#3] | Automatic reversal by compensation | `fmap_subProcess_automaticReversal` with `fmap_startEvent_applicationWithdrawn`, `event_reverseApplication`, `serviceTask_sendCancellationConfirmation`, `fmap_endEvent_applicationCancelled` | `subProcess_applicationWithdrawn`, `userTask_reverseApplication`, `serviceTask_sendContract`, `serviceTask_issueInsurancePolicy` | core |
| 3 | [#4] | Bike order cancellation | `fmap_gateway_bikeOrdered`, `callActivity_cancelBikeOrder` (Cancel bike order) with `startEvent_cancellationRequired`, `serviceTask_requestCancellation`, `gateway_cancellationPossible`, `userTask_clarifyReturn`, `serviceTask_bookCosts`, `cbo_gateway_join`, `endEvent_orderReversed`; `fmap_gateway_orderCancellationJoin` | `serviceTask_orderBike` | #3 |
| 4 | [#7] | Live dealer availability | note on `serviceTask_orderBike` | `serviceTask_orderBike` | none; shares the availability check with #20 |

Notes on the ids:

- Core ids are the executable ids; `tbpm_gateway_applicationValid` comes from the t.BPM model and
  stands for the error boundary `event_applicationInvalid`.
- `event_contractSigned` on the map and in the t.BPM model is the business event "Contract signed".
  The executable model waits for the same message in the receive task `receiveTask_contractSigned`,
  because the timers of #18 and #1 attach to it as boundary events.
- Feature ids come from the issues and the original model. `gateway_bikeAvailable` is the gateway
  variant #20 names (option 2); #20 still chooses between it and the error boundary
  `event_bikeUnavailable`.
- Ids that exist only in the t.BPM model start with `tbpm_`, its flows `tbpm_flow_*`.
- Ids that exist only on the map start with `fmap_`. The #3 sub-process and its start and end
  events need them because the core already owns `subProcess_applicationWithdrawn`,
  `startEvent_applicationWithdrawn` and `endEvent_applicationCancelled`.
- `cbo_gateway_join` is the join of the "Cancel Bike Order" process in the original model. It got
  the prefix `cbo_` because the core already uses `gateway_join`.
- `fmap_gateway_bikeOrdered` ("Bike already ordered?") and `fmap_gateway_orderCancellationJoin`
  express the rule that the reversal cancels an order only if one was placed. In BPMN that rule is
  compensation.
- Not drawn: the compensation handlers "Cancel contract" (`serviceTask_cancelContract`) and "Cancel
  policy" (`serviceTask_cancelPolicy`) of #3. "Reverse completed steps" stands for them, and #20
  reuses them.

## Open decisions on the map

| Decision | Issue | Sticky on | Anchored elements in the executable model |
|---|---|---|---|
| Contract & e-signature: make, buy or adapt? | [#8] | Send contract | `serviceTask_sendContract`, `receiveTask_contractSigned` |
| Customer notification channel? | [#9] | Send rejection | `serviceTask_sendContract`, `serviceTask_sendRejection` |
| Dealer integration? | [#10] | Handover reported | `serviceTask_orderBike`, `event_handoverReported` |

[#11] (back-office task UI) is decided: the back office works in an own inbox in the frontend
([ADR-0010](../../../adr/0010-back-office-task-ui-own-inbox-in-the-frontend.md)). A note sticky
above "Leasing request received" and "Validate application" records it.

The options, criteria and the Wardley map are in
[sourcing-decisions.md](../../../architecture/sourcing-decisions.md) and
[sourcing-map.owm](../../../architecture/sourcing-map.owm).

Two open questions are on the map as well:

- In #18: does the rejection after the deadline revoke the issued contract? Decided with #8.
- In #4, at "Cancellation possible?": which answer leads to "Clarify return with dealer" and "Book
  cancellation costs"? In the original model, the branch labels contradict their conditions, so the
  two branches are deliberately unlabelled here.

[tag]: https://github.com/miragon-blueprints/miragon-ai-example/tree/original-process-v1/kotlin-gradle/service/app/src/main/resources
[#1]: https://github.com/miragon-blueprints/miragon-ai-example/issues/1
[#2]: https://github.com/miragon-blueprints/miragon-ai-example/issues/2
[#3]: https://github.com/miragon-blueprints/miragon-ai-example/issues/3
[#4]: https://github.com/miragon-blueprints/miragon-ai-example/issues/4
[#5]: https://github.com/miragon-blueprints/miragon-ai-example/issues/5
[#7]: https://github.com/miragon-blueprints/miragon-ai-example/issues/7
[#8]: https://github.com/miragon-blueprints/miragon-ai-example/issues/8
[#9]: https://github.com/miragon-blueprints/miragon-ai-example/issues/9
[#10]: https://github.com/miragon-blueprints/miragon-ai-example/issues/10
[#11]: https://github.com/miragon-blueprints/miragon-ai-example/issues/11
[#18]: https://github.com/miragon-blueprints/miragon-ai-example/issues/18
[#19]: https://github.com/miragon-blueprints/miragon-ai-example/issues/19
[#20]: https://github.com/miragon-blueprints/miragon-ai-example/issues/20
[#21]: https://github.com/miragon-blueprints/miragon-ai-example/issues/21
