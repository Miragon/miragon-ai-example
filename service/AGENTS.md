# service/AGENTS.md

The Kotlin backend: `app/` is the bike-leasing service (Spring Boot 4, hexagonal, embedded CIB seven),
`common-architecture-tests/` the reusable rule suite every service under `service/` runs. The root
`AGENTS.md` covers the process model and the contract.

## Map

```
app/src/main/kotlin/io/miragon/blueprint/
  adapter/inbound/rest        REST controllers + OpenAPI / problem-details configuration
  adapter/inbound/cibseven    JavaDelegates for the service tasks (extend BaseDelegate)
  adapter/outbound/cibseven   drives the engine: start, correlate, complete and list user tasks
  adapter/outbound/db         JPA persistence
  adapter/outbound/…          simulated contract / insurance / notification / dealer adapters
  adapter/process             GENERATED typed process API (+ hand-written HistoryCleanupConfiguration)
  application/{port,service}  use-case ports and their services
  domain/{leasing,bike}       pure domain model
app/src/main/resources/db/migration   Flyway migrations
common-architecture-tests/src/main/kotlin/io/miragon/common/architecture/   the rules
```

## Architecture rules

The rules in `common-architecture-tests` (ArchUnit and Konsist) fail the build. Read
`HexagonalArchitectureTest.kt` and `NamingConventionArchitectureTest.kt` before adding a class. What
they do not spell out:

- **One inbound port per controller and per delegate.** A read and a completion are two controllers.
- **No top-level `config` package.** `io.miragon.blueprint.config` fails the containment rule.
  Cross-cutting `@Configuration` goes in `adapter.inbound.rest` (suffix `Configuration`).
- **Spring Data types stop at the adapter.** Ports own their `Criteria`/`Page` types.
- **Suffixes:** inbound port `UseCase|Query`; outbound `Port|Repository|Process`; service
  `Service|Configuration`; `adapter.inbound.rest` `Controller|Dto|Input|Mapper|Configuration`;
  `adapter.inbound.cibseven` `Delegate|Worker|Listener`; `adapter.outbound`
  `PersistenceAdapter|Adapter|Mapper|Entity|Repository`.
- **The domain has no clock.** Pass the time in; never call `now()` in `domain/`.

## Process glue

- A service task calls a Spring-bean delegate by expression (`#{beanName}`); the model test
  `ServiceTaskDelegateExpressionRule` rejects anything else.
- A delegate extends `BaseDelegate`, overrides `executeTask`, reads the application id from the
  business key and any other variable through `BikeLeasingProcessProcessApi`, never a raw string.
  It turns a domain exception into `BpmnError(Errors.<X>.code, …)` where the model has a boundary event.
- A `@Transactional` service behind a delegate that throws the exception the delegate maps declares
  it in `noRollbackFor`, or the error path never runs.
- Never hand-edit `adapter/process` (except `HistoryCleanupConfiguration.kt`); edit the `.bpmn` and
  run `./gradlew generateBpmnModels`.

## Persistence

Flyway owns the schema. An entity change ships with a new forward-only `V{n}__description.sql` in the
same change; never edit an applied migration. Hibernate only validates, so a mismatch fails startup.
A new query on a large table gets the index it filters or sorts on.

## Testing

TDD. The test tree mirrors `main` package by package: a test lives in the package of the class it
tests (`OpenApiSpecExportTest` and `ActuatorEndpointsTest` next to the controllers in
`adapter/inbound/rest`, the process tests next to the generated process API in `adapter/process`).
`ArchitectureTest` covers the whole application and sits in the root package.
Match the test style to the layer:

| Layer | Test style |
|---|---|
| domain | plain unit tests |
| application service | unit tests with mocked ports (MockK) |
| `adapter.inbound.rest` | `@WebMvcTest` with the use case mocked |
| `adapter.inbound.cibseven` | MockK unit test per delegate (skill `create-delegate`) |
| `adapter.outbound.db` | `@DataJpaTest` |
| process end to end | CIB seven process tests in `adapter/process/`, paths asserted via `ProcessPath` (`adapter/process/ProcessPathAssertions.kt`) |

- **Mock ports, not the domain.** Build real domain objects (`testLeasingApplication()`).
- **Strict mocks, verified calls in unit tests.** Stub with `every`, then `verify` and
  `confirmVerified`; a relaxed mock lets a removed call survive the mutation gate. (Process tests may
  relax the ports they do not assert on.)
- **Build the expected value independently**, never from the object the code under test received.
- **Seed a persistence test directly**, not through the adapter under test, and flush and clear before
  reading back.
- Tests run against H2 with Flyway off; no Postgres needed.

## Verify

- One class: `./gradlew test --tests "*<Name>Test"`
- Architecture: `./gradlew test --tests "io.miragon.blueprint.ArchitectureTest"`
- Process: `./gradlew test --tests "io.miragon.blueprint.adapter.process.*"`
- Mutation for one class: `./gradlew pitest -PmutationTargetClasses=<fqcn>` (gate 80)
