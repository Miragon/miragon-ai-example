---
name: create-persistence-adapter
argument-hint: "[<path-to-Repository-port-file-or-domain-aggregate>]"
description: "Create or update the JPA persistence adapter of an aggregate: entity, repository, mapper, adapter, Flyway migration and @DataJpaTest. Use when persisting an aggregate or adding a column or query."
allowed-tools: Read, Write, Edit, Glob, Grep, Bash(./gradlew *)
---

# Skill: create-persistence-adapter

Files per aggregate in `service/app/src/main/kotlin/io/miragon/blueprint/adapter/outbound/db/`, test
in `service/app/src/test/kotlin/io/miragon/blueprint/adapter/outbound/db/`. The reference is the
`LeasingApplication*` set; the backend rules are in `service/AGENTS.md`.

| File | Content |
|---|---|
| `*Entity.kt` | `@Entity`, `snake_case` columns, enums `@Enumerated(EnumType.STRING)` |
| `*JpaRepository.kt` | Spring Data interface with the queries the port needs |
| `*EntityMapper.kt` | Kotlin `object`, value objects ↔ primitives (`ApplicationId(UUID)` → `UUID`) |
| `*PersistenceAdapter.kt` | `@Component` implementing exactly the outbound `*Repository` port |
| `service/app/src/main/resources/db/migration/V<next>__<what>.sql` | the schema change |

## Production pattern

```kotlin
@Component
class LeasingApplicationPersistenceAdapter(
    private val repository: LeasingApplicationJpaRepository,
) : LeasingApplicationRepository {

    override fun save(application: LeasingApplication): LeasingApplication {
        val entity = repository.save(LeasingApplicationEntityMapper.toEntity(application))
        return LeasingApplicationEntityMapper.toDomain(entity)
    }

    override fun findById(id: ApplicationId): LeasingApplication? =
        repository.findByApplicationId(id.value)?.let { LeasingApplicationEntityMapper.toDomain(it) }

    override fun findAll(criteria: LeasingApplicationRepository.Criteria): LeasingApplicationRepository.Page {
        val newestFirst = PageRequest.of(criteria.page, criteria.size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = criteria.status?.let { repository.findAllByStatus(it, newestFirst) } ?: repository.findAll(newestFirst)
        return LeasingApplicationRepository.Page(
            items = result.content.map { LeasingApplicationEntityMapper.toDomain(it) },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}
```

- **Spring Data types stop at the adapter.** The port owns `Criteria`/`Page`; the adapter translates.
- **Flyway owns the schema.** Hibernate only validates, so every entity change ships with the next
  `V<n>__*.sql`; never edit an applied migration. A new filter or sort column gets an index.

## Test pattern

```kotlin
@DataJpaTest
@ActiveProfiles("test")
@Import(LeasingApplicationPersistenceAdapter::class)
class LeasingApplicationPersistenceAdapterTest {

    @Autowired
    private lateinit var underTest: LeasingApplicationPersistenceAdapter

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))

    @Test
    fun `saves and reloads an application`() {
        val application = testLeasingApplication(id = id)

        underTest.save(application)
        entityManager.flush()
        entityManager.clear()

        assertThat(underTest.findById(id)).usingRecursiveComparison().isEqualTo(application)
    }

    @Test
    @Sql("classpath:sql/leasing-application.sql")
    fun `findById maps an existing row back to the domain`() {
        assertThat(underTest.findById(id))
            .usingRecursiveComparison()
            .isEqualTo(testLeasingApplication(id = id))
    }
}
```

- **Round trip through a cleared context:** `flush()` and `clear()` before reading back, or the test
  only reads the first-level cache.
- **Read tests seed independently** of the adapter under test (`@Sql` script in
  `service/app/src/test/resources/sql/` or the JPA repository), so a broken `save` cannot hide a
  broken read.
- Compare whole objects (`usingRecursiveComparison`) built from the domain test factory, one test per
  port method and per filter (including the empty result). The mapper gets its own plain unit test
  (`LeasingApplicationEntityMapperTest`) for every field.

## Instructions

1. **Resolve the aggregate** from `$ARGUMENTS` (a `*Repository` port or a domain aggregate under
   `domain/{leasing,bike}/`); else list the ports and ask.
2. **Derive the target state:** columns, value-object mapping, the queries the port needs. Add only
   what is missing to existing files.
3. **Write the tests first**, then entity, repository, mapper, adapter and migration.
4. **Verify:** `./gradlew test --tests "*PersistenceAdapterTest" --tests "*EntityMapperTest"`, then
   `./gradlew build` (architecture rules, migrations against the entities).
