# Contributing

Thanks for your interest in the MiraVelo bike-leasing designIQ demo. Bug reports, feature ideas,
docs, models and code are all welcome. The [README](README.md) explains what the repo is for. This
file covers how to work on it.

## Getting started

```bash
git clone git@github.com:miragon-blueprints/miragon-ai-example.git
cd miragon-ai-example
npm ci && npm run hooks:install   # bpmnlint + the pre-commit hook
```

You need:

- **JDK 21**. The Gradle wrapper is included, so nothing else needs installing for the build.
- **Docker** (or Podman) with Compose, for Postgres.
- **Node.js 22+**, for bpmnlint, the Bruno scenarios and the designIQ validator. The service itself
  has no Node dependency.

Start Postgres, then the backend and engine on :8081:

```bash
docker compose -f stack/docker-compose.yml up -d
```

```bash
./gradlew bootRun
```

### Ports

| What | Port |
|---|---|
| Postgres | 5432 |
| Backend (REST + `/engine-rest`) | 8081 |
| CIB seven Cockpit / Tasklist / Admin | 8081/camunda (admin/admin) |
| CIB seven webclient (new UI) | 8081/webapp (admin/admin) |
| OpenAPI spec · Swagger UI | 8081/v3/api-docs · 8081/swagger-ui.html |
| Actuator (health · liveness/readiness · prometheus) | 8081/actuator |

So: <http://localhost:8081/camunda>, <http://localhost:8081/webapp>,
<http://localhost:8081/swagger-ui.html> and <http://localhost:8081/actuator/health> (status `UP`).

Under Conductor the ports are fixed and the workspace runs `nonconcurrent` (see
[ADR-0006](docs/adr/0006-fixed-ports-for-v1-portless-as-the-upgrade.md)).

## Commands

From the repo root:

| Task | Command |
|---|---|
| Full build (arch + unit + process + model validation + spec export) | `./gradlew build` |
| Mutation testing (gate 80) | `./gradlew pitest` |
| Regenerate the typed process API after editing a `.bpmn` | `./gradlew generateBpmnModels` |
| Regenerate the OpenAPI contract | `./gradlew test --tests "io.miragon.blueprint.openapi.OpenApiSpecExportTest"` |
| Lint the executable BPMN models | `npm run lint:bpmn` |
| Validate the designIQ models under `docs/` | `npx --yes @miragon/design-iq-validator@5.3.0 --root .` |
| Build the OCI image | `./gradlew bootBuildImage` |

The tests use an in-memory H2 database, so `./gradlew build` needs no running Postgres.

## Bruno scenarios and demo data

With the service running, prepare the demo cases or drive every REST scenario end to end:

```bash
cd bruno && npx --yes @usebruno/cli@4.0.0 run 00-demo-data --env local && cd ..
```

```bash
cd bruno && npx --yes @usebruno/cli@4.0.0 run . --env local -r && cd ..
```

`00-demo-data` leaves four cases in demo-friendly states and creates the Tasklist filter "All tasks"
if it is missing. The numbered folders cover the happy path,
the signature deadline, a rejected credit check, an unavailable bike, a declined alternative, the
alternative chosen via the Tasklist, invalid requests and the incident demo. The full list is in
[bruno/README.md](bruno/README.md). CI runs the whole collection on every pull request.

To start from an empty engine: stop the service, run `docker compose -f stack/docker-compose.yml down -v`,
start Postgres and the service again, and load `00-demo-data` again.

## Run it in containers

The dev loop above runs the backend from source. To run it as a container instead, build the OCI
image (Spring buildpacks, no Dockerfile) and run it against the Postgres compose stack. The rationale
is in [ADR-0011](docs/adr/0011-build-and-deployment-approach.md).

Build the image. It produces `miravelo/cibseven-embedded-example:1.0-SNAPSHOT`:

```bash
./gradlew bootBuildImage
```

Then run it against the compose Postgres:

```bash
# 1. start Postgres
docker compose -f stack/docker-compose.yml up -d

# 2. run the image against it
docker run --rm -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/bikeleasing \
  -e SPRING_DATASOURCE_USERNAME=admin -e SPRING_DATASOURCE_PASSWORD=admin \
  miravelo/cibseven-embedded-example:1.0-SNAPSHOT
```

**Podman:** the image build needs a Docker-API socket. Expose Podman's, then build the image as above:

```bash
podman system service --time=0 unix:///tmp/podman.sock &
export DOCKER_HOST=unix:///tmp/podman.sock
```

**Configuration.** `application.yaml` ships dev defaults. The deploy-relevant values are read from
the environment and win over the defaults:

| Env var | Purpose | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/bikeleasing` |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | DB credentials | `admin` / `admin` |

> **Not production-hardened.** The image carries the example `jwtSecret` and the admin/admin
> credentials from `application.yaml`. Override them, and the DB credentials, before running it
> anywhere real.

## Ground rules

- **Start from an issue.** Every change traces back to one. Open an issue, or pick an existing one,
  and agree on the approach *before* you write code. Then reference it in the PR (`Closes #123`).
  The planned features are #1–#7 and the open decisions #8–#11. [AGENTS.md](AGENTS.md) explains what
  implementing one involves.
- **Read [AGENTS.md](AGENTS.md) first.** It is the single source of guidance for humans and AI agents
  alike (see [ADR-0005](docs/adr/0005-agents-md-as-the-single-source.md)).
- **Conventional Commits.** Commit messages and PR titles follow
  [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`,
  `refactor:`, `test:`, `chore:`). Write everything in **English**.
- **Keep the gates green.** The architecture, contract-drift and mutation (≥ 80) gates run in CI on
  every PR. They are fitness functions, not style guides: a violation fails the build. The mutation
  gate is **diff-scoped** on PRs (only the classes you changed). The full-module gate-80 sweep runs
  nightly.
- **Add tests.** This is a TDD codebase. Match the test style to the layer (see AGENTS.md). Because of
  mutation testing, a test that runs without asserting fails CI.
- **Changing the process?** Edit the model in `docs/use-cases/bike-leasing/process/` (Gradle packages
  it onto the classpath). Then run `./gradlew generateBpmnModels`, commit the regenerated
  `adapter/process` files (never hand-edit them), run `npm run lint:bpmn` and the process tests. Keep
  element ids stable, because issues and docs refer to them.
- **Never edit `docs/use-cases/bike-leasing/original/`.** It is the frozen original process
  (tag `original-process-v1`).
- **Changing the API?** Re-export the spec with the `OpenApiSpecExportTest` (every full build runs
  it), so that the committed `openapi/openapi.json` stays in sync. It is **drift-gated in CI**.
- **Changing the database schema?** Flyway owns it. Add a new forward-only migration
  `V{n}__description.sql` under `src/main/resources/db/migration/` in the same change as the entity
  edit, and never edit an applied migration. Hibernate runs `validate`, so a mismatch fails startup.
  See [ADR-0010](docs/adr/0010-flyway-for-database-migrations.md).
- **Changing a design artifact?** Everything under `docs/` is a designIQ model and must pass the
  designIQ validator. The only accepted errors are the known compensation false positives on the
  executable and the original process
  ([ADR-0016](docs/adr/0016-docs-is-the-designiq-workspace.md)). Keep file stems unique under
  `docs/`, keep BPMN diagrams complete (BPMNDI), and write Markdown that reads well as plain text.

## Before opening a PR

From the repo root:

```bash
./gradlew build pitest                                     # all tests, mutation score >= 80
git diff --exit-code openapi/openapi.json                  # the API contract must not drift
npm run lint:bpmn                                          # the executable models
npx --yes @miragon/design-iq-validator@5.3.0 --root .      # the designIQ models: no new errors
```

CI runs the build, bpmnlint, the contract-drift check, the mutation gate and the Bruno scenarios on
every pull request (JDK 21, Node 22). Before you open a PR, check that a feature is wired end to end:
BPMN element → outbound port → service → inbound port → REST controller → `openapi.json` → Bruno
scenario → feature map and glossary.

## Reporting bugs / requesting features

Open an issue. For a process- or contract-related bug, attach the relevant `.bpmn` model or the
`openapi.json` diff; that is the fastest path to a fix. To make a feature request show up in designIQ
as a todo, label it `todo` and `process:bike-leasing` and start the body with the anchor block
described in [AGENTS.md](AGENTS.md#designiq-rules).
