# Executable process: bike leasing

For developers and the demo: what the engine runs, how it gets there, which element does what, and
how to change the model without breaking the service. The business view is in
[bike-leasing-use-case.md](../bike-leasing-use-case.md).

## What is deployed

| File | Contains | Lives in |
|---|---|---|
| [bike-leasing.bpmn](bike-leasing.bpmn) | process `bikeLeasingProcess` "MiraVelo Bike Leasing" (history kept 180 days) | this folder |
| [checkCreditRating.dmn](checkCreditRating.dmn) | decision `checkCreditRating`, hit policy FIRST | this folder |
| [clarify-alternative.form](../../../../src/main/resources/forms/clarify-alternative.form) | Camunda Form `clarifyAlternativeForm` for the user task | `src/main/resources/forms/` (designIQ does not show `.form` files) |

Nothing else is deployed. The original models in `../original/`, the t.BPM model and the feature
map are documentation only. Why the executable models live under `docs/`:
[ADR-0016](../../../adr/0016-docs-is-the-designiq-workspace.md).

## How it gets into the engine

1. Packaging: Gradle `processResources` copies this folder's `*.bpmn` to `bpmn/` and `*.dmn` to
   `dmn/` on the classpath ([build.gradle.kts](../../../../build.gradle.kts)).
2. Deployment: at start-up the embedded CIB seven engine deploys `classpath*:**/*.bpmn`, `*.dmn`
   and `*.form` (`camunda.bpm.deployment-resource-pattern` in `application.yaml`). A changed model
   becomes a new process version; running cases stay on the version they started with.
3. Generated API: bpmn-to-code (`./gradlew generateBpmnModels`, also run before every compile)
   reads `docs/use-cases/bike-leasing/process/*.bpmn` and writes the typed process API to
   `src/main/kotlin/io/miragon/blueprint/adapter/process/` (BikeLeasingProcessProcessApi, ServiceTasks,
   Messages, ProcessVariables, Errors). Delegates, adapters and tests use these constants, so a
   renamed or removed id breaks the compile.
4. Gates on every pull request ([pre-merge.yml](../../../../.github/workflows/pre-merge.yml)):
   bpmnlint on this folder, `./gradlew build` (model validation, process tests, unit and
   architecture tests), and the Bruno scenarios against the running service. A designIQ release
   opens a pull request, so a model change made in designIQ passes the same gates before it reaches
   main.

The process tests (`src/test/kotlin/io/miragon/blueprint/process/`) cover every path: happy path,
signature deadline, not solvent, alternative found, alternative declined, and the dealer-outage
incident.

## Elements

| id | type | name | implementation | note |
|---|---|---|---|---|
| startEvent_leasingRequestReceived | message start event | Leasing request received | message `miravelo.leasingRequestReceived` (`POST /api/bike-leasing`) | asyncAfter; starts with applicationId (business key), bikeId, age, monthlyNetIncome |
| businessRuleTask_checkCreditRating | business rule task | Check credit rating | DMN `checkCreditRating` → `solvent` | not solvent: age < 18, age > 99 or income < 1500 |
| gateway_isSolvent | exclusive gateway | Solvent? | `${!solvent}` → "No" | default flow → Send contract |
| serviceTask_sendContract | service task | Send contract | delegate `#{sendContractDelegate}` | issues the contract (ContractPort), asks the customer to sign; asyncAfter |
| event_compensateContract | compensation boundary event | Compensate contract | association → serviceTask_cancelContract | attached to serviceTask_sendContract |
| serviceTask_cancelContract | service task, compensation handler | Cancel contract | delegate `#{cancelContractDelegate}` | runs only after "Trigger reversal"; revokes the contract; asyncAfter |
| gateway_awaitSignature | event-based gateway | (unnamed) | | the signature races the 14-day timer |
| event_contractSigned | message catch event | Contract signed | message `miravelo.contractSigned` (`POST /{id}/sign-contract`) | asyncAfter |
| event_signatureDeadline | timer catch event | 14 days | timer `P14D` | leads to the rejection; the contract is not revoked ([#8]) |
| gateway_rejectionJoin | exclusive gateway (merge) | Rejection reason | | joins "not solvent" and "deadline passed" |
| serviceTask_sendRejection | service task | Send rejection | delegate `#{sendRejectionDelegate}` | notifies the customer; status REJECTED; asyncAfter |
| endEvent_applicationRejected | none end event | Application rejected | | |
| gateway_bikeSourceJoin | exclusive gateway (merge) | (unnamed) | | joins the first order and the re-order loop |
| serviceTask_orderBike | service task | Order bike from dealer | delegate `#{orderBikeDelegate}` | asyncBefore, retries `R3/PT10S`; reads bikeId, writes orderId; status ORDERED; BIKE-FAIL → incident |
| event_bikeUnavailable | error boundary event (interrupting) | Bike unavailable | error `bikeUnavailable` (Error_BikeUnavailable) | thrown by the delegate when the dealer has the bike out of stock (BIKE-OOS) |
| userTask_clarifyAlternative | user task | Clarify alternative with customer | form `clarifyAlternativeForm` (deployment binding); task listener `#{clarifyAlternativeTaskListener}` | completed in the Tasklist or by `POST /{id}/clarify-alternative`; writes alternativeFound, bikeId; asyncAfter |
| gateway_alternativeFound | exclusive gateway | Alternative found? | `${!alternativeFound}` → "No" | default "Yes" → re-order |
| event_triggerReversal | compensation throw event | Trigger reversal | | compensates every completed activity (today: Send contract) |
| serviceTask_confirmContractCancellation | service task | Send cancellation confirmation | delegate `#{sendCancellationConfirmationDelegate}` | notifies the customer; status CANCELLED; asyncAfter |
| endEvent_contractCancelled | none end event | Contract cancelled | | a terminate end in the original |
| event_handoverReported | message catch event | Handover reported | message `miravelo.handoverReported` (`POST /{id}/report-handover`) | status HANDED_OVER; asyncAfter |
| event_withdrawalPeriodElapsed | timer catch event | Await end of withdrawal period | timer `P14D` | |
| endEvent_leasingActive | message end event | Leasing active | message `miravelo.leasingActivated`, delegate `#{activateLeasingDelegate}` | status ACTIVE; nothing receives the message |

Process variables: applicationId (also the business key), age, monthlyNetIncome, bikeId (replaced
by an accepted alternative), solvent, orderId, alternativeFound. The customer's name and email stay
in the database (`leasing_application`) and never enter the engine.

## t.BPM element ↔ executable element

The t.BPM model ([bike-leasing-tbpm.bpmn](../tbpm/bike-leasing-tbpm.bpmn)) is the business view
of the same core. The rule: it reuses the executable id for every element that has a counterpart
here, so one id finds the step in both models. Ids that exist only in the t.BPM model start with
`tbpm_`. Shared steps carry no technical detail in the t.BPM model; for example "Check credit rating"
is a plain task there.

Where the two models differ:

| t.BPM element | t.BPM id | Executable element(s) |
|---|---|---|
| Signed within 14 days? (gateway, yes/no) | gateway_awaitSignature | gateway_awaitSignature (event-based) + event_contractSigned ("yes") + event_signatureDeadline ("no", timer P14D) |
| Bike available? (gateway, yes/no) | tbpm_gateway_bikeAvailable | the error boundary event_bikeUnavailable on serviceTask_orderBike ("no" is the BPMN error) |
| Leasing contract (document) | tbpm_dataObject_leasingContract | no element: the contract lives in the contract system, and its id is stored on the application |
| Cancel contract (task on the "no alternative" path) | serviceTask_cancelContract | compensation: event_triggerReversal → event_compensateContract → serviceTask_cancelContract |

Flows that exist only in the t.BPM model are named `tbpm_flow_*`.

## designIQ todos on this model

Each issue carries the labels `todo` and `process:bike-leasing` and an anchor block
(`<!-- bpmiq:todo v1 ... -->`) that names this file and the element ids. designIQ shows open
issues as todos on those elements. If you rename or delete an anchored element, update the issue's
anchor block, or the todo loses its place on the canvas.

| Issue | Anchored elements |
|---|---|
| [#1] Signature reminder | serviceTask_sendContract, gateway_awaitSignature |
| [#2] Insurance policy | event_contractSigned, serviceTask_orderBike |
| [#3] Withdrawal | event_handoverReported, event_withdrawalPeriodElapsed, serviceTask_cancelContract |
| [#4] Bike order cancellation | serviceTask_orderBike |
| [#5] Back-office inbox | userTask_clarifyAlternative |
| [#6] Application list | none (process-level todo) |
| [#7] Bike catalogue | serviceTask_orderBike, userTask_clarifyAlternative |
| [#8] Contract & e-signature | serviceTask_sendContract, event_contractSigned, serviceTask_cancelContract |
| [#9] Customer notification | serviceTask_sendContract, serviceTask_sendRejection, serviceTask_confirmContractCancellation |
| [#10] Dealer integration | serviceTask_orderBike, event_handoverReported |
| [#11] Back-office task UI | userTask_clarifyAlternative |

Busiest elements: serviceTask_orderBike (#2, #4, #7, #10), serviceTask_sendContract (#1, #8, #9),
userTask_clarifyAlternative (#5, #7, #11).

## Known designIQ validator findings

`npx --yes @miragon/design-iq-validator@5.3.0 --root . bike-leasing` (from the repo root) reports
3 errors. All three are false positives caused by the compensation handler (file path left out):

```
[ERROR] serviceTask_cancelContract is unreachable (no incoming flow, process bikeLeasingProcess)
[ERROR] serviceTask_cancelContract is a dead end (no outgoing flow, process bikeLeasingProcess)
[ERROR] event_compensateContract is a dead end (no outgoing flow, process bikeLeasingProcess)
```

A compensation handler is connected by an association, never by a sequence flow. That is correct
BPMN: the engine, bpmnlint and the model validation test accept it. The designIQ validator has no
exemption for compensation yet. What this means:

- Full saves through the designIQ MCP or REST API (`save_bpmn_xml`, `save_model_content`) are
  refused under the default `lint: "block"`, because these errors are already in the model. Pass
  `lint: "warn"`. `edit_process` refuses only a change that adds an error.
- Edits on the designIQ canvas and releases are not gated.
- `checkCreditRating` validates clean (0 errors, 0 warnings).

## How to change the process safely

1. Choose the editor.
   - designIQ: the diagram, together with others, live. A release opens a pull request.
   - Camunda Modeler: Camunda attributes (delegate expressions, async flags, retry cycle, timer
     durations, form refs). designIQ's canvas has no properties panel for these. After a designIQ
     edit, check in the pull request diff that the `camunda:*` attributes are still there.
2. Keep ids stable. The generated API, the delegates, the tests, the Bruno scenarios, the issue
   anchors and the t.BPM model all use them. A new element follows the pattern in this file
   (`serviceTask_<verbObject>`, `event_…`, `flow_…`; #1, for example, brings back
   `serviceTask_sendReminderMail`), gets complete diagram information (designIQ
   needs a shape for every element), and, for a service task, a delegate expression `#{beanName}`
   backed by a delegate in `adapter/inbound/cibseven`. The model validation test enforces the
   delegate expression.
3. Regenerate the API with `./gradlew generateBpmnModels` and commit the changed files in
   `adapter/process`. Never edit them by hand. A designIQ release pull request contains only the
   `.bpmn`. CI regenerates the API before compiling, so the tests see the new model, but the
   regenerated files must still be committed: check out the release branch, regenerate and push to
   the same pull request.
4. Verify: `npm run lint:bpmn`, then `./gradlew test --tests "io.miragon.blueprint.process.*"`, then
   `./gradlew build`. Run the Bruno scenarios against a running service. The designIQ validator should
   report exactly the 3 findings above.
5. When behaviour changes, update the process test paths (`ProcessPath`), the Bruno scenarios,
   [bike-leasing-use-case.md](../bike-leasing-use-case.md), this file and the affected issue anchors.

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
