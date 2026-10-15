# Glossary: MiraVelo bike leasing

One vocabulary for the workshop, the models and the code. When a word comes up in the demo, look it
up here: what it means for the business, where it appears in the models and where in the code.

How to read the tables:

- **Status** `core` = exists today, in the running process or in this repo. `planned #N` = only in
  the original model; it comes back with issue #N (links at the end).
- **In the models** cites element ids of the executable `bike-leasing.bpmn` and `checkCreditRating.dmn`.
  Cells starting with "original:" cite the frozen original (`original-bike-leasing.bpmn`,
  `original-cancel-bike-order.bpmn`) instead, because the core has no model or code for them yet.
- Models are named by file name. File names are unique under `docs/`, and designIQ uses them as ids.
- `{id}` in an endpoint is the application id (`{applicationId}` in the OpenAPI contract).

Where things live:

- Executable process and decision: [bike-leasing.bpmn](use-cases/bike-leasing/process/bike-leasing.bpmn),
  [checkCreditRating.dmn](use-cases/bike-leasing/process/checkCreditRating.dmn)
- Frozen original (not deployed): [original/](use-cases/bike-leasing/original/)
- The use case: [bike-leasing-use-case.md](use-cases/bike-leasing/bike-leasing-use-case.md) · all docs: [README.md](README.md)
- Code: [src/main/kotlin/io/miragon/blueprint/](../src/main/kotlin/io/miragon/blueprint/)
  (`domain`, `application`, `adapter`)

## Naming rules

| What | Rule | Examples |
|---|---|---|
| Activity (task) | verb + object, in business language | Check credit rating · Send contract · Order bike from dealer |
| Event | object + state | Leasing request received · Contract signed · Handover reported |
| Splitting gateway | a question ending in "?"; outgoing flows answer it | Solvent? · Alternative found? ("Yes" / "No") |
| Merging gateway | unnamed, or named after what it collects | Rejection reason |
| Timer | the duration | 14 days |
| BPMN element id | `<type>_<camelCase>`, never changed once released | `serviceTask_orderBike` · `event_contractSigned` · `gateway_isSolvent` |
| Message | id `Message_<PascalCase>`, name `miravelo.<camelCase>` | `Message_ContractSigned` / `miravelo.contractSigned` |
| Decision (DMN) | file name = decision id, so designIQ links the rule task | `checkCreditRating.dmn` → `checkCreditRating` |
| Code | English. Types PascalCase with their layer suffix, variables camelCase, statuses UPPER_SNAKE_CASE, REST paths kebab-case | `SignContractUseCase` · `bikeId` · `HANDED_OVER` · `/sign-contract` |
| One term, one name | the business name runs through model and code | Send contract → `serviceTask_sendContract` → `#{sendContractDelegate}` → `SendContractDelegate` → `SendContractUseCase` |

Known deviations, inherited from the original (ids stay stable):

- "Await end of withdrawal period" reads like an activity; its id `event_withdrawalPeriodElapsed`
  follows the event rule.
- "Send cancellation confirmation" has the id `serviceTask_confirmContractCancellation`.
- In the code, "Send rejection" calls `RejectApplicationUseCase`, "Clarify alternative with customer"
  calls `SelectAlternativeUseCase`, and "Cancel contract" calls `ContractPort.revokeContract`.

## Say this, not that

| Say | Not | Why |
|---|---|---|
| Customer | prospect, applicant, lessee | One word for the person everywhere. "Prospect reminded" exists only in the original. |
| Dealer | supplier, vendor | The original said "supplier" ("Clarify return with supplier") for the same party as `BikeDealerPort`. |
| Handover | delivery | The dealer reports the handover; there is no separate delivery step. |
| Reversal | rollback, storno | The business word for undoing completed steps. "Compensation" is the BPMN mechanism behind it. |
| Withdrawal | abort | The original tests called the customer's withdrawal "abort". |
| Withdrawal period | cooling-off period | One name for the 14 days after the handover. |

Do not mix up:

- **Signature deadline** (14 days to sign, before the order) and **withdrawal period** (14 days after
  the handover, before the lease is active).
- **Withdrawal period** (core: the process only waits it out) and **Withdrawal** (planned #3: the
  customer withdraws the application).
- **REJECTED** (no signed contract: not solvent, or the deadline passed) and **CANCELLED** (contract
  signed, but no acceptable alternative bike).
- **Cancel contract** (revoke the leasing contract, core) and **Bike order cancellation** (cancel the
  dealer order, planned #4).
- **Tasklist** (the CIB seven product, core) and **Back-office inbox** (a domain list, planned #5).
- **Bike id** (core) and **bike model** (the catalogue name, planned #7). "Model" alone means a BPMN,
  DMN or Wardley model.

## Actors

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| MiraVelo | The fictional bike brand that leases bikes to customers and runs this process. | process `bikeLeasingProcess` "MiraVelo Bike Leasing"; message names `miravelo.*` | `Messages.MIRAVELO_*` | core |
| Customer | The person who applies for a lease, signs the contract and receives the bike. | "Contract signed" (`event_contractSigned`); "Clarify alternative with customer" (`userTask_clarifyAlternative`) | `CustomerName`, `Email`, `LeasingApplication.customerName` | core |
| Back office | MiraVelo staff who agree an alternative bike with the customer when the requested one is out of stock. | `userTask_clarifyAlternative` | Tasklist form `clarifyAlternativeForm`, or `POST /api/bike-leasing/{id}/clarify-alternative` | core |
| Operations | Whoever watches running cases and resolves incidents. | incidents on `serviceTask_orderBike` | – (works in the Cockpit) | core |
| Dealer | The bike dealer: confirms availability, takes the order, reports the handover. Simulated in the core (decision #10). | "Order bike from dealer" (`serviceTask_orderBike`); "Handover reported" (`event_handoverReported`) | `BikeDealerPort` → `BikeDealerAdapter`; `POST /api/bike-leasing/{id}/report-handover` | core |

## Application and credit check

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| Leasing request | What the customer submits: name, email, age, monthly net income and bike id. It starts one process instance. | "Leasing request received" (`startEvent_leasingRequestReceived`); `Message_LeasingRequestReceived` / `miravelo.leasingRequestReceived` | `POST /api/bike-leasing`, `SubmitLeasingRequestUseCase`, `LeasingRequestInput` | core |
| Leasing application | The record MiraVelo keeps for one request: customer, bike id, status and, once known, contract id and order id. Its application id (a UUID) is also the business key. | one instance of `bikeLeasingProcess`; variable `applicationId` | `LeasingApplication`, `ApplicationId`, table `leasing_application`, `GET /api/bike-leasing/{id}` | core |
| Age / Monthly net income | The two facts the credit rating decides on. Income must be above zero, otherwise the request is refused (400). | DMN inputs `input_age` "Age", `input_monthlyNetIncome` "Monthly Net Income"; variables `age`, `monthlyNetIncome` | `LeasingApplication.age`, `LeasingApplication.monthlyNetIncome` | core |
| Check credit rating | Automatic decision whether the customer may lease: not solvent if under 18, over 99, or earning less than 1500 a month. | `businessRuleTask_checkCreditRating` → decision `checkCreditRating` (hit policy FIRST; rules `rule_tooYoung`, `rule_lowIncome`, `rule_tooOld`, `rule_solvent`) | – (the engine evaluates the DMN; no Kotlin code) | core |
| Solvent? | The answer of the credit rating. Only a solvent customer gets a contract. | `gateway_isSolvent`; DMN output `output_solvent` → variable `solvent`; flows `flow_solvent` (default), `flow_notSolvent` "No" | – (only the gateway reads it) | core |
| Rejection | The negative outcome before a contract is signed. Two reasons lead there: not solvent, or no signature within 14 days. The customer is told ("Your bike-leasing application was rejected"). | "Rejection reason" (`gateway_rejectionJoin`) → "Send rejection" (`serviceTask_sendRejection`) → "Application rejected" (`endEvent_applicationRejected`) | `SendRejectionDelegate`, `RejectApplicationUseCase`, `LeasingApplication.reject` | core |
| Application list | A paged list of leasing applications, filterable by status, for a customer portal. | – (process-level) | – (the core reads single applications only) | planned #6 |

## Contract

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| Leasing contract | The contract between MiraVelo and the customer. The contract system issues it when it is sent; its reference is stored on the application. Simulated in the core (decision #8). | "Send contract" (`serviceTask_sendContract`) | `ContractPort.issueContract` → `ContractId` (`CONTRACT-<uuid>`); `LoggingContractAdapter` | core |
| Send contract | Issue the contract and ask the customer to sign it ("Please review and sign your leasing contract"). | `serviceTask_sendContract` (`#{sendContractDelegate}`) | `SendContractDelegate`, `SendContractUseCase` | core |
| Contract signed | The customer has signed, so the bike can be ordered. Signing when no signature is awaited is refused (409). | `event_contractSigned`; `Message_ContractSigned` / `miravelo.contractSigned` | `POST /api/bike-leasing/{id}/sign-contract`, `SignContractUseCase`, `LeasingProcess.correlateContractSigned` | core |
| Signature deadline | 14 days to sign after the contract was sent; without a signature the application is rejected. Not the withdrawal period. | timer "14 days" (`event_signatureDeadline`, P14D) after the event-based gateway `gateway_awaitSignature`; `flow_deadlineToRejection` | – (an engine timer) | core |
| Cancel contract | Revoke an issued contract. Runs only as part of a reversal (no alternative bike found); a rejection after the deadline does not revoke the contract. | `serviceTask_cancelContract`, the compensation handler of "Send contract" (`event_compensateContract`) | `CancelContractDelegate`, `CancelContractUseCase`, `ContractPort.revokeContract` | core |
| Customer notification | A message to the customer, sent by "Send contract", "Send rejection" and "Send cancellation confirmation". The channel is simulated (decision #9). | `serviceTask_sendContract`, `serviceTask_sendRejection`, `serviceTask_confirmContractCancellation` | `NotificationPort.send` → `LoggingNotificationAdapter` | core |
| Signature reminder | One reminder if the contract is still unsigned after 7 days; the 14-day deadline stays as it is. | original: timer "7 days" (`event_signatureReminder`, P7D) → `serviceTask_sendReminderMail` → `endEvent_prospectReminded` | – | planned #1 |
| Insurance policy | The policy that accompanies the leasing contract. An insurer issues it in parallel with the bike order; a reversal cancels it. | original: `gateway_fork` → `serviceTask_issueInsurancePolicy` → `gateway_join`; "Compensate policy" (`event_compensateInsurance`) → `serviceTask_cancelPolicy` | – | planned #2 |

## Bike order and alternative

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| Bike | The concrete bike an application is about. The core knows it only by its bike id. | variable `bikeId` (start event, input of `serviceTask_orderBike`, output of `userTask_clarifyAlternative`) | `BikeId`, `LeasingApplication.bikeId` | core |
| Order bike from dealer | Ask the dealer whether the bike is available and, if so, order it. The order id is stored and the status becomes ORDERED. | `serviceTask_orderBike` (`#{orderBikeDelegate}`), output `orderId` | `OrderBikeDelegate`, `OrderBikeUseCase`, `BikeDealerPort.checkAvailability` / `order` → `OrderId` (`ORDER-<uuid>`) | core |
| Bike unavailable | The dealer cannot deliver the requested bike, so no order is placed. Demo bike: `BIKE-OOS`. | error boundary `event_bikeUnavailable`; `Error_BikeUnavailable` (code `bikeUnavailable`) | `BikeUnavailableException` → `Errors.BIKE_UNAVAILABLE` | core |
| Clarify alternative with customer | The back office agrees another bike with the customer, in the Tasklist form or via REST. | user task `userTask_clarifyAlternative`, form `clarifyAlternativeForm` | `POST /api/bike-leasing/{id}/clarify-alternative`, `SelectAlternativeUseCase`, `ClarifyAlternativeTaskListener` | core |
| Alternative found? | Yes: the new bike is ordered. No: the contract is reversed. An accepted alternative must name its bike, otherwise 400. | `gateway_alternativeFound`, variable `alternativeFound`; `flow_alternativeFound` "Yes" (back via `gateway_bikeSourceJoin`), `flow_noAlternativeFound` "No" | `AlternativeDecisionInput`, `LeasingProcess.completeAlternativeClarification` | core |
| Reversal | Undo the steps already done when the deal cannot go ahead. In the core this revokes the contract. BPMN calls the mechanism compensation. | "Trigger reversal" (`event_triggerReversal`) → `event_compensateContract` → `serviceTask_cancelContract` | – (the engine runs it; see Cancel contract) | core |
| Send cancellation confirmation | Tell the customer the application is cancelled ("Your bike-leasing application has been cancelled"). | `serviceTask_confirmContractCancellation` (`#{sendCancellationConfirmationDelegate}`) → "Contract cancelled" (`endEvent_contractCancelled`) | `SendCancellationConfirmationDelegate`, `SendCancellationConfirmationUseCase`, `LeasingApplication.cancel` | core |
| Dealer outage | The dealer system cannot be reached while ordering. Demo bike: `BIKE-FAIL`. The order step retries three times, 10 seconds apart, then raises an incident. | `serviceTask_orderBike` (asyncBefore, `failedJobRetryTimeCycle` R3/PT10S) | `BikeDealerAdapter.DealerUnavailableException` | core |
| Back-office inbox | One list of all applications waiting on "Clarify alternative with customer": customer, requested bike, waiting since. | `userTask_clarifyAlternative` (core and original) | – (today the CIB seven Tasklist shows the open tasks) | planned #5 |
| Bike catalogue / Bike model | MiraVelo's list of bikes with their model name (the original had e.g. "Gravel Explorer 900") and live dealer availability. | – (no BPMN element) | – (the original table `bike_portfolio` from `V1__init.sql` is dropped by `V2__drop_bike_portfolio.sql`) | planned #7 |
| Bike order cancellation | Cancel a placed order at the dealer when the customer withdraws after the bike was ordered. | original: "Compensate order" (`event_compensateOrder`) → `callActivity_cancelBikeOrder` → process `cancelBikeOrder` ("Request cancellation" `serviceTask_requestCancellation`, "Cancellation possible?" `gateway_cancellationPossible`, "Order reversed" `endEvent_orderReversed`) | – | planned #4 |
| Return clarification | Arrange the physical return of an ordered bike with the dealer. | original: "Clarify return with supplier" (`userTask_clarifyReturn`, form `clarifyReturnForm`) | – | planned #4 |
| Cancellation costs | The fee the dealer charges for cancelling a placed order; booked after the return is arranged. | original: "Book costs" (`serviceTask_bookCosts`) | – | planned #4 |

## Handover and activation

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| Handover | The dealer hands the bike to the customer and reports it; this starts the withdrawal period. Reporting before the bike is ordered is refused (409). | "Handover reported" (`event_handoverReported`); `Message_HandoverReported` / `miravelo.handoverReported` | `POST /api/bike-leasing/{id}/report-handover`, `ReportHandoverUseCase`, `LeasingApplication.reportHandover` | core |
| Withdrawal period | The 14 days after the handover before the lease becomes active. In the core the process only waits; the customer cannot withdraw yet (that is Withdrawal, #3). | timer "Await end of withdrawal period" (`event_withdrawalPeriodElapsed`, P14D) | – (an engine timer) | core |
| Leasing active | The lease is live: the end of the happy path. | message end event "Leasing active" (`endEvent_leasingActive`; `Message_LeasingActivated` / `miravelo.leasingActivated`; `#{activateLeasingDelegate}`) | `ActivateLeasingDelegate`, `ActivateLeasingUseCase`, `LeasingApplication.activate` | core |
| Withdrawal | The customer withdraws a running application, also during the withdrawal period. Every completed step is reversed and the application ends cancelled. | original: event sub-process "Application withdrawn" (`subProcess_applicationWithdrawn`), start "Request withdrawn" (`startEvent_applicationWithdrawn`, `Message_ApplicationWithdrawn` / `miravelo.applicationWithdrawn`), "Reverse application" (`event_reverseApplication`), "Leasing cancelled" (`endEvent_applicationCancelled`) | – | planned #3 |

## Statuses

The status is what `GET /api/bike-leasing/{id}` returns. It lives in the domain (`LeasingStatus`), not
in the engine; the services set it when the process reaches the step.

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| RECEIVED | Submitted. Stays so through the credit rating and the signature, until a bike order exists. | set before `startEvent_leasingRequestReceived` | `LeasingStatus.RECEIVED` (`LeasingApplication.receive`) | core |
| ORDERED | The dealer accepted the bike order. | `serviceTask_orderBike` | `LeasingStatus.ORDERED` (`documentOrder`) | core |
| HANDED_OVER | The customer has the bike; the withdrawal period is running. | `event_handoverReported` | `LeasingStatus.HANDED_OVER` (`reportHandover`) | core |
| ACTIVE | Final: the lease is live. | `endEvent_leasingActive` | `LeasingStatus.ACTIVE` (`activate`) | core |
| REJECTED | Final: not solvent, or not signed within 14 days. | `serviceTask_sendRejection` → `endEvent_applicationRejected` | `LeasingStatus.REJECTED` (`reject`) | core |
| CANCELLED | Final: no acceptable alternative bike, so the contract was reversed. | `serviceTask_confirmContractCancellation` → `endEvent_contractCancelled` | `LeasingStatus.CANCELLED` (`cancel`) | core |
| WITHDRAWN | The customer has withdrawn and the reversal is still running; ends as CANCELLED. | original: `startEvent_applicationWithdrawn` | – (not in `LeasingStatus`) | planned #3 |

## Technical terms you meet in the demo

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| BPMN | Business Process Model and Notation: the standard diagram language for processes. The file designIQ shows is the file the engine runs. | `bike-leasing.bpmn` (process `bikeLeasingProcess`) | generated `BikeLeasingProcessProcessApi` (bpmn-to-code) | core |
| DMN | Decision Model and Notation: business rules as a decision table. | `checkCreditRating.dmn` (decision `checkCreditRating`) | – (the engine evaluates it) | core |
| Process engine | CIB seven, the community fork of Camunda 7, embedded in the bike-leasing service. It runs the BPMN and DMN models and remembers where every case waits. | runs `bikeLeasingProcess` and `checkCreditRating` | `LeasingProcessAdapter`; the delegates in `adapter/inbound/cibseven`; `/engine-rest` | core |
| Process instance | One run of the process for one leasing application, from "Leasing request received" to an end event. | an instance of `bikeLeasingProcess` | `LeasingProcess.submitRequest` starts it | core |
| Business key | The application id attached to a process instance. Every message and task completion finds the right instance through it. | – | `LeasingProcessAdapter`, `correlateByBusinessKey`, `execution.processBusinessKey` in the delegates | core |
| Message | A named signal from outside that moves a waiting instance on, for example the signature. If no instance waits for it, the call is refused (409 "Action not available in the current state"). | `Message_*`, names `miravelo.*` | `Messages` (generated), `GlobalExceptionConfiguration` | core |
| User task | A step a person does; the instance waits until it is completed. The core has one. | `userTask_clarifyAlternative` | `LeasingProcess.completeAlternativeClarification` | core |
| Incident | A step failed and used up its retries. The case stops there until Operations fixes the cause and retries it in the Cockpit. | `serviceTask_orderBike` (R3/PT10S) | `DealerOutageIncidentTest` | core |
| Cockpit / Tasklist | CIB seven web apps at `/camunda` (admin/admin). Cockpit shows running cases and incidents (Operations); Tasklist shows open user tasks with their forms (Back office). The new CIB seven webclient runs at `/webapp`. | form `clarifyAlternativeForm` (in the Tasklist) | `application.yaml` | core |
| Simulated system | An external system the core only fakes with a logging adapter, so the demo needs no partners: contract system (#8), notification channel (#9), dealer system (#10). | – | `LoggingContractAdapter`, `LoggingNotificationAdapter`, `BikeDealerAdapter` | core |
| Bruno scenario | A scripted series of REST calls that plays customer, dealer and back office against the running service. `00-demo-data` prepares cases for a live demo. | – | `bruno/` (`00-demo-data` … `08-incident-demo`) | core |
| t.BPM | Tangible BPM: modelling a process together at a table with a reduced BPMN palette (activity, event, gateway, document). The table-top model is the shared language. | `bike-leasing-tbpm.bpmn` (workshop mode) | – | core |
| Feature map | The t.BPM model plus the planned features, in stages: Core (runs today), Stage 1 – Customer follow-up, Stage 2 – Insurance, Stage 3 – Withdrawal, Stage 4 – Portal and back-office views. A group marks each feature that adds steps (#1 to #4), and a note sticky names every feature (#1 to #7). No feature loses its place in the process. | `bike-leasing-feature-map.bpmn`, `feature-map.md` | – | core |
| C4 model | Architecture diagrams at two zoom levels: system context (who and what talks to the service) and container (what runs inside it). | `c4-system-context.md`, `c4-container.md` (Mermaid: drawn on GitHub, raw text in designIQ) | – | core |
| Wardley Map | A map of the components behind a user need: up = visible to the user, right = more evolved (genesis to commodity). It grounds the make, buy or adapt decisions #8 to #11. | `sourcing-map.owm` (components such as `Contract & e-signature`), `sourcing-decisions.md` | – | core |
| designIQ | A Git-native workspace where teams model their business and architecture together, live. It shows every model under `docs/` from the default branch; edits reach Git through a release pull request. | `designiq.yml` (`models: docs`) at the repo root | – | core |
| Todo | A GitHub issue that designIQ pins to process elements: labels `todo` and `process:bike-leasing` plus an anchor block with element ids. Issues #1 to #11 are todos. | anchor lines `element: <id>`, e.g. `serviceTask_sendContract` (#1, #8) | – | core |

## Issues referenced

Features (stage in the feature map):

- [#1 Signature reminder](https://github.com/miragon-blueprints/miragon-ai-example/issues/1) (Stage 1 – Customer follow-up)
- [#2 Insurance policy](https://github.com/miragon-blueprints/miragon-ai-example/issues/2) (Stage 2 – Insurance)
- [#3 Withdrawal](https://github.com/miragon-blueprints/miragon-ai-example/issues/3) (Stage 3 – Withdrawal)
- [#4 Bike order cancellation](https://github.com/miragon-blueprints/miragon-ai-example/issues/4) (Stage 3 – Withdrawal, needs #3)
- [#5 Back-office inbox](https://github.com/miragon-blueprints/miragon-ai-example/issues/5) (Stage 4 – Portal and back-office views)
- [#6 Application list](https://github.com/miragon-blueprints/miragon-ai-example/issues/6) (Stage 4 – Portal and back-office views)
- [#7 Bike catalogue](https://github.com/miragon-blueprints/miragon-ai-example/issues/7) (Stage 4 – Portal and back-office views)

Decisions (make, buy or adapt):

- [#8 Contract & e-signature](https://github.com/miragon-blueprints/miragon-ai-example/issues/8)
- [#9 Customer notification](https://github.com/miragon-blueprints/miragon-ai-example/issues/9)
- [#10 Dealer integration](https://github.com/miragon-blueprints/miragon-ai-example/issues/10)
- [#11 Back-office task UI](https://github.com/miragon-blueprints/miragon-ai-example/issues/11)
