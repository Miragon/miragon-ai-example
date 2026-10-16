# Sourcing decisions: make, buy or adapt

The Wardley Map [sourcing-map.owm](sourcing-map.owm) places every real part of the bike-leasing example
on one map. This file gives the reason for each position and decision, ties each component to its BPMN
elements and its code, and lists the three open decisions #8 to #10.

In designIQ, open the model `sourcing-map` to see the map drawn. GitHub shows it as plain text; the
picture is [sourcing-map.png](../assets/sourcing-map.png).

The rule from the talk: build what sets us apart, buy what is standard, and adapt on purpose in between.
Adapt is an option of its own, not a compromise.


## How to read the map

- Up means visible to the anchors (Customer, Back office, Operations). Down is hidden plumbing.
  Visibility is not importance: the database is at the bottom, yet nothing works without it.
- Right means more evolved: genesis, custom-built, product, commodity.
- Decorators: `build` = Make, `buy` = Buy a product, `outsource` = Buy as a service,
  `market` = Partner. No decorator plus a numbered "Open" annotation = open decision.
- Adapt has no decorator of its own. It is `buy` plus an "Adapt" annotation (talk convention). No
  component is adapted today. Until [ADR-0019][adr19] the CIB seven Tasklist with our own form was
  the Adapt option for the back-office task UI; the own inbox won.
- Open source counts as Buy: we take a finished product as it is. The cost is upgrades and operation,
  not a licence.
- Annotation 4 is no sourcing decision: it marks that the credit check is a human decision today and
  that feature [#19][i19] automates it.
- The map shows only parts that exist in this repository. The talk's map also had payments, a DMS and
  a credit bureau. This example has none of them, so they are not on the map.

Stage boundaries differ between tools. designIQ's model view counts evolution below 0.25 as genesis,
below 0.50 as custom-built, below 0.75 as product, the rest as commodity. The drawing uses 0.17, 0.40
and 0.70. Every component sits where both agree: custom-built 0.25 to 0.40, product 0.50 to 0.70,
commodity from 0.75. Keep it that way when you move a component.


## The five decision criteria (from the talk)

1. Differentiation: does it set MiraVelo apart for the customer?
2. Maturity / evolution: where does it sit on the horizontal axis of the map?
3. Strategic relevance: what breaks if it fails?
4. Market availability: can we get it as a product or service, from more than one provider?
5. Integration effort: what does it cost to fit it behind our ports and the process?

Left on the map and differentiating: Make. Right on the map and available: Buy. A product that almost
fits: Adapt.


## Sourcing table

Map position = evolution value on the map and its stage.

| Component | Map position (evolution stage) | Decision | Rationale | Where in the process (BPMN element ids) | Where in the code | Issue / ADR |
|---|---|---|---|---|---|---|
| Bike leasing | 0.55, product (Assumption) | – (the user need) | What the customer wants: a bike, not an application form. | the whole process `bikeLeasingProcess` | – | [use case][usecase] |
| Customer frontend | 0.26, custom-built | **Make** | The customer's view of the case: apply, follow the status, sign, withdraw. Built from known parts (React, Vite, TanStack) and a client generated from our contract, so not genesis. | `startEvent_leasingRequestReceived`, `event_contractSigned`, `startEvent_applicationWithdrawn` (and `event_handoverReported`, which the dealer would report) | [frontend/][fe]: pages `application-list`, `submit-application`, `application-detail` | [ADR-0018][adr18] |
| Back-office task UI | 0.36, custom-built | **Make**: own inbox in the frontend (decided) | Task data goes through the domain and its validation, and the inbox shows business data (customer, income, bike) instead of process variables. Rejected: adapting the CIB seven Tasklist with a Camunda Form, which writes variables past the domain. Trade-off: no assignment, claiming or permissions yet. | `userTask_checkCreditRating`, `userTask_reverseApplication` | page `task-inbox`; `GET /api/tasks` (`ListOpenTasksController`), `CompleteCreditCheckController`, `CompleteReversalController` | [ADR-0019][adr19], decides [#11][i11] |
| Process monitoring | 0.66, product | **Buy**: CIB seven Cockpit | Ships with the engine. Operations sees running cases and incidents without any code of ours. | incidents on `serviceTask_orderBike` (dealer outage); every running case | `cibseven-bpm-spring-boot-starter-webapp-4`, served at `/camunda` | [ADR-0009][adr9] (technical metrics) |
| Leasing application | 0.30, custom-built | **Make** | MiraVelo's own domain and use cases; nobody sells them. Built from known patterns (hexagon, Spring Boot), so not genesis. | every service task (via `#{...Delegate}`); the message events and both user tasks (via REST) | `domain/`, `application/`, `adapter/inbound/` under [io/miragon/blueprint][code] | [ADR-0002][adr2], [ADR-0003][adr3] |
| Insurance policy | 0.77, commodity (Assumption) | **Partner** (the insurer) (Assumption); no decision issue yet | Insuring a leased bike is a standard product that an insurer sells; we never build it. How to connect the insurer has no issue of its own yet. | `serviceTask_issueInsurancePolicy` | `InsurancePort`, `LoggingInsuranceAdapter` (simulated) | [ADR-0017][adr17] |
| Customer notification | 0.86, commodity (Assumption) | **Open** (#9) | The delivery sets nobody apart (the content may). Transactional messaging is a utility. | `serviceTask_sendContract`, `serviceTask_sendRejection` | `NotificationPort`, `LoggingNotificationAdapter` (simulated) | [#9][i9] |
| Contract & e-signature | 0.68, product (Assumption) | **Open** (#8) | Does not set us apart, but no signature means no lease. E-signature is regulated and sold as a product or service. | `serviceTask_sendContract`, `event_contractSigned` | `ContractPort`, `LoggingContractAdapter` (simulated) | [#8][i8] |
| Dealer integration | 0.38, custom-built (Assumption) | **Partner** (the dealer); how to connect is **open** (#10) | The dealer is an independent business: we never build its system. Dealer interfaces are probably uneven (APIs, portals). | `serviceTask_orderBike`, `event_handoverReported` | `BikeDealerPort`, `BikeDealerAdapter` (simulated); `ReportHandoverController` | [#10][i10] |
| Credit check | 0.30, custom-built | **Make**; manual today, automation is [#19][i19] | MiraVelo's own risk policy. Today a person decides it in the inbox, with no rule behind the decision. #19 brings back the original decision table `checkCreditRating` (age 18 to 99, monthly net income at least 1,500), which waits frozen in `original/`. | `userTask_checkCreditRating`, read by `gateway_isSolvent` | `CompleteCreditCheckService`; [checkCreditRating.dmn][dmn] (not deployed) | [#19][i19] |
| Process engine | 0.64, product | **Buy**: CIB seven (open source, embedded) | Running BPMN with messages, an event subprocess, async jobs and retries is a mature product. Building it would set us apart in nothing. | the waiting and the technical behaviour: `event_contractSigned`, `event_handoverReported`, `subProcess_applicationWithdrawn`, retries on `serviceTask_orderBike` | `cibseven-bpm-spring-boot-starter-rest-4` and `-webapp-4` ([versions][libs]); port `LeasingProcess`, adapter `LeasingProcessAdapter` | no ADR yet; [ADR-0008][adr8] |
| Leasing process model | 0.27, custom-built | **Make** | How MiraVelo runs a lease. It grows with every feature. BPMN itself is a standard, and the model is taken from Miragon's reference models, so not genesis. | all of `bikeLeasingProcess` ([bike-leasing.bpmn][bpmn]) | generated `adapter/process/BikeLeasingProcessProcessApi.kt`; packaged by [build.gradle.kts][gradle] | [ADR-0017][adr17], [ADR-0016][adr16] |
| UI framework | 0.76, commodity | **Buy**: React, Vite and their ecosystem (open source) | The standard way to build a web frontend. We use it as it is. | none; it hosts the pages that start and complete the steps | [frontend/package.json][fe-package] | [ADR-0018][adr18] |
| Application framework | 0.82, commodity | **Buy**: Spring Boot 4 (open source) | The standard way to run a JVM service. We use it as it is. | none; it hosts the delegate beans the service tasks call | [versions][libs], [build.gradle.kts][gradle] | [ADR-0008][adr8], [ADR-0014][adr14] |
| Database | 0.90, commodity | **Buy as a service** (outsource): PostgreSQL (Assumption) | A standard database sets nothing apart. The service reads its datasource from `SPRING_DATASOURCE_*`, so a managed PostgreSQL needs no code change. | none; it holds the engine state of every case and the `leasing_application` table | [docker-compose.yml][compose] (local only); [Flyway migrations][migrations]; `LeasingApplicationPersistenceAdapter` | [ADR-0010][adr10], [ADR-0011][adr11] |


## Open decisions

Three parts are still open. Each is a GitHub issue with options, the five criteria and acceptance
criteria. The number in the first column is the annotation number on the map.

designIQ cannot link a BPMN element to a Wardley component. It resolves cross-model links only for
call activities and business rule tasks. So the table below is the documented mapping from Wardley
component to BPMN element ids.

The same issues are designIQ todos on those elements. Each issue has the labels `todo` and
`process:bike-leasing`, and an anchor block that names these element ids. While the issue is open,
designIQ pins it on the elements of `bike-leasing.bpmn`.

| Map | Issue | Wardley component | Question | Anchored BPMN element ids | Today | Related |
|---|---|---|---|---|---|---|
| 1 | [#8][i8] | Contract & e-signature | Contract issuing and e-signature: make, buy or adapt? | `serviceTask_sendContract`, `event_contractSigned` | simulated: `LoggingContractAdapter`; the customer signs with a button in the frontend | #1, #3, #9, #18 |
| 2 | [#9][i9] | Customer notification | Which channel, and how is it sourced? | `serviceTask_sendContract`, `serviceTask_sendRejection` | simulated: `LoggingNotificationAdapter` | #1, #3, #8 |
| 3 | [#10][i10] | Dealer integration | How do we connect the dealer? | `serviceTask_orderBike`, `event_handoverReported` | simulated: `BikeDealerAdapter`; the handover is reported with a button in the frontend | #4, #7, #20 |

Decided: [#11][i11] Back-office task UI, build the own inbox ([ADR-0019][adr19]). The issue closes
with the merge of the rebase; its pins then disappear from the model.


## Assumptions

1. Evolution positions are estimates for this example, not market research. The map says so in a note.
2. Bike leasing sits at product: leasing a bike is an established offer with several providers, and
   MiraVelo competes on how well it runs the case.
3. Contract & e-signature sits at late product, moving towards commodity (first hypothesis in #8).
   Which level of electronic signature a leasing contract needs is for legal to confirm (#8).
4. Customer notification sits at commodity: transactional messaging is available as a utility
   (first hypothesis in #9).
5. Dealer integration sits at custom-built: some dealers offer an API, some only a portal (first
   hypothesis in #10). The domain knows no dealer id, so the core implicitly has one dealer.
6. Insurance policy sits at commodity, and the insurer is a partner. No issue asks how to connect
   the insurer yet; open one, like #8 to #10, before the simulated adapter is replaced.
7. Database as a service: production hosting offers a managed PostgreSQL. This repository decides
   no hosting; [ADR-0011][adr11] covers the container image and the overridable datasource, not where
   the database runs. Locally, PostgreSQL runs in Docker.
8. The credit check uses only what the customer states (age, monthly net income), today by a person
   and with #19 by the decision table. No credit bureau is asked. A bureau check would be a new
   component, so it is not on the map.


## When a decision is taken

1. Record it as an ADR in [`docs/adr/`](../adr/): copy [0000-adr-template.md][adr0], take the next
   free number, status Accepted. Cover the five criteria and the options you rejected.
2. Update the map in [sourcing-map.owm](sourcing-map.owm):
   - set the decorator of the component (`build`, `buy`, `outsource` or `market`);
   - replace the "Open: ..." annotation, for example with "Adapt: ...", or delete it;
   - point the component's `url` at the ADR instead of the issue.
3. Update the component's row in this file (Decision, Rationale, Issue / ADR) and remove it from
   "Open decisions".
4. Validate the map: `npx --yes @miragon/design-iq-validator@5.3.0 --root . sourcing-map`, and
   refresh [sourcing-map.png](../assets/sourcing-map.png).
5. Close the issue with a link to the ADR. Only open issues are todos, so its pins disappear from the
   BPMN elements in designIQ. For Buy or Adapt, open the follow-up implementation issue that the
   acceptance criteria of #8 to #10 ask for.

#11 went this way: [ADR-0019][adr19], the component "Back-office task UI" got `build` and a link to
the ADR, and the Tasklist variant left the map.

Edit a model either in designIQ (it reaches Git through a release pull request) or in Git, not in
both at the same time. A file changed on both sides becomes a conflict in designIQ.


## Related

- [Glossary](../glossary.md) for every term above.
- [C4 system context](c4-system-context.md) and [C4 container view](c4-container.md): the same parts
  as boxes. A Make decision in #8 to #10 pulls a capability into the service; Buy or Adapt makes a
  simulated box a real system.
- [Use case](../use-cases/bike-leasing/bike-leasing-use-case.md) and
  [feature map](../use-cases/bike-leasing/feature-map/feature-map.md) for the feature issues.

[usecase]: ../use-cases/bike-leasing/bike-leasing-use-case.md
[bpmn]: ../use-cases/bike-leasing/process/bike-leasing.bpmn
[dmn]: ../use-cases/bike-leasing/original/checkCreditRating.dmn
[code]: ../../src/main/kotlin/io/miragon/blueprint/
[fe]: ../../frontend/AGENTS.md
[fe-package]: ../../frontend/package.json
[migrations]: ../../src/main/resources/db/migration/
[compose]: ../../stack/docker-compose.yml
[gradle]: ../../build.gradle.kts
[libs]: ../../gradle/libs.versions.toml
[adr0]: ../adr/0000-adr-template.md
[adr2]: ../adr/0002-hexagonal-architecture-for-the-backend.md
[adr3]: ../adr/0003-openapi-as-the-checked-in-contract.md
[adr8]: ../adr/0008-track-the-latest-major-versions.md
[adr9]: ../adr/0009-actuator-probes-and-prometheus-metrics.md
[adr10]: ../adr/0010-flyway-for-database-migrations.md
[adr11]: ../adr/0011-build-and-deployment-approach.md
[adr14]: ../adr/0014-one-kotlin-gradle-stack-at-the-repo-root.md
[adr16]: ../adr/0016-docs-is-the-designiq-workspace.md
[adr17]: ../adr/0017-rebase-onto-the-simplified-event-subprocess-process.md
[adr18]: ../adr/0018-react-frontend-next-to-the-service.md
[adr19]: ../adr/0019-back-office-task-ui-own-inbox-in-the-frontend.md
[i8]: https://github.com/miragon-blueprints/miragon-ai-example/issues/8
[i9]: https://github.com/miragon-blueprints/miragon-ai-example/issues/9
[i10]: https://github.com/miragon-blueprints/miragon-ai-example/issues/10
[i11]: https://github.com/miragon-blueprints/miragon-ai-example/issues/11
[i19]: https://github.com/miragon-blueprints/miragon-ai-example/issues/19
