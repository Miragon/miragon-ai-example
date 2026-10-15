# REST scenarios

A [Bruno](https://www.usebruno.com) collection that drives the core bike-leasing process end to end
against a **running** service. Domain endpoints trigger the business actions; the CIB seven
`/engine-rest` API completes user tasks, fires timer jobs and reads the history, so the whole flow runs
without real 14-day waits. CI runs the collection on every pull request.

```bash
npx --yes @usebruno/cli@4.0.0 run . --env local -r
```

| Folder | Scenario |
|---|---|
| `00-demo-data` | leaves four cases in demo-friendly states (no timer is fired): one waiting for the signature, one rejected by the credit check (low income), one parked on *Clarify alternative with customer*, one waiting for the handover |
| `01-happy-path` | submit → sign the contract → bike ordered → report the handover → withdrawal period → active lease |
| `02-signature-deadline` | the contract is not signed within 14 days and the application is rejected |
| `03-not-solvent` | the `checkCreditRating` DMN rejects the applicant (under 18) |
| `04-bike-unavailable` | the bike is out of stock, the customer picks an alternative via the domain endpoint, the alternative is ordered and the lease becomes active |
| `05-alternative-declined` | the bike is out of stock and no alternative is found: the contract is revoked by compensation, a cancellation confirmation is sent, the application ends `CANCELLED`, no incident |
| `06-alternative-via-tasklist` | the bike is out of stock and the alternative is chosen the way the Tasklist form does it |
| `07-invalid-request` | invalid input is refused with a 400: a request without income, an accepted alternative without a bike |
| `08-incident-demo` | the dealer is down for `BIKE-FAIL`: the order job runs out of retries and raises an incident |

Run `00-demo-data` on its own to prepare a live demo (Cockpit and Tasklist at
`http://localhost:8081/camunda`, admin/admin):

```bash
npx --yes @usebruno/cli@4.0.0 run 00-demo-data --env local
```

## 📮 Start a case by hand

`POST http://localhost:8081/api/bike-leasing`

```json
{ "customerName": "…", "email": "…", "age": 35, "monthlyNetIncome": 3500, "bikeId": "BIKE-900" }
```

`age` and `monthlyNetIncome` feed the `checkCreditRating` DMN: applicants under 18 or over 99, or with
less than 1500 a month, are not solvent. `bikeId` is the only bike attribute the request and the engine
carry. Availability is decided by the `BikeDealerPort` outbound adapter, a simulated dealer with two demo
ids: `BIKE-OOS` is out of stock (the bike-unavailable branch) and `BIKE-FAIL` makes the dealer call fail
(the incident demo). Every other bike id is in stock.

The other actions on a case:

| Call | Effect |
|---|---|
| `GET /api/bike-leasing/{id}` | the application as the domain sees it (status, bike, contract and order ids) |
| `POST /api/bike-leasing/{id}/sign-contract` | the customer signs; 409 if the case is not waiting for a signature |
| `POST /api/bike-leasing/{id}/clarify-alternative` | `{ "alternativeFound": true, "bikeId": "BIKE-900" }` or `{ "alternativeFound": false }` |
| `POST /api/bike-leasing/{id}/report-handover` | the dealer reports the handover; 409 before the bike is ordered |

## 🔁 Two ways to complete the user task

If the requested bike is out of stock, the `Clarify alternative with customer` user task can be
completed two ways, and both reach the domain:

- a client calls `POST /api/bike-leasing/{id}/clarify-alternative` (`04-bike-unavailable`,
  `05-alternative-declined`), or
- a human completes the Camunda Form in the Tasklist; `06-alternative-via-tasklist` sends the same
  variables through `/engine-rest`.

Either way the chosen `bikeId` becomes a process variable, and the re-order reads it and persists it
on the application. An accepted alternative must name its bike: the form requires the field and the
endpoint answers 400 without it.

## 🚨 Incident demo

To teach **transaction boundaries, retries and incidents**, submit a request for the poison bike
`BIKE-FAIL`: the simulated dealer outage fails the *Order bike from dealer* job, its retries count down
(`R3/PT10S`), and an **incident** appears in the Cockpit to analyze and retry. `08-incident-demo/` has
the requests ready to run. A manual retry fails again: re-running the job does not fix a systematic
error.
