# 0010 — Back-office task UI: own inbox in the frontend

- **Status:** Accepted
- **Date:** 2026-10-16
- **Deciders:** Marco (@emaarco)
- **Decides:** [#11](https://github.com/miragon-blueprints/miragon-ai-example/issues/11)
- **Origin:** Consolidated baseline; earlier history in git.
- **Note (2026-10-18):** PR [#33](https://github.com/miragon-blueprints/miragon-ai-example/pull/33)
  removed `executable-process.md` from `docs/`. The link under Context points to its last
  version, which is no longer maintained. The two human tasks are in the executable model
  [bike-leasing.bpmn](../use-cases/bike-leasing/process/bike-leasing.bpmn).

## Context

The process has two human tasks, `Check credit rating` and `Reverse application`
([executable-process.md](https://github.com/miragon-blueprints/miragon-ai-example/blob/90e4f61475727b1d284e389ffc3a79d630b9e5c6/docs/use-cases/bike-leasing/process/executable-process.md)). Issue #11 asked
where the back office works on them: adapt the CIB seven Tasklist (forms, filters, users and
groups), build an own inbox on the domain API, or buy a task UI. A Tasklist form writes process
variables straight into the engine, past the domain; the service then has to read them back and
trust them.

## Decision

We **build our own inbox** in the frontend ([ADR-0009](0009-react-frontend-next-to-the-service.md)).

- `GET /api/tasks` lists the open tasks of both kinds with the application they belong to. It
  returns no engine task id.
- A task is completed through the domain, keyed by the application id:
  `POST /api/bike-leasing/{id}/credit-check` with `{ "solvent": … }` and
  `POST /api/bike-leasing/{id}/reversal`. The service completes the user task and updates the
  application in one transaction.
- The user tasks carry no Camunda Form. The CIB seven Tasklist and Cockpit stay available for
  operations, not as the back office's workplace.

## Consequences

- **Positive:** task data goes through the domain and its validation. The inbox shows business data
  (customer, income, bike) instead of process variables, in the same UI as the rest of the case.
- **Negative / trade-offs:** we own the inbox. Adding a human task means a new endpoint and an inbox
  entry, not a form.
- **Not covered:** no assignment, claiming, filters or permissions. The example has no
  authentication; whoever opens `/aufgaben` can decide.
