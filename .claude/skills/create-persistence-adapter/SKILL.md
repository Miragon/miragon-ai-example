---
name: create-persistence-adapter
argument-hint: "[<path-to-Repository-port-file-or-domain-aggregate>]"
description: Scaffold or update a JPA persistence adapter (outbound adapter) for a domain aggregate — entity, Spring Data repository, mapper, the @Component adapter implementing the outbound Repository port, and its Flyway migration. Use when the user asks to "create a persistence adapter", "generate a JPA adapter", "scaffold the persistence layer", or "add an outbound db adapter".
allowed-tools: Read, Write, Glob
---

# Skill: create-persistence-adapter

Generate or update the persistence files for one aggregate in
`service/app/src/main/kotlin/io/miragon/blueprint/adapter/outbound/db/`. See
[ADR-0002](../../../docs/adr/0002-hexagonal-architecture-for-the-backend.md) and
[ADR-0010](../../../docs/adr/0010-flyway-for-database-migrations.md). The reference is the
`LeasingApplication*` set.

Files per aggregate:

- `*Entity.kt` — `@Entity` class mapping the aggregate to a table.
- `*JpaRepository.kt` — Spring Data `JpaRepository` interface.
- `*EntityMapper.kt` — entity ↔ domain conversion, a Kotlin **`object`**.
- `*PersistenceAdapter.kt` — `@Component` implementing the outbound port.
- `service/app/src/main/resources/db/migration/V<next>__<what>.sql` — the schema change.

## IMPORTANT — the rules the build enforces

- The adapter implements **every** method of the outbound port and **that port only**.
- Suffixes in `adapter.outbound`: `PersistenceAdapter`, `Adapter`, `Mapper`, `Entity`, `Repository`.
- The outbound port is named `*Repository` in `application.port.outbound`.
- **Spring Data types stop at the adapter.** The port owns its own `Filter`/`Page`/`Criteria` types
  (see `LeasingApplicationRepository`); the adapter translates to `Pageable`/`Page`.
- Columns are `snake_case`; enums use `@Enumerated(EnumType.STRING)`.
- Hibernate runs with `ddl-auto: validate`: every entity change needs a Flyway migration. Never edit an
  applied migration; add the next `V<n>`.

## Instructions

1. **Resolve the aggregate** from `$ARGUMENTS` (a `*Repository` port or a domain aggregate under
   `domain/{leasing,bike}/`); else list the ports and ask.
2. **Derive the target state:** value objects map to primitives (`ApplicationId(UUID)` → `UUID`,
   `Email(String)` → `String`), `snake_case` names, the `findBy…` queries the port needs. For existing
   files add only what is missing.
3. **Write the files**, including the migration.
4. **Report and hand off:** add a `@DataJpaTest` (`./gradlew test --tests "*PersistenceAdapterTest"`)
   and run `./gradlew build` (architecture rules + migrations against the schema).
