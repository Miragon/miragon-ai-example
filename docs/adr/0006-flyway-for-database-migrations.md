# 0006 — Flyway owns the database schema

- **Status:** Accepted
- **Date:** 2026-10-16
- **Origin:** Consolidated baseline; earlier history in git.

## Context

Two concerns share one Postgres database: the application table `leasing_application` and the
CIB seven engine's `ACT_*` tables. The engine manages its own schema
(`camunda.bpm.database.schema-update: true`). The application schema needs changes that are
versioned, reviewable in a PR, repeatable and non-destructive; a Hibernate-generated schema is none
of these, and it loses data on restart.

## Decision

**Flyway owns the application schema; Hibernate only validates it** (`spring.jpa.hibernate.ddl-auto:
validate` in `application.yaml`).

- **Dependencies** (`database` bundle in `gradle/libs.versions.toml`): `spring-boot-starter-flyway`
  and `flyway-database-postgresql`. The starter is required, not plain `flyway-core`: Spring Boot 4
  moved the Flyway auto-configuration out of `spring-boot-autoconfigure`, so `flyway-core` alone
  never wires Flyway to the datasource and the migrations silently do not run.
- **Migrations** live in `service/app/src/main/resources/db/migration/` as `V{n}__description.sql`
  (today `V1` to `V3`). They are forward-only: an applied migration is checksummed and never edited;
  a fix is a new migration. Flyway applies pending migrations at start-up, before Hibernate
  validates, and records them in `flyway_schema_history`.
- **Flyway owns the application tables only.** The engine tables stay with the engine.
- **Schema only, no data.** Migrations carry no demo or test data; demo cases come from the Bruno
  collection `00-demo-data`.
- **Tests run without Flyway.** The `test` profile (`application-test.yaml`) uses in-memory H2 with
  `ddl-auto: create-drop` and `spring.flyway.enabled: false`, because the migrations are written for
  Postgres. The migrations run against real Postgres in local `bootRun` and in the Bruno and
  Playwright CI jobs.

The how-to (adding a migration) is in [CONTRIBUTING.md](../../CONTRIBUTING.md).

## Consequences

- **Positive:** schema changes are versioned SQL, reviewed in the PR that needs them, and applied
  the same way to every environment; data survives restarts.
- **Negative / trade-offs:** a schema change is two coordinated edits, the entity and a new
  migration. A mismatch fails at start-up, not at compile time.
- **Not covered:** the unit and `@DataJpaTest` tests do not run the migrations, so a migration error
  shows only in the end-to-end jobs or locally. Verifying migrations against real Postgres in the
  test suite (Testcontainers) is deferred; there is no Testcontainers setup. Reference data, if it
  ever appears, has no home yet (a Flyway repeatable migration `R__…sql` is the natural place).
