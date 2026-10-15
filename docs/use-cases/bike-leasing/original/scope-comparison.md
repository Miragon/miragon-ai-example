# Scope comparison: original vs. core

The executable process was reduced to a core. This file records what the original model could do,
what the core keeps, and where each removed capability went. The capability ids C1 to C19 are
referenced from the GitHub issues, so keep them stable. Why the core was cut this way:
[ADR-0015](../../../adr/0015-reduce-the-executable-process-to-a-core.md).

## Reference: the original

- Tag `original-process-v1` → commit `40da36ef7cc20423a7312ca29d5a1c493043ca9c` (main on 2026-10-13,
  before the core reduction): https://github.com/miragon-blueprints/miragon-ai-example/tree/original-process-v1
- Permalinks at that commit. The kotlin-gradle copies are linked; the java-maven copies were
  byte-identical.
  - `bike-leasing.bpmn`: process `bikeLeasingProcess`, blob `6c221a5`
    https://github.com/miragon-blueprints/miragon-ai-example/blob/40da36ef7cc20423a7312ca29d5a1c493043ca9c/kotlin-gradle/service/app/src/main/resources/bpmn/bike-leasing.bpmn
  - `cancel-bike-order.bpmn`: process `cancelBikeOrder` (called to cancel a placed order), blob `42b4f5a`
    https://github.com/miragon-blueprints/miragon-ai-example/blob/40da36ef7cc20423a7312ca29d5a1c493043ca9c/kotlin-gradle/service/app/src/main/resources/bpmn/cancel-bike-order.bpmn
  - `check-credit-rating.dmn`: decision `checkCreditRating`, blob `f32fd4a`. Unchanged in the core,
    renamed to `checkCreditRating.dmn`.
    https://github.com/miragon-blueprints/miragon-ai-example/blob/40da36ef7cc20423a7312ca29d5a1c493043ca9c/kotlin-gradle/service/app/src/main/resources/dmn/check-credit-rating.dmn
  - `clarify-alternative.form`: form `clarifyAlternativeForm`, blob `564c6a3`. Unchanged and still deployed.
    https://github.com/miragon-blueprints/miragon-ai-example/blob/40da36ef7cc20423a7312ca29d5a1c493043ca9c/kotlin-gradle/service/app/src/main/resources/forms/clarify-alternative.form
  - `clarify-return.form`: form `clarifyReturnForm`, blob `f25e015`. Removed together with `cancelBikeOrder`.
    https://github.com/miragon-blueprints/miragon-ai-example/blob/40da36ef7cc20423a7312ca29d5a1c493043ca9c/kotlin-gradle/service/app/src/main/resources/forms/clarify-return.form
- Frozen copies in this folder are byte-identical to the blobs above. They are not deployed,
  generated or linted:
  - [original-bike-leasing.bpmn](original-bike-leasing.bpmn) (blob `6c221a5`)
  - [original-cancel-bike-order.bpmn](original-cancel-bike-order.bpmn) (blob `42b4f5a`)
  - Check them with `git hash-object docs/use-cases/bike-leasing/original/*.bpmn`.
- Rendered original: [bike-leasing-original.png](../../../assets/bike-leasing-original.png)
  (at the commit: https://github.com/miragon-blueprints/miragon-ai-example/blob/40da36ef7cc20423a7312ca29d5a1c493043ca9c/docs/assets/bike-leasing.png)

At a glance:

- Original: 43 flow nodes and 33 sequence flows, plus the called process `cancelBikeOrder` (7 flow nodes).
- Core: 23 flow nodes and 21 sequence flows, and no called process.
- Every element that survived kept its id.

## Capabilities

C1 to C19 are the capabilities of the original, derived from its models, code and tests.
"Simplified" means the business behaviour is still there in a smaller form.

| Capability | Original BPMN elements | In the core? | Where now (core ids) or issue |
|---|---|---|---|
| **C1** Leasing request intake | startEvent_leasingRequestReceived | simplified | startEvent_leasingRequestReceived. The request carries the bike id only; bike model and portfolio → [#7] |
| **C2** Credit rating check via DMN | businessRuleTask_checkCreditRating, gateway_isSolvent | kept | same ids; DMN checkCreditRating unchanged |
| **C3** Rejection of non-solvent applicants | gateway_rejectionJoin, serviceTask_sendRejection, endEvent_applicationRejected | kept | same ids |
| **C4** Contract conclusion: issue, send and sign the leasing contract | subProcess_concludeContract with startEvent_customerEligible, serviceTask_sendContract, gateway_awaitSignature, event_contractSigned, endEvent_contractValid | simplified | serviceTask_sendContract, gateway_awaitSignature, event_contractSigned (sub-process flattened); sourcing → [#8] |
| **C5** Signature reminder after 7 days | event_signatureReminder, serviceTask_sendReminderMail, endEvent_prospectReminded | removed | [#1] |
| **C6** Signature deadline (14 days) escalation to rejection | event_signatureDeadline, endEvent_notSigned, event_contractNotSigned (escalation contractNotSigned) | simplified | event_signatureDeadline → gateway_rejectionJoin, no escalation; open question 1 → [#8] |
| **C7** Insurance policy issuance (parallel to bike order) | gateway_fork, serviceTask_issueInsurancePolicy, gateway_join, event_compensateInsurance, serviceTask_cancelPolicy | removed | [#2] |
| **C8** Bike order at the dealer | gateway_fork, gateway_bikeSourceJoin, serviceTask_orderBike, gateway_join | simplified | gateway_bikeSourceJoin, serviceTask_orderBike, now in sequence (no fork/join, no audit listener); sourcing → [#10] |
| **C9** Bike unavailable: clarify an alternative with the customer and re-order | event_bikeUnavailable, userTask_clarifyAlternative, gateway_alternativeFound, gateway_bikeSourceJoin | kept | same ids; the REST completion no longer stores a bike model ([#7]); task UI → [#11] |
| **C10** Back-office inbox, application list and bike catalogue | userTask_clarifyAlternative (read models only) | removed | inbox [#5], list [#6], catalogue [#7]. The single-application view `GET /api/bike-leasing/{id}` stays |
| **C11** No alternative found: reverse contract and policy, confirm cancellation, terminate | event_triggerReversal, event_compensateContract, serviceTask_cancelContract, event_compensateInsurance, serviceTask_cancelPolicy, serviceTask_confirmContractCancellation, endEvent_contractCancelled (terminate) | simplified | event_triggerReversal, event_compensateContract (now on serviceTask_sendContract), serviceTask_cancelContract, serviceTask_confirmContractCancellation, endEvent_contractCancelled (none end); policy part → [#2] |
| **C12** Customer withdrawal with SAGA compensation | subProcess_applicationWithdrawn with startEvent_applicationWithdrawn, event_reverseApplication, serviceTask_sendCancellationConfirmation, endEvent_applicationCancelled; Message_ApplicationWithdrawn | removed | [#3] |
| **C13** Bike order cancellation at the dealer (called process cancelBikeOrder) | event_compensateOrder, callActivity_cancelBikeOrder; in cancelBikeOrder: startEvent_cancellationRequired, serviceTask_requestCancellation, gateway_cancellationPossible, endEvent_orderReversed | removed | [#4]; open question 2 |
| **C14** Return clarification with the supplier (form-only human task) | in cancelBikeOrder: userTask_clarifyReturn (clarifyReturnForm) | removed | [#4]; the form-only pattern is discussed in [#11] |
| **C15** Cancellation cost booking | in cancelBikeOrder: serviceTask_bookCosts | removed | [#4] |
| **C16** Handover reporting | gateway_join, event_handoverReported | simplified | event_handoverReported, directly after the order (no join); sourcing → [#10] |
| **C17** Withdrawal period (14 days) and leasing activation | event_withdrawalPeriodElapsed, endEvent_leasingActive | kept | same ids; withdrawing during the period comes back with [#3] |
| **C18** Incident demo (dealer outage, retries, incident) | serviceTask_orderBike (asyncBefore, retry cycle R3/PT10S) | kept | serviceTask_orderBike; now also covered by a process test |
| **C19** Customer notifications (cross-cutting) | serviceTask_sendContract, serviceTask_sendReminderMail, serviceTask_sendRejection, serviceTask_sendCancellationConfirmation, serviceTask_confirmContractCancellation | simplified | serviceTask_sendContract, serviceTask_sendRejection, serviceTask_confirmContractCancellation; reminder → [#1], withdrawal confirmation → [#3]; channel → [#9] |

Totals: 5 kept (C2, C3, C9, C17, C18), 7 simplified (C1, C4, C6, C8, C11, C16, C19), 7 removed
(C5, C7, C10, C12, C13, C14, C15).

## Technical simplifications (not business features)

| Change | Original | Core |
|---|---|---|
| Java/Maven variant removed | two equivalent services, `kotlin-gradle/` and `java-maven/`, kept identical by a CI parity check (Blueprint Checks) | one Kotlin service |
| Single Gradle project | two modules, `service:app` and `service:common-architecture-tests` | one project at the repo root; the architecture rules live in `src/test/kotlin/io/miragon/common/architecture/` |
| "Conclude contract" flattened, escalation removed | subProcess_concludeContract with its own start and end events; the deadline threw escalation `contractNotSigned`, caught by boundary event_contractNotSigned | the elements sit on the process level; the 14-day timer leads straight to gateway_rejectionJoin (flow_deadlineToRejection) |
| Compensation moved | event_compensateContract on subProcess_concludeContract | event_compensateContract on serviceTask_sendContract (see open question 3) |
| Terminate end → none end | endEvent_contractCancelled terminated the instance, killing the insurance token waiting at gateway_join | a plain end event; no parallel token is left to kill |
| Execution listener removed | BikeOrderAuditListener on serviceTask_orderBike (end event, technical audit log) | removed |
| DevCorsConfiguration removed | dev-profile CORS bean for a browser frontend on another origin | removed; the service is headless |
| Bike portfolio table dropped | table `bike_portfolio` created by `V1__init.sql` and seeded at start-up | `V2__drop_bike_portfolio.sql` drops it; V1 stays unchanged |
| Models executed from `docs/` | `src/main/resources/bpmn/` and `dmn/` | `docs/use-cases/bike-leasing/process/`, packaged by Gradle (see [executable-process.md](../process/executable-process.md)) |
| DMN file renamed | `check-credit-rating.dmn` | `checkCreditRating.dmn`: the file stem equals the decision id, so designIQ links the business rule task to the decision |
| Definitions removed | Message_ApplicationWithdrawn (definition id `Msg_AntragZurueckgezogen_d`, the only German id), Escalation_ContractNotSigned | gone; `cancel-bike-order.bpmn` and `clarify-return.form` left the deployment and are frozen here |

## Open questions inherited from the original

1. The deadline rejection keeps the issued contract (C6). "Send contract" issues the contract before
   the signature wait. When the 14-day deadline rejects the application, nothing revokes it. In the
   original, the escalation interrupted "Conclude contract" before its compensation was ever
   registered. In the core, the timer leads straight to the rejection, and "Cancel contract" runs only
   after "Trigger reversal". Should a rejected application void its contract? Carried by [#8].
2. cancelBikeOrder: the gateway labels contradict the conditions (C13, C14). `flow_cancellationNotPossible`
   is labelled "No" but fires on `${cancellationPossible}`. The default flow `flow_cancellationPossible`,
   labelled "Yes", fires when cancellation is not possible. The form text matches the condition; the
   labels match the opposite reading. The simulated dealer always answered "possible", so only the
   return-and-costs branch ever ran. Decide which reading is right before the process comes back.
   Carried by [#4].
3. The compensation now sits on "Send contract" (C11, C12). In the original, the contract became
   compensable only when "Conclude contract" completed, that is after the signature. In the core it is
   compensable as soon as it was sent. Today the only reversal ("Trigger reversal") happens after the
   signature, so behaviour is unchanged. This matters only once a withdrawal can reverse a case before
   the signature: the core would then revoke a sent but unsigned contract, the original would not
   (that case was never tested). Carried by [#3].

In designIQ, the frozen original shows 9 validator errors and 2 warnings: the compensation false
positives (see [executable-process.md](../process/executable-process.md)), 14 activities, and the
call to `cancelBikeOrder`, whose file stem differs. This is expected; the copy is reference only.

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
