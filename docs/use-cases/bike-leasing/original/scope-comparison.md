# Scope comparison: original vs. core

The executable process is a core: the simplified event-subprocess model
(see [executable-process.md](../process/executable-process.md)). This file records what the
original model could do, what the core keeps, and where each missing capability went. The
capability ids C1 to C19 are referenced from the GitHub issues, so keep them stable.

The original is the target picture for the roadmap. Every capability the core lacks or has in a
smaller form points to a GitHub issue in the table below. A feature issue's body says what the
original did, and its anchor block pins it to the core element it would extend (or to the process as
a whole). The feature map orders the issues into stages.

This file is maintained. Only the copies of the original models and of the decision in this folder
are frozen.

## Reference: the original

- Tag `original-process-v1` → commit `40da36ef7cc20423a7312ca29d5a1c493043ca9c` (main on 2026-10-13):
  https://github.com/miragon-blueprints/miragon-ai-example/tree/original-process-v1. The tag is never
  moved; a later baseline gets a new tag.
- Permalinks at that commit. The kotlin-gradle copies are linked; the java-maven copies were
  byte-identical.
  - `bike-leasing.bpmn`: process `bikeLeasingProcess`, blob `6c221a5`
    https://github.com/miragon-blueprints/miragon-ai-example/blob/40da36ef7cc20423a7312ca29d5a1c493043ca9c/kotlin-gradle/service/app/src/main/resources/bpmn/bike-leasing.bpmn
  - `cancel-bike-order.bpmn`: process `cancelBikeOrder` (called to cancel a placed order), blob `42b4f5a`
    https://github.com/miragon-blueprints/miragon-ai-example/blob/40da36ef7cc20423a7312ca29d5a1c493043ca9c/kotlin-gradle/service/app/src/main/resources/bpmn/cancel-bike-order.bpmn
  - `check-credit-rating.dmn`: decision `checkCreditRating`, blob `f32fd4a`
    https://github.com/miragon-blueprints/miragon-ai-example/blob/40da36ef7cc20423a7312ca29d5a1c493043ca9c/kotlin-gradle/service/app/src/main/resources/dmn/check-credit-rating.dmn
  - `clarify-alternative.form`: form `clarifyAlternativeForm`, blob `564c6a3`. No longer deployed.
    https://github.com/miragon-blueprints/miragon-ai-example/blob/40da36ef7cc20423a7312ca29d5a1c493043ca9c/kotlin-gradle/service/app/src/main/resources/forms/clarify-alternative.form
  - `clarify-return.form`: form `clarifyReturnForm`, blob `f25e015`. No longer deployed.
    https://github.com/miragon-blueprints/miragon-ai-example/blob/40da36ef7cc20423a7312ca29d5a1c493043ca9c/kotlin-gradle/service/app/src/main/resources/forms/clarify-return.form
- Frozen copies in this folder are byte-identical to the blobs above. They are not deployed,
  generated or linted:
  - [original-bike-leasing.bpmn](original-bike-leasing.bpmn) (blob `6c221a5`)
  - [original-cancel-bike-order.bpmn](original-cancel-bike-order.bpmn) (blob `42b4f5a`)
  - [checkCreditRating.dmn](checkCreditRating.dmn) (blob `f32fd4a`). It was deployed from
    `process/` until the rebase and moved here unchanged. Its file stem equals the decision id, so
    designIQ still links it; [#19] moves it back to `process/` (a move, never a copy, so the stem
    stays unique under `docs/`).
  - Check them with `git hash-object docs/use-cases/bike-leasing/original/*.bpmn docs/use-cases/bike-leasing/original/*.dmn`.
- Rendered original: [bike-leasing-original.png](../../../assets/bike-leasing-original.png)
  (at the commit: https://github.com/miragon-blueprints/miragon-ai-example/blob/40da36ef7cc20423a7312ca29d5a1c493043ca9c/docs/assets/bike-leasing.png)

At a glance:

- Original: 43 flow nodes and 33 sequence flows, plus the called process `cancelBikeOrder` (7 flow
  nodes) and the DMN `checkCreditRating`.
- Core: 20 flow nodes and 18 sequence flows, no called process, no DMN.
- 16 of the 20 core elements are original elements with their original ids. Two of them got a new
  name: `startEvent_applicationWithdrawn` ("Request withdrawn" → "Application withdrawn") and
  `endEvent_applicationCancelled` ("Leasing cancelled" → "Application cancelled").
- New in the core: `serviceTask_validateApplication` with the error boundary
  `event_applicationInvalid`, `userTask_checkCreditRating` (the original had the business rule task
  `businessRuleTask_checkCreditRating`) and `userTask_reverseApplication` (the original had the
  compensation throw `event_reverseApplication`).

## Capabilities

C1 to C19 are the capabilities of the original, derived from its models, code and tests.
"Simplified" means the business behaviour is still there in a smaller form.

| Capability | Original BPMN elements | In the core? | Where now (core ids) or issue |
|---|---|---|---|
| **C1** Leasing request intake | startEvent_leasingRequestReceived | kept | startEvent_leasingRequestReceived; start variables applicationId, bikeId, bikeModel (age and income stay in the database); new: serviceTask_validateApplication and event_applicationInvalid |
| **C2** Credit rating check via DMN | businessRuleTask_checkCreditRating, gateway_isSolvent | simplified | userTask_checkCreditRating (the back office decides in the inbox), gateway_isSolvent; the DMN is frozen here → [#19] |
| **C3** Rejection of non-solvent applicants | gateway_rejectionJoin, serviceTask_sendRejection, endEvent_applicationRejected | kept | same ids; also reached by an invalid application (flow_applicationInvalid) |
| **C4** Contract conclusion: issue, send and sign the leasing contract | subProcess_concludeContract with startEvent_customerEligible, serviceTask_sendContract, gateway_awaitSignature, event_contractSigned, endEvent_contractValid | simplified | serviceTask_sendContract → event_contractSigned (no sub-process, no event-based gateway); sourcing → [#8] |
| **C5** Signature reminder after 7 days | event_signatureReminder, serviceTask_sendReminderMail, endEvent_prospectReminded | removed | [#1] (needs [#18]) |
| **C6** Signature deadline (14 days) escalation to rejection | event_signatureDeadline, endEvent_notSigned, event_contractNotSigned (escalation contractNotSigned) | removed | [#18]; the core waits for the signature without a time limit |
| **C7** Insurance policy issuance (parallel to bike order) | gateway_fork, serviceTask_issueInsurancePolicy, gateway_join, event_compensateInsurance, serviceTask_cancelPolicy | simplified | gateway_fork, serviceTask_issueInsurancePolicy, gateway_join (delivered [#2]); cancelling the policy by compensation → [#3] |
| **C8** Bike order at the dealer | gateway_fork, gateway_bikeSourceJoin, serviceTask_orderBike, gateway_join | simplified | serviceTask_orderBike between gateway_fork and gateway_join; no availability check and no re-order loop (gateway_bikeSourceJoin → [#20]), no audit listener; sourcing → [#10] |
| **C9** Bike unavailable: clarify an alternative with the customer and re-order | event_bikeUnavailable, userTask_clarifyAlternative, gateway_alternativeFound, gateway_bikeSourceJoin | removed | [#20]; live availability in the catalogue → [#7] |
| **C10** Back-office inbox, application list and bike catalogue | userTask_clarifyAlternative (read models only) | simplified | inbox `GET /api/tasks` for both user tasks (delivered [#5], decided [#11]); list `GET /api/bike-leasing` (delivered [#6]); catalogue `GET /api/bikes` without availability, not persisted → [#7] |
| **C11** No alternative found: reverse contract and policy, confirm cancellation, terminate | event_triggerReversal, event_compensateContract, serviceTask_cancelContract, event_compensateInsurance, serviceTask_cancelPolicy, serviceTask_confirmContractCancellation, endEvent_contractCancelled (terminate) | removed | [#20], reusing the compensation handlers of [#3] |
| **C12** Customer withdrawal with SAGA compensation | subProcess_applicationWithdrawn with startEvent_applicationWithdrawn, event_reverseApplication, serviceTask_sendCancellationConfirmation, endEvent_applicationCancelled; Message_ApplicationWithdrawn | simplified | subProcess_applicationWithdrawn, startEvent_applicationWithdrawn, userTask_reverseApplication (the back office reverses by hand), endEvent_applicationCancelled; status WITHDRAWN → CANCELLED; compensation and confirmation → [#3] |
| **C13** Bike order cancellation at the dealer (called process cancelBikeOrder) | event_compensateOrder, callActivity_cancelBikeOrder; in cancelBikeOrder: startEvent_cancellationRequired, serviceTask_requestCancellation, gateway_cancellationPossible, endEvent_orderReversed | removed | [#4]; open question 2 |
| **C14** Return clarification with the dealer (form-only human task) | in cancelBikeOrder: userTask_clarifyReturn (clarifyReturnForm) | removed | [#4], as an inbox task (ADR-0010), not a form |
| **C15** Cancellation cost booking | in cancelBikeOrder: serviceTask_bookCosts | removed | [#4] |
| **C16** Handover reporting | gateway_join, event_handoverReported | kept | gateway_join, event_handoverReported; sourcing → [#10] |
| **C17** Withdrawal period (14 days) and leasing activation | event_withdrawalPeriodElapsed, endEvent_leasingActive | simplified | endEvent_leasingActive (a none end event); the handover sets ACTIVE at once, no withdrawal period → [#21] |
| **C18** Incident demo (dealer outage, retries, incident) | serviceTask_orderBike (asyncBefore, retry cycle R3/PT10S) | kept | serviceTask_orderBike (delegate, asyncBefore, R3/PT10S; the reference has an external task there); covered by `DealerOutageIncidentTest` |
| **C19** Customer notifications (cross-cutting) | serviceTask_sendContract, serviceTask_sendReminderMail, serviceTask_sendRejection, serviceTask_sendCancellationConfirmation, serviceTask_confirmContractCancellation | simplified | serviceTask_sendContract, serviceTask_sendRejection; reminder → [#1], cancellation confirmation → [#3] and [#20]; channel → [#9] |

Totals: 4 kept (C1, C3, C16, C18), 8 simplified (C2, C4, C7, C8, C10, C12, C17, C19), 7 removed
(C5, C6, C9, C11, C13, C14, C15).

## Technical simplifications (not business features)

| Change | Original | Core |
|---|---|---|
| Java/Maven variant removed | two equivalent services, `kotlin-gradle/` and `java-maven/`, kept identical by a CI parity check (Blueprint Checks) | one Kotlin service |
| Gradle build at the repo root | `kotlin-gradle/` holds the build with `service:app` and `service:common-architecture-tests` | the same two modules, without the `kotlin-gradle/` level |
| Executable model from a source model | the repo's own model | the simplified event-subprocess model, with the deviations listed in [executable-process.md](../process/executable-process.md) |
| No sub-process, no escalation, no compensation | "Conclude contract" sub-process; escalation `contractNotSigned`; compensation boundaries on contract, policy and order | flat main flow; the reversal is a user task |
| Terminate end gone | endEvent_contractCancelled terminated the instance, killing the insurance token waiting at gateway_join | no such path; an interrupting event sub-process ends the instance on withdrawal |
| Execution listener removed | BikeOrderAuditListener on serviceTask_orderBike (end event, technical audit log) | removed |
| User tasks without forms | Camunda Forms `clarifyAlternativeForm`, `clarifyReturnForm`, completed in the Tasklist | no forms; the back office completes tasks through the domain API in the frontend inbox ([ADR-0010](../../../adr/0010-back-office-task-ui-own-inbox-in-the-frontend.md)) |
| CORS bean removed | `DevCorsConfiguration`, a dev-profile CORS bean for a browser frontend on another origin | removed; the React frontend in `frontend/` ([ADR-0009](../../../adr/0009-react-frontend-next-to-the-service.md)) reaches the API through the Vite proxy, so the service needs no CORS |
| Bike portfolio table dropped | table `bike_portfolio` created by `V1__init.sql` and seeded at start-up | `V2__drop_bike_portfolio.sql` drops it; the catalogue is a fixed list in the simulated dealer; `V3__bike_model_and_policy.sql` adds the bike model and the policy id to the application |
| Models executed from `docs/` | `src/main/resources/bpmn/` and `dmn/` | `docs/use-cases/bike-leasing/process/`, packaged by Gradle |
| DMN not deployed | `check-credit-rating.dmn`, deployed | `checkCreditRating.dmn` (stem = decision id), frozen in this folder |
| Definitions removed | Message_LeasingActivated, Error_BikeUnavailable, Escalation_ContractNotSigned; the message event definition id `Msg_AntragZurueckgezogen_d` (the only German id) | gone; new: Error_ApplicationInvalid (`applicationInvalid`) |

## Open questions inherited from the original

1. Should a rejection after the signature deadline revoke the issued contract (C6)? "Send contract"
   issues the contract before the signature wait. In the original, the escalation interrupted
   "Conclude contract" before its compensation was ever registered, so the contract stayed issued.
   The core has no deadline at all. Carried by [#18], decided together with [#8].
2. cancelBikeOrder: the gateway labels contradict the conditions (C13, C14). `flow_cancellationNotPossible`
   is labelled "No" but fires on `${cancellationPossible}`. The default flow `flow_cancellationPossible`,
   labelled "Yes", fires when cancellation is not possible. The form text matches the condition; the
   labels match the opposite reading. The simulated dealer always answered "possible", so only the
   return-and-costs branch ever ran.  Decide which reading is right before the process comes back. Carried by [#4].
3. Should a withdrawal before the signature revoke the sent contract (C12)? In the original, the
   contract became compensable only when "Conclude contract" completed, that is after the
   signature. Today the back office decides case by case in "Reverse application". Once the
   reversal is automatic, the position of the compensation boundary decides it: on "Send contract",
   a sent but unsigned contract is revoked; on a "Conclude contract" scope, it is not. Carried by
   [#3], decided together with [#8] and the shape [#18] gives the signature wait.

In designIQ, the frozen original shows 9 validator errors and 2 warnings: false positives from its
compensation handlers, 14 activities, and the call to `cancelBikeOrder`, whose file stem differs.
This is expected; the copy is reference only. The frozen `checkCreditRating` validates clean.

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
