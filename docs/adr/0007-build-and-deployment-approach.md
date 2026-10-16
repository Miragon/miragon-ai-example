# 0007 — Operability and delivery: OCI images, probes and metrics

- **Status:** Accepted
- **Date:** 2026-10-16
- **Origin:** Consolidated baseline; earlier history in git.

## Context

A blueprint that aims to be production-shaped has to answer two questions out of the box: how do I
ship this as a container, and how does an orchestrator and a monitoring system see that it is alive
and healthy. The service is a Spring Boot 4 app with an embedded engine; the frontend is a static
single-page app. Spring Boot covers both questions with built-in tooling (buildpacks, Actuator,
Micrometer).

## Decision

**Delivery: one OCI image per deployable.**

- **Service image** with Spring's `bootBuildImage` (Cloud Native Buildpacks, no Dockerfile):
  `./gradlew bootBuildImage` builds `miravelo/cibseven-embedded-example:<version>`
  (`imageName` in `service/app/build.gradle.kts`, JVM pinned with `BP_JVM_VERSION=21`). The image
  is layered and runs as non-root. It is self-contained: the REST API, the embedded engine, the
  CIB seven webapps, `/engine-rest` and `/v3/api-docs` are all served on port 8081.
- **Frontend image** from `frontend/Dockerfile`: a Node stage builds the SPA, an nginx stage serves
  it and proxies the backend paths to the service (`frontend/nginx.conf`).
- **Full stack in containers:** `stack/docker-compose.full.yml` runs Postgres, the service image and
  the frontend image (frontend on 8090). `stack/docker-compose.yml` runs only Postgres for the dev
  loop.
- **Configuration is environment-overridable.** `application.yaml` keeps dev defaults, and the
  datasource is read from `SPRING_DATASOURCE_URL`, `_USERNAME` and `_PASSWORD`.

**Operability: Actuator and Prometheus.**

- `spring-boot-starter-actuator` and `micrometer-registry-prometheus` (`defaultService` bundle in
  `gradle/libs.versions.toml`). Exposed: `health`, `info`, `metrics`, `prometheus`; nothing else.
- Health probes are on: `/actuator/health/liveness` and `/actuator/health/readiness`.
- `springBoot { buildInfo() }` fills `/actuator/info` with the build information.
- No engine-specific health contributor: the engine uses the Spring datasource, which the built-in
  `db` indicator covers.
- `show-details: never`, because the example has no authentication and `/actuator` is reachable by
  anyone.

The how-to (building the images, the Podman socket) is in [CONTRIBUTING.md](../../CONTRIBUTING.md).

## Consequences

- **Positive:** a fork can run the whole stack as containers and plug it into an orchestrator and a
  Prometheus scrape without writing code. There is no service Dockerfile to keep in sync with the JDK.
- **Negative / trade-offs:** with Podman the buildpack step needs a Docker-API socket. `/actuator` is
  open and component health detail is hidden.
- **Not covered:** the images are not production-hardened: they carry the example `jwtSecret` and the
  admin/admin credentials from `application.yaml`, which a real deployment must override. No CI job
  builds or publishes images; that is deferred. OpenTelemetry tracing is deferred. Securing
  `/actuator` (authentication or a separate management port) is left to the fork. Hosting is not
  decided.
