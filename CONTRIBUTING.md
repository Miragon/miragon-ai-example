# Contributing

Thanks for your interest in the MiraVelo bike-leasing example. Bug reports, feature ideas,
docs, models and code are all welcome. The [README](README.md) explains what the repo is for. This
file covers how to work on it.

## Getting started

```bash
git clone git@github.com:miragon-blueprints/miragon-ai-example.git
cd miragon-ai-example
npm ci && npm run hooks:install   # bpmnlint, bpmn-cli, operate + the pre-commit hook
npm --prefix frontend ci          # the frontend
```

You need:

- **JDK 21**. The Gradle wrapper is included, so nothing else needs installing for the build.
- **Docker** (or Podman) with Compose, for Postgres.
- **Node.js** in the version from `frontend/.nvmrc`, for the frontend, bpmnlint, the Bruno scenarios
  and the designIQ validator. The service itself has no Node dependency.

Start Postgres, the backend and engine on :8081, and the frontend on :5173:

```bash
docker compose -f stack/docker-compose.yml up -d
```

```bash
./gradlew bootRun
```

```bash
npm --prefix frontend run dev
```

### Ports

| What | Port |
|---|---|
| Frontend (Vite dev server, proxies `/api`, `/v3/api-docs`, `/engine-rest`, `/camunda`, `/webapp` to 8081) | 5173 |
| Frontend in a container (nginx, `stack/docker-compose.full.yml`) | 8090 |
| Postgres | 5432 |
| Backend (REST + `/engine-rest`) | 8081 |
| CIB seven webclient: Cockpit / Tasklist / Admin (entry point) | 8081/webapp/#/seven/auth/start (admin/admin) |
| CIB seven classic webapps | 8081/camunda (admin/admin) |
| OpenAPI spec · Swagger UI | 8081/v3/api-docs · 8081/swagger-ui.html |
| Actuator (health · liveness/readiness · prometheus) | 8081/actuator |

So: <http://localhost:5173> (the frontend), <http://localhost:8081/webapp/#/seven/auth/start> (Cockpit),
<http://localhost:8081/swagger-ui.html> and <http://localhost:8081/actuator/health> (status `UP`).

This table is the only place the ports are listed; `frontend/vite.config.ts`,
`frontend/playwright.config.ts`, `.conductor/settings.toml` and `.operate.json` must match it. The
ports are fixed on purpose: under Conductor the workspace runs `nonconcurrent`, so only one worktree
runs the stack at a time. Per-worktree URLs (portless) would only cover the Vite server; the backend, Postgres and the
shared engine schema would still collide.

## Commands

From the repo root:

| Task | Command |
|---|---|
| Full build (arch + unit + process + model validation + spec export) | `./gradlew build` |
| Mutation testing (gate 80) | `./gradlew pitest` |
| Regenerate the typed process API after editing a `.bpmn` | `./gradlew generateBpmnModels` |
| Regenerate the OpenAPI contract | `./gradlew test --tests "io.miragon.blueprint.adapter.inbound.rest.OpenApiSpecExportTest"` |
| Frontend gates (format, lint, FSD, dead code, types, unit tests) | `npm --prefix frontend run verify` |
| Regenerate the frontend API client after a contract change | `npm --prefix frontend run api:generate` |
| Frontend end-to-end tests (running stack and dev server) | `npm --prefix frontend run e2e` |
| Lint the executable BPMN models | `npm run lint:bpmn` |
| Read, change, validate or render a BPMN model | `npx bpmn show <file>`, `npx bpmn guide --short` |
| Where a running case waits, open incidents (running backend) | `npx operate inspect --business-key <applicationId>`, `npx operate status` |
| Validate the designIQ models under `docs/` | `npx --yes @miragon/design-iq-validator@5.3.0 --root .` |
| Build the OCI image | `./gradlew bootBuildImage` |

The tests use an in-memory H2 database, so `./gradlew build` needs no running Postgres.

### The mutation gate

`./gradlew pitest` fails below a mutation score of 80. On a pull request CI mutates only the main
classes the PR changed (`-PmutationTargetClasses=…`); the nightly run mutates the whole module. To
check one class locally:

```bash
./gradlew pitest -PmutationTargetClasses=io.miragon.blueprint.application.service.SendContractService
```

A surviving mutant means a test executes the code without noticing the change: typically a relaxed
mock whose call is never verified, or an assertion on a value built from the same input the code under
test received. Add the missing assertion; never lower the threshold. Generated code
(`adapter/process`) and the application class are excluded; process and architecture tests do not
count as killing tests.

## Bruno scenarios and demo data

With the service running, prepare the demo cases or drive every REST scenario end to end:

```bash
cd bruno && npx --yes @usebruno/cli@4.0.0 run 00-demo-data --env local && cd ..
```

```bash
cd bruno && npx --yes @usebruno/cli@4.0.0 run . --env local -r && cd ..
```

`00-demo-data` leaves cases in demo-friendly states, so the list and the inbox are not empty. The
numbered folders cover the happy path, an invalid application, a rejected credit check, the
withdrawal, invalid requests and the incident demo. The full list is in
[bruno/README.md](bruno/README.md). CI runs the whole collection and the Playwright scenarios on
every pull request.

To start from an empty engine: stop the service, run `docker compose -f stack/docker-compose.yml down -v`,
start Postgres and the service again, and load `00-demo-data` again.

## Run it in containers

The dev loop above runs the backend from source. To run it as a container instead, build the OCI
image (Spring buildpacks, no Dockerfile) and run it against the Postgres compose stack. The rationale
is in [ADR-0007](docs/adr/0007-build-and-deployment-approach.md).

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

To run the frontend in a container too (nginx on :8090, proxying to the service), use the full
stack instead. Its service binds 8081 too, so stop `./gradlew bootRun` first:

```bash
./gradlew bootBuildImage
docker compose -f stack/docker-compose.full.yml up --build
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
  The planned features and open decisions are listed in the [README](README.md#roadmap) and as
  GitHub issues; [AGENTS.md](AGENTS.md) explains what implementing one involves.
- **Read [AGENTS.md](AGENTS.md) first.** It is the single source of guidance for humans and AI agents
  alike, with area files in [service/AGENTS.md](service/AGENTS.md) and
  [frontend/AGENTS.md](frontend/AGENTS.md) (see [ADR-0001](docs/adr/0001-record-architecture-decisions.md)).
  Claude Code (v2.1.277 or later) reads them natively. Do not add a `CLAUDE.md` or `CLAUDE.local.md`
  anywhere in the repo: either one switches Claude Code back to reading only `CLAUDE.md` files.
- **Conventional Commits.** Commit messages and PR titles follow
  [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`,
  `refactor:`, `test:`, `chore:`, `ci:`, `build:`). Write everything in **English**.
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
  it), so that the committed `openapi/openapi.json` stays in sync, then regenerate the frontend
  client with `npm --prefix frontend run api:generate`. Both are **drift-gated in CI**.
- **Changing the database schema?** Flyway owns it. Add a new forward-only migration
  `V{n}__description.sql` under `service/app/src/main/resources/db/migration/` in the same change as the entity
  edit, and never edit an applied migration. Hibernate runs `validate`, so a mismatch fails startup.
  See [ADR-0006](docs/adr/0006-flyway-for-database-migrations.md).
- **Changing a design artifact?** Everything under `docs/` is a designIQ model and must pass the
  designIQ validator. The executable process validates clean; the only accepted errors are the known
  ones on the frozen original ([ADR-0008](docs/adr/0008-docs-is-the-designiq-workspace.md)). Keep file stems unique under
  `docs/`, keep BPMN diagrams complete (BPMNDI), and write Markdown that reads well as plain text.

## Before opening a PR

From the repo root:

```bash
./gradlew build pitest                                     # all tests, mutation score >= 80
git diff --exit-code openapi/openapi.json                  # the API contract must not drift
npm run lint:bpmn                                          # the executable models
npm --prefix frontend run verify                           # the frontend gates
npm --prefix frontend run api:check                        # the client matches the contract
npx --yes @miragon/design-iq-validator@5.3.0 --root .      # the designIQ models: no new errors
```

CI runs the build, bpmnlint, the contract-drift check, the mutation gate, the frontend gates, the
Bruno scenarios and the Playwright scenarios on every pull request. Before you open
a PR, check that a feature is wired end to end: BPMN element → outbound port → service → inbound port
→ REST controller → `openapi.json` → frontend client and UI → Bruno and Playwright scenario →
feature map and glossary.

## Dependency updates

Dependabot proposes updates; patch and minor updates merge automatically once CI is green, majors are
merged by hand. `.github/dependabot.yml` carries no `ignore` entries. When an update cannot be taken
yet, comment `@dependabot ignore this major version` (or `minor version` / `dependency`) on its pull
request and open an issue for the blocker, so the update comes back once it is fixable
([ADR-0005](docs/adr/0005-track-the-latest-major-versions.md)).

bpmn-cli and operate are not covered by Dependabot. Bump each one by hand in two places together:
its exact version in the root `package.json` and the `ref` of its marketplace in
`.claude/settings.json`.

## Reporting bugs / requesting features

Open an issue. For a process- or contract-related bug, attach the relevant `.bpmn` model or the
`openapi.json` diff; that is the fastest path to a fix.

To make a feature request show up in designIQ as a todo, label it `todo` and `process:bike-leasing`
and start the body with this anchor block (one `element:` line per element, none for a process-level
todo):

```
<!-- bpmiq:todo v1
process: bike-leasing
file: docs/use-cases/bike-leasing/process/bike-leasing.bpmn
element: <element id> | <element name>
-->
```

`process` is the file stem, not the BPMN process id (`bikeLeasingProcess`). The element ids must exist
in the executable model. The marker `bpmiq:todo v1` is frozen: never rewrite it, and keep the block at
the top of the body when you edit an issue.
