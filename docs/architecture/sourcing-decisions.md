# Sourcing decisions: make, buy or adapt

The Wardley Map [sourcing-map.owm](sourcing-map.owm) places every real part of the bike-leasing example
on one map. This file gives the reason for each position and decision, ties each component to its BPMN
elements and its code, and lists the four open decisions #8 to #11.

In designIQ, open the model `sourcing-map` to see the map drawn. GitHub shows it as plain text.

The rule from the talk: build what sets us apart, buy what is standard, and adapt on purpose in between.
Adapt is an option of its own, not a compromise.


## How to read the map

- Up means visible to the anchors (Customer, Back office, Operations). Down is hidden plumbing.
  Visibility is not importance: the database is at the bottom, yet nothing works without it.
- Right means more evolved: genesis, custom-built, product, commodity.
- Decorators: `build` = Make, `buy` = Buy a product, `outsource` = Buy as a service,
  `market` = Partner. No decorator plus a numbered "Open" annotation = open decision.
- Adapt has no decorator of its own. It is `buy` plus an "Adapt" annotation (talk convention).
  Annotation 5 shows the one Adapt this example already has: the bought CIB seven Tasklist with our
  own form `clarify-alternative.form`. Whether that stays is #11.
- Open source counts as Buy: we take a finished product as it is. The cost is upgrades and operation,
  not a licence.
- Back-office task UI carries a pipeline: two variants of the same capability, an own inbox (`build`)
  and the CIB seven Tasklist (`buy`). Issue #11 picks one.
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
| Bike leasing | 0.55, product (Assumption) | – (the user need) | What the customer wants: a bike, not an application form. | the whole process `bikeLeasingProcess` | REST API `/api/bike-leasing` ([contract][openapi]) | [use case][usecase] |
| Leasing application | 0.30, custom-built | **Make** | MiraVelo's own domain and use cases; nobody sells them. Built from known patterns (hexagon, Spring Boot), so not genesis. | every service task (via `#{...Delegate}`); `startEvent_leasingRequestReceived`, `event_contractSigned`, `event_handoverReported` (via REST) | `domain/`, `application/`, `adapter/inbound/rest/`, `adapter/inbound/cibseven/` under [io/miragon/blueprint][code] | [ADR-0002][adr2], [ADR-0003][adr3] |
| Leasing process model | 0.26, custom-built | **Make** | How MiraVelo runs a lease. It grows with every feature (#1 to #7). BPMN itself is a standard, so not genesis. | all of `bikeLeasingProcess` ([bike-leasing.bpmn][bpmn]) | generated `adapter/process/BikeLeasingProcessProcessApi.kt`; packaged by [build.gradle.kts][gradle] | [ADR-0015][adr15], [ADR-0016][adr16] |
| Credit rating rules | 0.30, custom-built | **Make** | MiraVelo's own risk policy: age 18 to 99 and a monthly net income of at least 1,500. As DMN the business can read and test it. | `businessRuleTask_checkCreditRating`, read by `gateway_isSolvent` | [checkCreditRating.dmn][dmn]; `age` and `monthlyNetIncome` are passed in `LeasingProcessAdapter` | – |
| Process engine | 0.62, product | **Buy**: CIB seven (open source, embedded) | Running BPMN and DMN with timers, messages, compensation and retries is a mature product. Building it would set us apart in nothing. | the waiting and the technical behaviour: `gateway_awaitSignature`, timers `event_signatureDeadline` and `event_withdrawalPeriodElapsed`, compensation `event_triggerReversal`, retries on `serviceTask_orderBike` | `cibseven-bpm-spring-boot-starter-rest-4` and `-webapp-4` ([versions][libs]); port `LeasingProcess`, adapter `LeasingProcessAdapter` | no ADR yet; [ADR-0008][adr8] |
| Process monitoring | 0.66, product | **Buy**: CIB seven Cockpit | Ships with the engine. Operations sees running cases and incidents without any code of ours. | incidents on `serviceTask_orderBike` (dealer outage); every running case | `cibseven-bpm-spring-boot-starter-webapp-4`, served at `/camunda` | [ADR-0009][adr9] (technical metrics) |
| Back-office task UI | 0.60, product. Variants: own inbox 0.38, custom-built; CIB seven Tasklist 0.66, product | **Open** (#11). Today both ways work. | Adapt the shipped Tasklist (little effort, no UI code; today it shows our own form, annotation 5) or build an own inbox on the domain API (domain validation, tailored view, but a frontend to build and run). | `userTask_clarifyAlternative` | [clarify-alternative.form][form] (Tasklist); `SelectAlternativeController` (`POST .../clarify-alternative`) | [#11][i11]; [#5][i5] is the inbox the Make option needs |
| Contract & e-signature | 0.68, product (Assumption) | **Open** (#8) | Does not set us apart, but no signature means no lease. E-signature is regulated and sold as a product or service. | `serviceTask_sendContract`, `event_contractSigned`, `serviceTask_cancelContract` | `ContractPort`, `LoggingContractAdapter` (simulated) | [#8][i8] |
| Customer notification | 0.86, commodity (Assumption) | **Open** (#9) | The delivery sets nobody apart (the content may). Transactional messaging is a utility. | `serviceTask_sendContract`, `serviceTask_sendRejection`, `serviceTask_confirmContractCancellation` | `NotificationPort`, `LoggingNotificationAdapter` (simulated) | [#9][i9] |
| Dealer integration | 0.38, custom-built (Assumption) | **Partner** (the dealer); how to connect is **open** (#10) | The dealer is an independent business: we never build its system. Dealer interfaces are probably uneven (APIs, portals). | `serviceTask_orderBike`, `event_bikeUnavailable`, `event_handoverReported` | `BikeDealerPort`, `BikeDealerAdapter` (simulated); `ReportHandoverController` | [#10][i10] |
| Application framework | 0.80, commodity | **Buy**: Spring Boot 4 (open source) | The standard way to run a JVM service. We use it as it is. | none; it hosts the delegate beans the service tasks call | [versions][libs], [build.gradle.kts][gradle] | [ADR-0008][adr8], [ADR-0014][adr14] |
| Database | 0.90, commodity | **Buy as a service** (outsource): PostgreSQL (Assumption) | A standard database sets nothing apart. The service reads its datasource from `SPRING_DATASOURCE_*`, so a managed PostgreSQL needs no code change. | none; it holds the engine state of every case and the `leasing_application` table | [docker-compose.yml][compose] (local only); [Flyway migrations][migrations]; `LeasingApplicationPersistenceAdapter` | [ADR-0010][adr10], [ADR-0011][adr11] |


## Open decisions

Four parts are still open. Each is a GitHub issue with options, the five criteria and acceptance
criteria. The number in the first column is the annotation number on the map.

designIQ cannot link a BPMN element to a Wardley component. It resolves cross-model links only for
call activities and business rule tasks. So the table below is the documented mapping from Wardley
component to BPMN element ids.

The same issues are designIQ todos on those elements. Each issue has the labels `todo` and
`process:bike-leasing`, and an anchor block that names these element ids. While the issue is open,
designIQ pins it on the elements of `bike-leasing.bpmn`.

| Map | Issue | Wardley component | Question | Anchored BPMN element ids | Also affected | Today | Related |
|---|---|---|---|---|---|---|---|
| 1 | [#8][i8] | Contract & e-signature | Contract issuing and e-signature: make, buy or adapt? | `serviceTask_sendContract`, `event_contractSigned`, `serviceTask_cancelContract` | `event_signatureDeadline`, `event_compensateContract` | simulated: `LoggingContractAdapter` | #1, #3, #9 |
| 2 | [#9][i9] | Customer notification | Which channel, and how is it sourced? | `serviceTask_sendContract`, `serviceTask_sendRejection`, `serviceTask_confirmContractCancellation` | the messages of #1 and #3 | simulated: `LoggingNotificationAdapter` | #1, #3, #8 |
| 3 | [#10][i10] | Dealer integration | How do we connect the dealer? | `serviceTask_orderBike`, `event_handoverReported` | `event_bikeUnavailable` | simulated: `BikeDealerAdapter` | #4, #7, #11 |
| 4 | [#11][i11] | Back-office task UI | Adapt the CIB seven Tasklist or build an own inbox? | `userTask_clarifyAlternative` | the return clarification that #4 brings back | Tasklist form and REST endpoint both work | #4, #5, #10 |


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
6. Database as a service: production hosting offers a managed PostgreSQL. This repository decides
   no hosting; [ADR-0011][adr11] covers the container image and the overridable datasource, not where
   the database runs. Locally, PostgreSQL runs in Docker.
7. Credit rating rules use only what the customer states (age, monthly net income). No credit bureau
   is asked. A bureau check would be a new component, so it is not on the map.


## When a decision is taken

1. Record it as an ADR in [`docs/adr/`](../adr/): copy [0000-adr-template.md][adr0], take the next
   free number, status Accepted. Cover the five criteria and the options you rejected.
2. Update the map in [sourcing-map.owm](sourcing-map.owm):
   - set the decorator of the component (`build`, `buy`, `outsource` or `market`);
   - replace the "Open: ..." annotation, for example with "Adapt: ...", or delete it;
   - point the component's `url` at the ADR instead of the issue;
   - for #11, drop the pipeline variant that lost (and annotation 5 if the Tasklist lost).
3. Update the component's row in this file (Decision, Rationale, Issue / ADR) and remove it from
   "Open decisions".
4. Validate the map: `npx --yes @miragon/design-iq-validator@5.3.0 --root . sourcing-map`.
5. Close the issue with a link to the ADR. Only open issues are todos, so its pins disappear from the
   BPMN elements in designIQ. For Buy or Adapt, open the follow-up implementation issue that the
   acceptance criteria of #8 to #11 ask for.

Edit a model either in designIQ (it reaches Git through a release pull request) or in Git, not in
both at the same time. A file changed on both sides becomes a conflict in designIQ.


## Related

- [Glossary](../glossary.md) for every term above.
- [C4 system context](c4-system-context.md) and [C4 container view](c4-container.md): the same parts
  as boxes. A Make decision in #8 to #10 pulls a capability into the service; Buy or Adapt makes a
  simulated box a real system.
- [Use case](../use-cases/bike-leasing/bike-leasing-use-case.md) and
  [feature map](../use-cases/bike-leasing/feature-map/feature-map.md) for features #1 to #7.

[usecase]: ../use-cases/bike-leasing/bike-leasing-use-case.md
[bpmn]: ../use-cases/bike-leasing/process/bike-leasing.bpmn
[dmn]: ../use-cases/bike-leasing/process/checkCreditRating.dmn
[openapi]: ../../openapi/openapi.json
[code]: ../../src/main/kotlin/io/miragon/blueprint/
[form]: ../../src/main/resources/forms/clarify-alternative.form
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
[adr15]: ../adr/0015-reduce-the-executable-process-to-a-core.md
[adr16]: ../adr/0016-docs-is-the-designiq-workspace.md
[i5]: https://github.com/miragon-blueprints/miragon-ai-example/issues/5
[i8]: https://github.com/miragon-blueprints/miragon-ai-example/issues/8
[i9]: https://github.com/miragon-blueprints/miragon-ai-example/issues/9
[i10]: https://github.com/miragon-blueprints/miragon-ai-example/issues/10
[i11]: https://github.com/miragon-blueprints/miragon-ai-example/issues/11
