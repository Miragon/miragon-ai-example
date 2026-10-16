# Glossary: MiraVelo bike leasing

One vocabulary for the workshop, the models and the code. When a word comes up in the demo, look it
up here: what it means for the business, where it appears in the models and where in the code.

How to read the tables:

- **Status** `core` = exists today, in the running process or in this repo. `planned #N` = not in
  the core; it comes (back) with feature issue #N (links at the end).
- **In the models** cites element ids of the executable `bike-leasing.bpmn`. Cells starting with
  "original:" cite the frozen original (`original-bike-leasing.bpmn`, `original-cancel-bike-order.bpmn`,
  `checkCreditRating.dmn` in `original/`) instead, because the core has no model or code for them.
  Cells starting with "map:" cite the feature map, where a planned feature is already drawn.
- Models are named by file name. File names are unique under `docs/`, and designIQ uses them as ids.
- `{id}` in an endpoint is the application id (`{applicationId}` in the OpenAPI contract).

Where things live:

- Executable process: [bike-leasing.bpmn](use-cases/bike-leasing/process/bike-leasing.bpmn)
- Frozen original and the credit decision (not deployed): [original/](use-cases/bike-leasing/original/)
- The use case: [bike-leasing-use-case.md](use-cases/bike-leasing/bike-leasing-use-case.md) · all docs: [README.md](README.md)
- Backend code: [src/main/kotlin/io/miragon/blueprint/](../src/main/kotlin/io/miragon/blueprint/)
  (`domain`, `application`, `adapter`) · frontend: [frontend/src/](../frontend/src/)

## Naming rules

| What | Rule | Examples |
|---|---|---|
| Activity (task) | verb + object, in business language | Validate application · Check credit rating · Order bike from dealer |
| Event | object + state | Leasing request received · Contract signed · Application invalid |
| Splitting gateway | a question ending in "?"; outgoing flows answer it | Solvent? ("No" / default) |
| Merging gateway | unnamed, or named after what it collects | Rejection reason |
| Parallel gateway | unnamed | `gateway_fork`, `gateway_join` |
| Timer | the duration | 14 days (planned #18, #21) |
| BPMN element id | `<type>_<camelCase>`, never changed once released | `serviceTask_orderBike` · `event_contractSigned` · `gateway_isSolvent` |
| Sequence flow id | `flow_<camelCase>` describing the step, as bpmnlint requires | `flow_requestToValidation` · `flow_notSolvent` |
| Message | id `Message_<PascalCase>`, name `miravelo.<camelCase>` | `Message_ContractSigned` / `miravelo.contractSigned` |
| Decision (DMN) | file name = decision id, so designIQ links the rule task | `checkCreditRating.dmn` → `checkCreditRating` |
| Code | English. Types PascalCase with their layer suffix, variables camelCase, statuses UPPER_SNAKE_CASE, REST paths kebab-case | `SignContractUseCase` · `bikeId` · `CONTRACT_SIGNED` · `/sign-contract` |
| One term, one name | the business name runs through model and code | Send contract → `serviceTask_sendContract` → `#{sendContractDelegate}` → `SendContractDelegate` → `SendContractUseCase` |
| Frontend routes | German, like the UI copy | `/antraege/neu` (new application) · `/aufgaben` (inbox) |

Known deviations (ids stay stable):

- In the code, "Send rejection" calls `RejectApplicationUseCase`, and the two user tasks are
  completed through `CompleteCreditCheckUseCase` and `CompleteReversalUseCase`.
- The boundary event "Application invalid" is `event_applicationInvalid`; the reference model calls
  it `boundary_applicationInvalid` (see executable-process.md).

## Say this, not that

| Say | Not | Why |
|---|---|---|
| Customer | prospect, applicant, lessee | One word for the person everywhere. "Prospect reminded" exists only in the original. |
| Dealer | supplier, vendor | The original said "supplier" ("Clarify return with supplier") for the same party as `BikeDealerPort`. |
| Handover | delivery | The dealer reports the handover; there is no separate delivery step. |
| Withdrawal | abort, cancellation | The customer withdraws; the original tests called it "abort". |
| Reversal | rollback, storno | Undoing the steps already done. Today the back office does it by hand ("Reverse application"); "compensation" is the BPMN mechanism planned for it (#3). |
| Inbox | Tasklist | The back office works in the frontend inbox. The CIB seven Tasklist is a tool for operations. |
| Withdrawal period | cooling-off period | One name for the 14 days after the handover (planned #21). |

Do not mix up:

- **Application invalid** (wrong data: under 18, or a bike outside the catalogue; checked by the
  service) and **Not solvent** (the back office's credit decision). Both end as REJECTED.
- **Withdrawal** (core: the customer withdraws a running application) and **Withdrawal period**
  (planned #21: 14 days after the handover before the lease is active).
- **Signature deadline** (planned #18: 14 days to sign) and **Withdrawal period** (planned #21:
  14 days after the handover).
- **WITHDRAWN** (the customer withdrew; the reversal is still open) and **CANCELLED** (the reversal
  is done).
- **REJECTED** (no contract signed: invalid or not solvent) and **CANCELLED** (withdrawn and
  reversed).
- **Reverse application** (core: a user task, done by hand) and **automatic reversal** (planned #3:
  compensation of every completed step).
- **Bike order** (core: placed in parallel with the policy) and **Bike order cancellation** (planned
  #4: cancel the dealer order after a withdrawal).
- **Bike id** (`BIKE-900`) and **bike model** ("Gravel Explorer 900"). "Model" alone means a BPMN,
  DMN or Wardley model.

## Actors

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| MiraVelo | The fictional bike brand that leases bikes to customers and runs this process. | process `bikeLeasingProcess` "MiraVelo Bike Leasing"; message names `miravelo.*` | `Messages.MIRAVELO_*` | core |
| Customer | The person who applies for a lease, signs the contract, may withdraw and receives the bike. | "Leasing request received" (`startEvent_leasingRequestReceived`); "Contract signed" (`event_contractSigned`); "Application withdrawn" (`startEvent_applicationWithdrawn`) | `CustomerName`, `Email`, `LeasingApplication.customerName`; frontend `/antraege/neu`, `/antraege/{id}` | core |
| Back office | MiraVelo staff who check the credit rating and reverse withdrawn applications. They work in the inbox of the frontend. | `userTask_checkCreditRating`, `userTask_reverseApplication` | `GET /api/tasks`, `POST /api/bike-leasing/{id}/credit-check`, `POST /api/bike-leasing/{id}/reversal`; frontend `/aufgaben` | core |
| Operations | Whoever watches running cases and resolves incidents. | incidents on `serviceTask_orderBike` | – (works in the CIB seven Cockpit) | core |
| Dealer | The bike dealer: offers the catalogue, takes the order, reports the handover. Simulated in the core (decision #10). | "Order bike from dealer" (`serviceTask_orderBike`); "Handover reported" (`event_handoverReported`) | `BikeDealerPort` → `BikeDealerAdapter`; `POST /api/bike-leasing/{id}/report-handover` | core |
| Insurer | Issues the insurance policy for the leased bike. Simulated in the core. | "Issue insurance policy" (`serviceTask_issueInsurancePolicy`) | `InsurancePort` → `LoggingInsuranceAdapter` | core |

## Application, validation and credit check

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| Leasing request | What the customer submits: name, email, age, monthly net income and bike id. It starts one process instance. | "Leasing request received" (`startEvent_leasingRequestReceived`); `Message_LeasingRequestReceived` / `miravelo.leasingRequestReceived`; start variables `applicationId`, `bikeId`, `bikeModel` | `POST /api/bike-leasing`, `SubmitLeasingRequestUseCase`, `LeasingRequestInput`; frontend `/antraege/neu` | core |
| Leasing application | The record MiraVelo keeps for one request: customer, bike, status and, once known, contract id, policy id and order id. Its application id (a UUID) is also the business key. | one instance of `bikeLeasingProcess`; variable `applicationId` | `LeasingApplication`, `ApplicationId`, table `leasing_application`, `GET /api/bike-leasing/{id}`; frontend `/antraege/{id}` | core |
| Application list | A paged list of leasing applications, newest first, filterable by status. | – (process-level) | `GET /api/bike-leasing?status=&page=&size=`, `ListLeasingApplicationsQuery`; frontend `/` | core (delivered #6) |
| Age / Monthly net income | Facts on the application. Age is validated (at least 18); both are shown to the back office for the credit check. Income must be above zero, otherwise the request is refused (400). | – (not process variables) | `LeasingApplication.age`, `LeasingApplication.monthlyNetIncome` | core |
| Validate application | Check the application before anyone works on it. **Assumption:** valid means the applicant is at least 18 and the bike is in the dealer's catalogue. | `serviceTask_validateApplication` (`#{validateApplicationDelegate}`) | `ValidateApplicationDelegate`, `ValidateApplicationUseCase`, `LeasingApplication.validate` | core |
| Application invalid | The validation failed; the application goes straight to the rejection. | error boundary `event_applicationInvalid` on `serviceTask_validateApplication`; `Error_ApplicationInvalid` (code `applicationInvalid`); `flow_applicationInvalid` | `ApplicationInvalidException` → `BpmnError(Errors.APPLICATION_INVALID.code)` | core |
| Check credit rating | The back office decides whether the customer may lease: "solvent" or "not solvent". A manual user task; no rule guides it yet. | user task `userTask_checkCreditRating`, output variable `solvent` | `POST /api/bike-leasing/{id}/credit-check` (`CreditCheckInput`), `CompleteCreditCheckUseCase`, inbox task type `CREDIT_CHECK`; frontend feature `check-credit-rating` | core |
| Credit decision (DMN) | The rules that would decide automatically: not solvent if under 18, over 99, or earning less than 1500 a month. | original: decision `checkCreditRating` (hit policy FIRST; rules `rule_tooYoung`, `rule_lowIncome`, `rule_tooOld`, `rule_solvent`), `businessRuleTask_checkCreditRating` | – | planned #19 |
| Solvent? | The answer of the credit check. Only a solvent customer gets a contract. | `gateway_isSolvent`; flows `flow_solvent` (default), `flow_notSolvent` "No" (`${!solvent}`) | – (only the gateway reads it) | core |
| Rejection | The negative outcome before a contract is signed. Two reasons lead there today: invalid, or not solvent. The customer is told ("Your bike-leasing application was rejected"). A third reason, the signature deadline, is planned (#18). | "Rejection reason" (`gateway_rejectionJoin`) → "Send rejection" (`serviceTask_sendRejection`) → "Application rejected" (`endEvent_applicationRejected`) | `SendRejectionDelegate`, `RejectApplicationUseCase`, `LeasingApplication.reject` | core |

## Contract

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| Leasing contract | The contract between MiraVelo and the customer. The contract system issues it when it is sent; its reference is stored on the application. Simulated in the core (decision #8). | "Send contract" (`serviceTask_sendContract`) | `ContractPort.issueContract` → `ContractId` (`CONTRACT-<uuid>`); `LoggingContractAdapter` | core |
| Send contract | Issue the contract and ask the customer to sign it ("Please review and sign your leasing contract"). | `serviceTask_sendContract` (`#{sendContractDelegate}`) | `SendContractDelegate`, `SendContractUseCase`, `LeasingApplication.sendContract` | core |
| Contract signed | The customer has signed, so policy and order can start. The process waits without a time limit. Signing when no signature is awaited is refused (409). | `event_contractSigned`; `Message_ContractSigned` / `miravelo.contractSigned` | `POST /api/bike-leasing/{id}/sign-contract`, `SignContractUseCase`, `LeasingApplication.signContract`; frontend feature `sign-contract` | core |
| Customer notification | A message to the customer, sent by "Send contract" and "Send rejection". The channel is simulated (decision #9). | `serviceTask_sendContract`, `serviceTask_sendRejection` | `NotificationPort.send` → `LoggingNotificationAdapter` | core |
| Signature deadline | 14 days to sign after the contract was sent; without a signature the application is rejected. | map: event-based gateway `gateway_awaitSignature`, timer "14 days" (`event_signatureDeadline`, P14D), `flow_deadlineToRejection` | – | planned #18 |
| Signature reminder | One reminder if the contract is still unsigned after 7 days; the deadline (#18) stays as it is. | map and original: timer "7 days" (`event_signatureReminder`, P7D) → `serviceTask_sendReminderMail` → `endEvent_prospectReminded` | – | planned #1 |

## Insurance and bike order

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| Insurance policy | The policy that accompanies the leasing contract. The insurer issues it in parallel with the bike order; its id is stored on the application. | `gateway_fork` → "Issue insurance policy" (`serviceTask_issueInsurancePolicy`, `#{issueInsurancePolicyDelegate}`) → `gateway_join` | `IssueInsurancePolicyDelegate`, `IssueInsurancePolicyUseCase`, `InsurancePort.issuePolicy` → `PolicyId` (`POLICY-<uuid>`), `LeasingApplication.documentPolicy` | core (delivered #2) |
| Bike | The bike an application is about: its bike id and, from the catalogue, its model name. | variables `bikeId`, `bikeModel` (start event) | `BikeId`, `Bike`, `LeasingApplication.bikeId`, `LeasingApplication.bikeModel` | core |
| Bike catalogue | The bikes a customer can choose. A fixed list in the simulated dealer: `BIKE-900` "Gravel Explorer 900", `BIKE-800` "Carbon Road 800", `BIKE-600` "Mountain Trail 600", `BIKE-FAIL` (outage demo). Live availability per bike is planned. | – (no BPMN element) | `GET /api/bikes`, `ListBikesQuery`, `BikeDealerPort.catalogue` | core; availability planned #7 |
| Order bike from dealer | Order the bike at the dealer. The order id is stored and the status becomes ORDERED. | `serviceTask_orderBike` (`#{orderBikeDelegate}`, asyncBefore) | `OrderBikeDelegate`, `OrderBikeUseCase`, `BikeDealerPort.order` → `OrderId` (`ORDER-<uuid>`), `LeasingApplication.documentOrder` | core |
| Dealer outage | The dealer system cannot be reached while ordering. Demo bike: `BIKE-FAIL`. The order step retries three times, 10 seconds apart, then raises an incident. | `serviceTask_orderBike` (asyncBefore, `failedJobRetryTimeCycle` R3/PT10S) | `BikeDealerAdapter.DealerUnavailableException`; `DealerOutageIncidentTest` | core |
| Bike unavailable / Clarify alternative with customer | The dealer cannot deliver the bike; the back office agrees another bike with the customer, which is then ordered. Without an alternative the application is reversed. | map and original: `event_bikeUnavailable` (or `gateway_bikeAvailable`), `userTask_clarifyAlternative`, `gateway_alternativeFound`, `gateway_bikeSourceJoin`, `event_triggerReversal`, `serviceTask_confirmContractCancellation`, `endEvent_contractCancelled` | – | planned #20 |

## Handover and activation

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| Handover | The dealer hands the bike to the customer and reports it. Accepted only after policy and order are both done; earlier it is refused (409). | "Handover reported" (`event_handoverReported`); `Message_HandoverReported` / `miravelo.handoverReported` | `POST /api/bike-leasing/{id}/report-handover`, `ReportHandoverUseCase`; frontend feature `report-handover` | core |
| Leasing active | The lease is live: the end of the happy path. The handover sets it at once. | none end event "Leasing active" (`endEvent_leasingActive`) | `LeasingApplication.activate` (called by `ReportHandoverService`) | core |
| Withdrawal period | 14 days after the handover before the lease becomes active; the customer can still withdraw. | map and original: timer "Await end of withdrawal period" (`event_withdrawalPeriodElapsed`, P14D) | – | planned #21 |

## Withdrawal and reversal

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| Withdrawal | The customer withdraws a running application. Allowed while the status is RECEIVED, CONTRACT_SENT, CONTRACT_SIGNED or ORDERED; otherwise refused (409). It interrupts whatever runs and sets WITHDRAWN. | interrupting event sub-process "Application withdrawn" (`subProcess_applicationWithdrawn`), start `startEvent_applicationWithdrawn`; `Message_ApplicationWithdrawn` / `miravelo.applicationWithdrawn` | `POST /api/bike-leasing/{id}/withdraw`, `WithdrawApplicationUseCase`, `LeasingApplication.withdraw`; frontend feature `withdraw-application` | core |
| Reverse application | The back office undoes what was done for a withdrawn application (contract, policy, order) outside the system and confirms it. The case ends cancelled. | user task `userTask_reverseApplication` → "Application cancelled" (`endEvent_applicationCancelled`) | `POST /api/bike-leasing/{id}/reversal`, `CompleteReversalUseCase`, `LeasingApplication.cancel`, inbox task type `REVERSAL`; frontend feature `reverse-application` | core |
| Automatic reversal | The process undoes every completed step itself (BPMN compensation: cancel contract, cancel policy) and sends a cancellation confirmation; replaces "Reverse application". | map: compensation throw `event_reverseApplication` → `serviceTask_sendCancellationConfirmation`; original also: `serviceTask_cancelContract`, `serviceTask_cancelPolicy` | – | planned #3 |
| Bike order cancellation | Cancel a placed order at the dealer when the customer withdraws after the bike was ordered, including the return of the bike and the cancellation costs. | map and original: `callActivity_cancelBikeOrder` (process `cancelBikeOrder`: `serviceTask_requestCancellation`, `gateway_cancellationPossible`, `userTask_clarifyReturn`, `serviceTask_bookCosts`, `endEvent_orderReversed`) | – | planned #4 |

## Back office

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| Inbox | The back office's list of open work: every open "Check credit rating" and "Reverse application" task with its application (customer, income, bike, waiting since). It carries no engine task id; a task is completed through the domain, by application id. | `userTask_checkCreditRating`, `userTask_reverseApplication` | `GET /api/tasks`, `ListOpenTasksQuery`, `OpenTaskDto`, `BackOfficeTaskType` (`CREDIT_CHECK`, `REVERSAL`); frontend `/aufgaben` | core (delivered #5, decided #11) |

## Statuses

The status is what `GET /api/bike-leasing/{id}` returns. It lives in the domain (`LeasingStatus`), not
in the engine; the services set it when the process reaches the step.

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| RECEIVED | Submitted. Stays so through the validation and the credit check. | set before `startEvent_leasingRequestReceived` | `LeasingStatus.RECEIVED` (`LeasingApplication.receive`) | core |
| CONTRACT_SENT | The contract is out; the customer's signature is awaited. | `serviceTask_sendContract` | `LeasingStatus.CONTRACT_SENT` (`sendContract`) | core |
| CONTRACT_SIGNED | The customer signed; the policy is issued and the bike ordered. | `event_contractSigned` | `LeasingStatus.CONTRACT_SIGNED` (`signContract`) | core |
| ORDERED | The dealer accepted the bike order; the handover is awaited. | `serviceTask_orderBike` | `LeasingStatus.ORDERED` (`documentOrder`) | core |
| ACTIVE | Final: the lease is live. | `event_handoverReported` → `endEvent_leasingActive` | `LeasingStatus.ACTIVE` (`activate`) | core |
| REJECTED | Final: invalid, or not solvent. | `serviceTask_sendRejection` → `endEvent_applicationRejected` | `LeasingStatus.REJECTED` (`reject`) | core |
| WITHDRAWN | The customer withdrew; the reversal is still open in the inbox. Ends as CANCELLED. | `startEvent_applicationWithdrawn` | `LeasingStatus.WITHDRAWN` (`withdraw`) | core |
| CANCELLED | Final: withdrawn and reversed. | `userTask_reverseApplication` → `endEvent_applicationCancelled` | `LeasingStatus.CANCELLED` (`cancel`) | core |
| HANDED_OVER | The customer has the bike and the withdrawal period runs. | original: `event_handoverReported` | – (not in `LeasingStatus`) | planned #21 |

## Technical terms you meet in the demo

| Term | Definition | In the models | In the code | Status |
|---|---|---|---|---|
| BPMN | Business Process Model and Notation: the standard diagram language for processes. The file designIQ shows is the file the engine runs. | `bike-leasing.bpmn` (process `bikeLeasingProcess`) | generated `BikeLeasingProcessProcessApi` (bpmn-to-code) | core |
| DMN | Decision Model and Notation: business rules as a decision table. Not deployed in the core; the frozen credit decision waits for #19. | `original/checkCreditRating.dmn` (decision `checkCreditRating`) | – | planned #19 |
| Process engine | CIB seven, the community fork of Camunda 7, embedded in the bike-leasing service. It runs the BPMN model and remembers where every case waits. | runs `bikeLeasingProcess` | `LeasingProcessAdapter`; the delegates in `adapter/inbound/cibseven`; `/engine-rest` | core |
| Process instance | One run of the process for one leasing application, from "Leasing request received" to an end event. | an instance of `bikeLeasingProcess` | `LeasingProcess.submitRequest` starts it | core |
| Business key | The application id attached to a process instance. Every message and task completion finds the right instance through it. | – | `LeasingProcessAdapter`, `correlateByBusinessKey`, `execution.processBusinessKey` in the delegates | core |
| Message | A named signal from outside that moves a waiting instance on, for example the signature. If the state does not allow it, the call is refused (409 "Action not available in the current state"). | `Message_*`, names `miravelo.*` | `Messages` (generated), `ActionNotAvailableException`, `GlobalExceptionConfiguration` | core |
| Event sub-process | A part of the process that can start at any time while the instance runs, triggered by an event. "Application withdrawn" is interrupting: it stops the main flow. | `subProcess_applicationWithdrawn` (`triggeredByEvent`) | – | core |
| User task | A step a person does; the instance waits until it is completed. The core has two, both in the inbox. | `userTask_checkCreditRating`, `userTask_reverseApplication` | `LeasingProcess.completeCreditCheck`, `LeasingProcess.completeReversal` | core |
| Incident | A step failed and used up its retries. The case stops there until Operations fixes the cause and retries it in the Cockpit. | `serviceTask_orderBike` (R3/PT10S) | `DealerOutageIncidentTest` | core |
| Cockpit / Tasklist | CIB seven webclient at `/webapp/#/seven/auth/start` (admin/admin), for operations; the classic web apps still run at `/camunda`. Cockpit shows running cases and incidents. The Tasklist would show the user tasks too, but the back office does not use it: completing a task there would bypass the domain (ADR-0019). | – | `application.yaml`; the frontend links the Cockpit | core |
| Frontend | The React app in `frontend/` (ADR-0018): application list, request form, detail page with the customer and dealer actions, and the inbox. Its API client is generated from `openapi/openapi.json`. | – | `frontend/src/app/routes/` (`/`, `/antraege/neu`, `/antraege/{id}`, `/aufgaben`) | core |
| Simulated system | An external system the core only fakes, so the demo needs no partners: contract system (#8), notification channel (#9), dealer system (#10), insurer. | – | `LoggingContractAdapter`, `LoggingNotificationAdapter`, `BikeDealerAdapter`, `LoggingInsuranceAdapter` | core |
| Bruno scenario | A scripted series of REST calls that plays customer, dealer and back office against the running service. `00-demo-data` prepares cases for a live demo. | – | `bruno/` (`00-demo-data` … `06-incident-demo`) | core |
| Reference model | The MiraVelo models Miragon maintains in Miragon/miravelo-reference. The executable process is its stage `03-bike-leasing-event-subprocess-simplified.bpmn` (ADR-0017). | `bike-leasing.bpmn` | – | core |
| t.BPM | Tangible BPM: modelling a process together at a table with a reduced BPMN palette (activity, event, gateway, document). The table-top model is the shared language. | `bike-leasing-tbpm.bpmn` (workshop mode) | – | core |
| Feature map | The t.BPM model plus the planned features, in stages: Core (runs today), Stage 1 – Customer follow-up, Stage 2 – Automation and supply, Stage 3 – Withdrawal, Stage 4 – Portal. A group marks each feature that adds steps, and a note sticky names every feature. No feature loses its place in the process. | `bike-leasing-feature-map.bpmn`, `feature-map.md` | – | core |
| C4 model | Architecture diagrams at two zoom levels: system context (who and what talks to the service) and container (what runs inside it). | `c4-system-context.md`, `c4-container.md` (Mermaid: drawn on GitHub, raw text in designIQ) | – | core |
| Wardley Map | A map of the components behind a user need: up = visible to the user, right = more evolved (genesis to commodity). It grounds the make, buy or adapt decisions #8 to #11. | `sourcing-map.owm`, `sourcing-decisions.md` | – | core |
| designIQ | A Git-native workspace where teams model their business and architecture together, live. It shows every model under `docs/` from the default branch; edits reach Git through a release pull request. | `designiq.yml` (`models: docs`) at the repo root | – | core |
| Todo | A GitHub issue that designIQ pins to process elements: labels `todo` and `process:bike-leasing` plus an anchor block with element ids. | anchor lines `element: <id>`, e.g. `serviceTask_sendContract` (#1, #8, #18) | – | core |

## Issues referenced

Features (stage in the feature map):

- [#18 Signature deadline](https://github.com/miragon-blueprints/miragon-ai-example/issues/18) (Stage 1 – Customer follow-up)
- [#1 Signature reminder](https://github.com/miragon-blueprints/miragon-ai-example/issues/1) (Stage 1 – Customer follow-up, needs #18)
- [#19 Credit check by DMN](https://github.com/miragon-blueprints/miragon-ai-example/issues/19) (Stage 2 – Automation and supply)
- [#20 Alternative bike](https://github.com/miragon-blueprints/miragon-ai-example/issues/20) (Stage 2 – Automation and supply)
- [#21 Withdrawal period](https://github.com/miragon-blueprints/miragon-ai-example/issues/21) (Stage 3 – Withdrawal)
- [#3 Automatic reversal](https://github.com/miragon-blueprints/miragon-ai-example/issues/3) (Stage 3 – Withdrawal)
- [#4 Bike order cancellation](https://github.com/miragon-blueprints/miragon-ai-example/issues/4) (Stage 3 – Withdrawal, needs #3)
- [#7 Live dealer availability](https://github.com/miragon-blueprints/miragon-ai-example/issues/7) (Stage 4 – Portal)

Delivered by the rebase onto the new core: [#2 Insurance policy](https://github.com/miragon-blueprints/miragon-ai-example/issues/2),
[#5 Back-office inbox](https://github.com/miragon-blueprints/miragon-ai-example/issues/5),
[#6 Application list](https://github.com/miragon-blueprints/miragon-ai-example/issues/6).

Decisions (make, buy or adapt):

- [#8 Contract & e-signature](https://github.com/miragon-blueprints/miragon-ai-example/issues/8)
- [#9 Customer notification](https://github.com/miragon-blueprints/miragon-ai-example/issues/9)
- [#10 Dealer integration](https://github.com/miragon-blueprints/miragon-ai-example/issues/10)
- [#11 Back-office task UI](https://github.com/miragon-blueprints/miragon-ai-example/issues/11) (decided by ADR-0019)
