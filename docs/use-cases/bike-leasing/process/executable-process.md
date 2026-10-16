# Executable process: bike leasing

For developers and the demo: where the model comes from, what the engine runs, how it gets there,
which element does what, and how to change the model without breaking the service. The business
view is in [bike-leasing-use-case.md](../bike-leasing-use-case.md).

## Provenance

The executable model is a copy of the simplified event-subprocess variant of the MiraVelo process
that Miragon maintains (the source model): 20 flow nodes, two user tasks (`Check credit rating`,
`Reverse application`), an interrupting event sub-process for the withdrawal, and no DMN, timer,
compensation or called process. The flow-node ids of the source model are kept, except the error
boundary and "Contract signed". The copy here differs only where the engine or this repo's gates require it, and every
difference is listed in "Deviations from the source model"; add a row when you add one.

The original model of this repo is not the source model. It is frozen in `../original/` as the
target picture for the roadmap; [scope-comparison.md](../original/scope-comparison.md) maps each of
its capabilities to the core or to a feature issue.

## What is deployed

| File | Contains | Lives in |
|---|---|---|
| [bike-leasing.bpmn](bike-leasing.bpmn) | process `bikeLeasingProcess` "MiraVelo Bike Leasing" (history kept 180 days) | this folder |

Nothing else is deployed. The decision `checkCreditRating.dmn` lies in `../original/` as a frozen
reference until [#19] brings it back. The original models, the t.BPM model and the feature map are
documentation only. The user tasks carry no Camunda Form: the back office completes them through
the domain API in the frontend inbox
([ADR-0010](../../../adr/0010-back-office-task-ui-own-inbox-in-the-frontend.md)). Why the executable
model lives under `docs/`: [ADR-0008](../../../adr/0008-docs-is-the-designiq-workspace.md).

## How it gets into the engine

1. Packaging: Gradle `processResources` copies this folder's `*.bpmn` to `bpmn/` (and any `*.dmn` to
   `dmn/`) on the classpath ([build.gradle.kts](../../../../service/app/build.gradle.kts)).
2. Deployment: at start-up the embedded CIB seven engine deploys `classpath*:**/*.bpmn` and `*.dmn`
   (`camunda.bpm.deployment-resource-pattern` in `application.yaml`). A changed model becomes a new
   process version; running cases stay on the version they started with.
3. Generated API: bpmn-to-code (`./gradlew generateBpmnModels`, also run before every compile)
   reads `docs/use-cases/bike-leasing/process/*.bpmn` and writes the typed process API to
   `service/app/src/main/kotlin/io/miragon/blueprint/adapter/process/` (`BikeLeasingProcessProcessApi`,
   `ServiceTasks`, `Messages`, `ProcessVariables`, `Errors`). Delegates, adapters and tests use these
   constants, so a renamed or removed id breaks the compile.
4. Gates on every pull request ([pre-merge.yml](../../../../.github/workflows/pre-merge.yml)):
   bpmnlint on this folder; `./gradlew build` (model validation, process tests, unit and
   architecture tests, OpenAPI drift); mutation testing; the Bruno scenarios and the Playwright
   end-to-end tests against the running service. A designIQ release opens a pull request, so a model
   change made in designIQ passes the same gates before it reaches main.

The process tests (`service/app/src/test/kotlin/io/miragon/blueprint/adapter/process/`) cover every path:
happy path, invalid application, not solvent, withdrawal during the credit check, while the
signature is awaited and after the order, and the dealer-outage incident. `InvalidApplicationTransactionTest`
shows that an under-age applicant is rejected, not stuck in an incident.

## Elements

20 flow nodes and 18 sequence flows. "async" lists the transaction boundaries (`camunda:asyncBefore`
/ `camunda:asyncAfter`).

| id | type | name | implementation | async | note |
|---|---|---|---|---|---|
| startEvent_leasingRequestReceived | message start event | Leasing request received | message `miravelo.leasingRequestReceived` (`POST /api/bike-leasing`) | after | starts with applicationId (business key), bikeId, bikeModel; status RECEIVED |
| serviceTask_validateApplication | service task | Validate application | delegate `#{validateApplicationDelegate}` | after | **Assumption:** valid = age ≥ 18 and bike in the dealer catalogue; otherwise throws `applicationInvalid` |
| event_applicationInvalid | error boundary event (interrupting) | Application invalid | error `applicationInvalid` (`Error_ApplicationInvalid`) | – | on serviceTask_validateApplication → flow_applicationInvalid |
| userTask_checkCreditRating | user task | Check credit rating | completed by `POST /api/bike-leasing/{id}/credit-check` (`{ "solvent": … }`) | after | output `solvent`; inbox task type `CREDIT_CHECK`; no form |
| gateway_isSolvent | exclusive gateway | Solvent? | `${!solvent}` → "No" (`flow_notSolvent`) | – | default `flow_solvent` → Send contract |
| serviceTask_sendContract | service task | Send contract | delegate `#{sendContractDelegate}` | – | issues the contract (ContractPort), asks the customer to sign; status CONTRACT_SENT |
| receiveTask_contractSigned | receive task | Contract signed | message `miravelo.contractSigned` (`POST /api/bike-leasing/{id}/sign-contract`) | after | no time limit ([#18]); status CONTRACT_SIGNED; a task, so the timers of [#1] and [#18] can attach as boundary events |
| gateway_fork | parallel gateway (split) | (unnamed) | | – | insurance and order in parallel |
| serviceTask_issueInsurancePolicy | service task | Issue insurance policy | delegate `#{issueInsurancePolicyDelegate}` | – | InsurancePort; stores policyId |
| serviceTask_orderBike | service task | Order bike from dealer | delegate `#{orderBikeDelegate}` | before | retries `R3/PT10S`; stores orderId; status ORDERED; `BIKE-FAIL` → incident |
| gateway_join | parallel gateway (join) | (unnamed) | | – | the handover is accepted only after both branches |
| event_handoverReported | message catch event | Handover reported | message `miravelo.handoverReported` (`POST /api/bike-leasing/{id}/report-handover`) | after | the REST call sets ACTIVE |
| endEvent_leasingActive | none end event | Leasing active | | – | |
| gateway_rejectionJoin | exclusive gateway (merge) | Rejection reason | | – | joins "invalid" and "not solvent" |
| serviceTask_sendRejection | service task | Send rejection | delegate `#{sendRejectionDelegate}` | after | notifies the customer; status REJECTED |
| endEvent_applicationRejected | none end event | Application rejected | | – | |
| subProcess_applicationWithdrawn | event sub-process (interrupting) | Application withdrawn | | – | interrupts every wait state of the main flow |
| startEvent_applicationWithdrawn | message start event | Application withdrawn | message `miravelo.applicationWithdrawn` (`POST /api/bike-leasing/{id}/withdraw`) | after | the REST call sets WITHDRAWN; allowed from RECEIVED to ORDERED |
| userTask_reverseApplication | user task | Reverse application | completed by `POST /api/bike-leasing/{id}/reversal` | after | inbox task type `REVERSAL`; status CANCELLED; no form |
| endEvent_applicationCancelled | none end event | Application cancelled | | – | |

Sequence flows: `flow_requestToValidation`, `flow_validationToCreditCheck`,
`flow_creditCheckToSolvency`, `flow_solvent`, `flow_notSolvent` ("No"), `flow_contractSentToSigned`,
`flow_signedToFork`, `flow_forkToInsurance`, `flow_forkToOrder`, `flow_insuranceToJoin`,
`flow_orderToJoin`, `flow_joinToHandover`, `flow_handoverToActive`, `flow_applicationInvalid`,
`flow_rejectionJoined`, `flow_rejectionSent`, and inside the event sub-process
`flow_withdrawnToReversal`, `flow_reversalToCancelled`.

Process variables: applicationId (also the business key), bikeId, bikeModel, solvent. The
customer's name, email, age and income stay in the database (`leasing_application`) and never enter
the engine.

## Deviations from the source model

| What | Source model | Executable copy | Why |
|---|---|---|---|
| Order bike from dealer | external task, `camunda:type="external"`, topic `orderBike`; asyncAfter | delegate `#{orderBikeDelegate}`; asyncBefore, no asyncAfter; `failedJobRetryTimeCycle` `R3/PT10S` | the engine stays embedded and every service task follows the same pattern; the retry cycle drives the incident demo (`BIKE-FAIL`) |
| Send contract, Issue insurance policy | asyncAfter | no asyncAfter | CONTRACT_SENT commits together with the wait at Contract signed; the policy is issued inline before the join, so the order job sets ORDERED and reaches Handover reported in one transaction. The frontend never offers "sign" or "report handover" before the process waits for it (`StatusMatchesWaitStateTest`) |
| Error boundary on Validate application | `boundary_applicationInvalid` | `event_applicationInvalid` | `@miragon/rules/element-id-naming` (bpmnlint) |
| Contract signed | message catch event `event_contractSigned` | receive task `receiveTask_contractSigned`, same message | BPMN attaches boundary events only to activities, and the reminder of [#1] and the deadline of [#18] are timers on this wait; the id follows `@miragon/rules/element-id-naming` |
| Sequence flow ids | generated (`Flow_0cq39pq`, `E01`, …) | descriptive `flow_*` (list above) | the same bpmnlint rule; readable ids in tests and anchors |
| History time to live | `180` | `P180D` | the same 180 days, as an ISO 8601 duration |
| Rejection merge | `gateway_rejectionJoin` unnamed | named "Rejection reason" | the merge rule of the glossary; the name the issues use |
| Validation rule | not defined | age ≥ 18 and bike in the dealer catalogue | **Assumption**; the source model names the step only |

Everything else (flow-node ids and names, messages, the error, the variables) is as in the source model.

## t.BPM element ↔ executable element

The t.BPM model ([bike-leasing-tbpm.bpmn](../tbpm/bike-leasing-tbpm.bpmn)) is the business view
of the same core. It reuses the executable id for every element that has a counterpart here, so one
id finds the step in both models; the feature map uses the same ids. Ids that exist only in the
t.BPM model start with `tbpm_`. Shared steps carry no technical detail in the t.BPM model: tasks are
plain tasks, and "Contract signed" and "Handover reported" are plain intermediate events.

Where the two models differ:

| t.BPM element | t.BPM id | Executable element(s) |
|---|---|---|
| Application valid? (gateway, yes/no) | tbpm_gateway_applicationValid | the error boundary event_applicationInvalid on serviceTask_validateApplication ("no" is the BPMN error) |
| Leasing contract (document) | tbpm_dataObject_leasingContract | no element: the contract lives in the contract system, and its id is stored on the application |
| Customer, Back office, Dealer (role stickies) | tbpm_sticky_* | no element: the executable model has no lanes |

Flows that exist only in the t.BPM model are named `tbpm_flow_*`.

## designIQ todos on this model

Each issue carries the labels `todo` and `process:bike-leasing` and an anchor block
(`<!-- bpmiq:todo v1 ... -->`) that names this file and the element ids. designIQ shows open
issues as todos on those elements. If you rename or delete an anchored element, update the issue's
anchor block, or the todo loses its place on the canvas.

| Issue | Anchored elements (the `element:` lines of the issue body) |
|---|---|
| [#18] Signature deadline | receiveTask_contractSigned, gateway_rejectionJoin |
| [#1] Signature reminder | receiveTask_contractSigned |
| [#19] Credit check by DMN | userTask_checkCreditRating, gateway_isSolvent |
| [#20] Alternative bike | serviceTask_orderBike |
| [#21] Withdrawal period | event_handoverReported, endEvent_leasingActive |
| [#3] Automatic reversal | subProcess_applicationWithdrawn, userTask_reverseApplication, serviceTask_sendContract, serviceTask_issueInsurancePolicy |
| [#4] Bike order cancellation | serviceTask_orderBike |
| [#7] Live dealer availability | serviceTask_orderBike |
| [#8] Contract & e-signature | serviceTask_sendContract, receiveTask_contractSigned |
| [#9] Customer notification | serviceTask_sendContract, serviceTask_sendRejection |
| [#10] Dealer integration | serviceTask_orderBike, event_handoverReported |

Busiest elements: serviceTask_sendContract (#3, #8, #9), serviceTask_orderBike (#4, #7, #10,
#20), receiveTask_contractSigned (#1, #8, #18).

[#2], [#5], [#6] and [#11] are delivered or decided by the rebase and close with it; their anchor
blocks still name the elements of the previous core.

## designIQ validation

`npx --yes @miragon/design-iq-validator@5.3.0 --root . bike-leasing` (from the repo root) reports
0 errors and 0 warnings. Keep it that way: a change must add no finding.

The previous core had three known false positives from its compensation handler (`Cancel
contract`). The new core has no compensation, so they are gone. [#3] brings compensation back; its
handlers and boundary events will again be reported as "unreachable" and "dead end", which is
correct BPMN but not yet understood by the validator. That pull request records them here and in
AGENTS.md.

## How to change the process safely

1. Choose the editor.
   - designIQ: the diagram, together with others, live. A release opens a pull request.
   - Camunda Modeler: Camunda attributes (delegate expressions, async flags, retry cycle, timer
     durations). designIQ's canvas has no properties panel for these. After a designIQ edit, check
     in the pull request diff that the `camunda:*` attributes are still there.
2. Keep ids stable. The generated API, the delegates, the tests, the Bruno scenarios, the issue
   anchors, the t.BPM model and the feature map all use them. A new element follows the pattern in
   this file (`serviceTask_<verbObject>`, `event_…`, `flow_…`; #1, for example, brings back
   `serviceTask_sendReminderMail`), gets complete diagram information (designIQ needs a shape for
   every element), and, for a service task, a delegate expression `#{beanName}` backed by a delegate
   in `adapter/inbound/cibseven`. The model validation test enforces the delegate expression. A new
   user task also needs a domain endpoint and an inbox entry (ADR-0010), not a form.
3. Regenerate the API with `./gradlew generateBpmnModels` and commit the changed files in
   `adapter/process`. Never edit them by hand. A designIQ release pull request contains only the
   `.bpmn`. CI regenerates the API before compiling, so the tests see the new model, but the
   regenerated files must still be committed: check out the release branch, regenerate and push to
   the same pull request.
4. Verify: `npm run lint:bpmn`, then `./gradlew test --tests "io.miragon.blueprint.adapter.process.*"`, then
   `./gradlew build`. Run the Bruno scenarios and the Playwright tests against a running service.
   The designIQ validator must still report 0 errors.
5. When behaviour changes, update the process test paths (`ProcessPath`), the Bruno scenarios, the
   frontend, [bike-leasing-use-case.md](../bike-leasing-use-case.md), this file (elements and, if
   the source model is left further behind, the deviations), the glossary, the feature map and the
   affected issue anchors.

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
