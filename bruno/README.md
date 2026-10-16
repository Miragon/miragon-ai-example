# REST scenarios

A [Bruno](https://www.usebruno.com) collection that drives the bike-leasing process end to end
against a **running** service. Domain endpoints trigger the business actions (including the two
back-office tasks); the CIB seven `/engine-rest` API reads process instances, history, jobs and
incidents, and resets the retries of the failed job in the incident demo. The tasks, events and
the order step are asynchronous continuations, so each step polls for the wait state it needs
(`pollApp`, `pollEngine`, `pollActivity` in `collection.bru`, see ADR-0012) instead of sleeping. CI
runs the collection on every pull request. Run the commands from this folder.

```bash
npx --yes @usebruno/cli@4.0.0 run . --env local -r
```

| Folder | Scenario |
|---|---|
| `00-demo-data` | leaves six cases in demo-friendly states: one waiting for the credit check, one `CONTRACT_SENT` (waiting for the signature), one `ORDERED` (waiting for the handover), one `ACTIVE`, one `REJECTED` and one `WITHDRAWN` waiting for the reversal |
| `01-happy-path` | submit → credit check (solvent) → sign the contract → insurance policy ‖ bike order → report the handover → `ACTIVE` |
| `02-invalid-application` | an applicant under 18 and a bike outside the catalogue (`BIKE-X`) fail *Validate application* (`applicationInvalid`) and end `REJECTED` without reaching the back office |
| `03-not-solvent` | the back office decides "not solvent" and the application ends `REJECTED` without a contract |
| `04-withdrawal` | the customer withdraws while the contract waits for the signature: the event subprocess cancels the main flow, signing now answers 409, the back office completes the reversal and the application ends `CANCELLED` |
| `05-invalid-request` | invalid input is refused with a 400: no income, an invalid email, a credit check without a decision, an invalid application id, an invalid page size |
| `06-incident-demo` | the dealer is down for `BIKE-FAIL`: the order job runs out of retries and raises an incident (about 30 s) |

Run `00-demo-data` on its own to prepare a live demo: the frontend list and the inbox at
`http://localhost:5173` then show cases in every state, and Cockpit at `http://localhost:8081/camunda`
(admin/admin) shows the running instances. Every run adds six new cases; reset the database
(`docker compose -f stack/docker-compose.yml down -v`) for a clean demo:

```bash
npx --yes @usebruno/cli@4.0.0 run 00-demo-data --env local
```

To start again from an empty engine, follow "Bruno scenarios and demo data" in
[CONTRIBUTING.md](../CONTRIBUTING.md).

## 📮 Start a case by hand

`POST http://localhost:8081/api/bike-leasing`

```json
{ "customerName": "…", "email": "…", "age": 35, "monthlyNetIncome": 3500, "bikeId": "BIKE-900" }
```

*Validate application* rejects applicants under 18 and bikes outside the catalogue
(`GET /api/bikes`: `BIKE-900`, `BIKE-800`, `BIKE-600`, and `BIKE-FAIL`, whose order fails as if the
dealer were down). The credit rating is a human decision, not a rule.

The other actions on a case:

| Call | Effect |
|---|---|
| `GET /api/bike-leasing?status=&page=&size=` | a page of applications, newest first |
| `GET /api/bike-leasing/{id}` | the application as the domain sees it (status, bike, contract, policy and order ids) |
| `GET /api/tasks` | the open back-office tasks (`CREDIT_CHECK`, `REVERSAL`) with their application |
| `POST /api/bike-leasing/{id}/credit-check` | `{ "solvent": true }` or `{ "solvent": false }`; 409 if no credit check is open, 404 for an unknown id |
| `POST /api/bike-leasing/{id}/sign-contract` | the customer signs (`CONTRACT_SIGNED`); 409 if the case is not waiting for a signature, 404 for an unknown id |
| `POST /api/bike-leasing/{id}/report-handover` | the dealer reports the handover; 409 before the order and the policy are done |
| `POST /api/bike-leasing/{id}/withdraw` | the customer withdraws; 409 once withdrawn or ended, 404 for an unknown id |
| `POST /api/bike-leasing/{id}/reversal` | the back office reverses a withdrawn case; 409 if no reversal is open, 404 for an unknown id |

## 🚨 Incident demo

To teach **transaction boundaries, retries and incidents**, submit a request for the poison bike
`BIKE-FAIL`, approve it and sign: the simulated dealer outage fails the *Order bike from dealer* job,
its retries count down (`R3/PT10S`), and an **incident** appears in the Cockpit to analyze and retry.
The insurance policy from the parallel branch stays issued, while the application stays
`CONTRACT_SIGNED`. `06-incident-demo/` has the requests ready to run. A manual retry fails again:
re-running the job does not fix a systematic error.
