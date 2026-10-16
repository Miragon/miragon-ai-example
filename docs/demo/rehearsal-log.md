# Rehearsal log: demo script, 2026-10-15

This is a record of one full rehearsal of the [demo script](demo-script.md), run from a fresh clone of
the branch (including the not yet committed `docs/` files). It lists what was checked and what was
still open. It does not repeat the script.

> **Current state:** the entries below up to "Limitations found" describe the process and the
> setup of 2026-10-15, before the frontend. Since 2026-10-16 the process, the demo and the stack
> are different; read the newest entry at the end of this file first.

Who and how: an AI agent (Claude) ran the rehearsal on Thomas's machine. Browser steps ran in
headless Chrome, driven over the DevTools protocol: page loads, button clicks and form input were
scripted, and each step was screenshotted. The screenshots are not in the repository. No human used
a mouse, so hovering, dragging and opening new tabs were not tried.

Status: ✅ verified · ⚠️ partially verified · ❌ not possible yet (needs the designIQ App and the merge)


## Environment

- macOS 27.0.1 (arm64), Temurin JDK 21.0.12.1, Docker 29.8.2 with Compose v5.6.0,
  Node.js 26.11.1 (npm 11.20.0), Chrome 155 (headless), gh CLI.
- Bruno CLI 4.0.0, `@miragon/design-iq-validator@5.3.0`,
  `ghcr.io/miragon/designiq-live-host:latest` = v5.3.0 (revision `ebd71c2`, digest `sha256:81a0d451…`).
- The Gradle, npm and Docker caches were already filled. On a new machine the first build, the first
  `npx` and the first image pull also have to download, which this rehearsal did not time.


## Summary

- The live part (README build, start and demo data, then every step of "Run it live") worked as
  written.
- In a local designIQ, all models opened. That includes the canvas, which earlier checks had not
  opened in a browser.
- Todo badges, the hosted workspace and every link into `docs/` on `main` can only be checked after
  the GitHub App is installed and the branch is merged. The checklist is below. **This is the open
  point for Thomas:** an organisation admin installs the App, then the hosted steps are rehearsed.
- The ⚠️ points led to changes in the script, the README, the models and the contract. They are
  listed, with their re-checks, under "Changes after the rehearsal".


## README "Build, test, start" and "Demo data and reset"

- ✅ `./gradlew build`: `BUILD SUCCESSFUL in 1m 17s`, 117 tests in 46 classes, 0 failures.
  `openapi/openapi.json` and the generated process API did not change.
- ✅ `docker compose -f stack/docker-compose.yml up -d`: `bikeleasing-postgres` (postgres:18.6) on :5432.
- ✅ `./gradlew bootRun`: `Started CibsevenBikeLeasingApplicationKt in 3.777 seconds`. Flyway
  applied V1 and V2, and the health check returned `{"status":"UP",...}`. The engine deployed
  `bikeLeasingProcess` v1 and the decision `checkCreditRating`, both packaged from
  `docs/use-cases/bike-leasing/process/`.
- ✅ `npx --yes @usebruno/cli@4.0.0 run 00-demo-data --env local`: `✓ PASS`, 10 requests,
  23/23 assertions, 2.7 s. Result: 3 running instances, 1 completed (the rejected case), and 1 task
  "Clarify alternative with customer".
- ✅ `npx --yes @usebruno/cli@4.0.0 run . --env local -r`: `✓ PASS`, 56 requests,
  115/115 assertions, 25 s.
- ✅ Reset: stopped `bootRun` with SIGINT (the same as Ctrl+C), ran `down -v`, `up -d` and `bootRun`
  again. Result: 0 instances and no Tasklist filter. `00-demo-data` passed again.
- ⚠️ The README's demo-data block runs `cd bruno` but never `cd ..`. The reset commands that follow
  (`docker compose -f stack/...`) then fail if they are run in the same terminal. The README also
  has no filter step, so a reader who follows only the README sees "No filter available." in the
  Tasklist (checked after the reset). The demo script has both steps.


## Demo script: "Before the demo"

- ✅ Creating the Tasklist filter with the `filter/create` call returned the new filter with its
  `id`. Without it, the Tasklist shows "No filter available." and "Add a simple filter".
- ✅ `08-incident-demo`: `✓ PASS`, 6 requests, 13/13 assertions, 22.5 s. The incident
  "Bike dealer API unavailable for BIKE-FAIL" appeared on "Order bike from dealer". The log shows
  the three attempts about 11 s apart.
- ✅ Smoke check (local rows): health `UP`. After the demo data, 3 running instances; the Cockpit
  process list later showed "MiraVelo Bike Leasing" with 5 (the 3, plus the incident case and the
  live request) and 1 incident. "All tasks" had 1 task, and Swagger UI showed five operations.
- ❌ Smoke check (designIQ rows): the repo, the "Todos" count and the GitHub todo filter are covered
  in the checklist below. The GitHub filter itself works: 11 open issues with `todo` and
  `process:bike-leasing`.


## Demo script: steps 1 to 6

**Step 1, context and glossary**

- ⚠️ In the local designIQ, `/f/docs/use-cases/bike-leasing/bike-leasing.md` (since renamed to
  `bike-leasing-use-case.md`) and `/f/docs/glossary.md` open as text. Every section the script names exists: "The problem" with six
  points, "Actors", ACTIVE/REJECTED/CANCELLED, "Say this, not that", "Do not mix up" and
  "One term, one name".
- ❌ GitHub rendering of the glossary: the file is not on `main` yet.

**Step 2, Wardley Map**

- ✅ `/f/docs/architecture/sourcing-map.owm` opens in the Wardley modeler. It shows the anchors
  Customer, Back office and Operations, the Doctrine note, the four numbered "Open: … See #8…#11"
  annotations, the pipeline (Own inbox `build`, CIB seven Tasklist `buy`) and the assumption note.
- ✅ Selecting "Contract & e-signature" shows the context-pad entry
  "Open link (https://github.com/miragon-blueprints/miragon-ai-example/issues/8)". It is the last
  arrow icon in the pad, and its label only appears as a tooltip. In headless Chrome it was not
  clicked through to a new tab.
- ⚠️ The map shows Database as `outsource`, but script item 4 lists it under "Buy".
- ❌ The link of "Leasing process model" points to `main` and returns 404 until the merge.

**Step 3, t.BPM and feature map**

- ✅ `/p/bike-leasing-tbpm`: the "Workshop" toggle is on; the role stickies Customer, Dealer and
  Back office are shown, plus one note; the happy path carries the labels from the script.
- ⚠️ In the t.BPM model, the merge gateway before "Send rejection" (`gateway_rejectionJoin`) has no
  name, so the "Rejection reason" label the script mentions is not shown there. It is shown in the
  executable model.
- ✅ `/p/bike-leasing-feature-map`: the "Workshop" toggle is on. Six core groups carry the label
  "Core · runs today: …". The groups #1 to #4 carry their labels and stages, #4 sits inside #3, and
  all 12 stickies are drawn: notes #1 to #7, decisions #8 to #11, and the question in #4.
  Screenshot size 1600×1000: the #6 sticky at the pool corner sits partly under the palette.
- ❌ Live co-editing with presence: not testable locally, because the local host has only one user.

**Step 4, C4**

- ⚠️ Both files exist, with "Four questions behind the picture" and "From process elements to
  containers". In designIQ they are raw text, as the script says. Mermaid on GitHub cannot be seen
  until the merge. The PNG fallbacks are in `docs/assets/`.

**Step 5, executable process**

- ✅ `/p/bike-leasing` draws the core model. On a 1600×1000 window, the start event sits partly
  under the palette, so drag the canvas a little to the right.
- ❌ The "Todos" panel shows "todo tracking is not configured (requires platform credentials)"
  in the local designIQ.
- ✅ `/f/docs/use-cases/bike-leasing/process/checkCreditRating.dmn` opens on the decision-diagram
  view. One click on the table icon of "Check Credit Rating" opens the table: hit policy "First",
  four rules (`< 18`, `< 1500`, `> 99`, else solvent). The script does not mention that click.
- ✅ Swagger UI, `POST /api/bike-leasing` with Anna's body ("Try it out", "Execute"): `200`,
  `applicationId` `87448b86-…`. The log then shows "Notifying anna@example.com: Please review and
  sign your leasing contract".
- ✅ Engine REST and Cockpit: the token is on the event-based gateway after "Send contract" (14-day
  timer and `miravelo.contractSigned` subscription), and the business key is the `applicationId`.
- ✅ Swagger, `sign-contract`: Swagger shows the status as "202 Undocumented".
- ✅ The token then sits on "Clarify alternative with customer". The recorded path was:
  `… → serviceTask_orderBike → event_bikeUnavailable → userTask_clarifyAlternative`.
- ✅ Tasklist, "All tasks": "Claim", then the tick box. "Alternative bike id*" appeared, pre-filled
  with `BIKE-OOS`. It was replaced with `BIKE-900`, then "Complete" was clicked.
- ✅ Swagger, `GET`: `status` `ORDERED`, `bikeId` `BIKE-900`, `orderId` `ORDER-fb2629c4-…`.
- ✅ Optional: the incident shows in Cockpit. `report-handover` returned 202 (`HANDED_OVER`).
  The "Fire a timer" commands from the script, copied as written, returned `204`, and the status
  became `ACTIVE` (instance `COMPLETED`).
- ✅ The webclient `/webapp` login works with admin/admin, and it lists the task under "All tasks".

**Step 6, from a feature to its issue**

- ✅ Issue #1 has "Today (core)", "Desired behaviour", "Acceptance criteria" and "Feature map".
  `gh issue view 1 … | head -7` prints the anchor block with `serviceTask_sendContract` and
  `gateway_awaitSignature`.
- ✅ Issue #8 has the options Make, Buy and Adapt, the decision criteria and the assumptions.
  Its anchors are "Send contract", "Contract signed" and "Cancel contract".
- ✅ Anchors of all 11 issues compared with the executable model: every element id exists.
  "Send contract" → #1, #8, #9. "Order bike from dealer" → #2, #4, #7, #10. #6 has no element, so it
  is a process-level todo. This matches the script.
- ❌ Badges, the filtered panel and the element chip in designIQ: these need the hosted designIQ.


## designIQ local rehearsal

The commands under "Local designIQ fallback" in the script were run as written, from the repo root,
with one change: `-d --name designiq-rehearsal` instead of `--rm` in the foreground. The host logged
"Live Host ready" for `miragon-blueprints/miragon-ai-example` (local content, static repo).

- Models served: 34. These are 5 BPMN, 1 DMN, 1 Wardley Map and 27 Markdown; with this log there are
  28 Markdown files.
- URL patterns that work, under `http://localhost:8301/r/miragon-blueprints/miragon-ai-example`:
  - the repo page itself: folders, with "Load latest from main", "Release" and "New".
  - `?dir=use-cases/bike-leasing`: one folder.
  - `/p/<model id>`: the BPMN modeler (`bike-leasing`, `bike-leasing-tbpm`,
    `bike-leasing-feature-map`, `original-bike-leasing`).
  - `/p/bike-leasing?element=serviceTask_orderBike`: opens the model with that element selected.
  - `/f/<path from the repo root>`: any file. `.owm` opens the Wardley modeler, `.dmn` the decision
    modeler, `.md` the text view.
- Seen in the toolbar: "Workshop", "Diagram | XML", "Todos", "Add todo", "Analyse with AI"; for the
  DMN, "Checks" and "Simulate". "Release → PR", "Discard & load latest" and "Done" exist in the
  5.3 web app but were not clicked.
- Todos: none, because the host has no GitHub connection (message above).
- Validator: the full run gives `12 error(s), 5 warning(s) — FAIL`, as the script says. Per model:
  `bike-leasing` has 3 errors (exit 1); `checkCreditRating`, `sourcing-map` and `glossary` have none;
  `bike-leasing-tbpm` has 1 warning; `bike-leasing-feature-map` has 2 warnings. The hosted dry run
  `validate_model` of `sourcing-map.owm` returned `ok`.
- Nothing was saved: the mounted copy had no changes afterwards.
- Not tried: running without `GITHUB_REPO`. Assumption, based on the designIQ configuration docs: the
  repo would then be listed as `Miragon/design-iq`, so keep the variable.


## Changes after the rehearsal

Made on 2026-10-15 after the run above, then re-checked on the same machine. The hosted designIQ
steps are still not rehearsed.

- **Demo data and filter.** `00-demo-data` has a new last request, `11-ensure-tasklist-filter`, that
  creates the Tasklist filter "All tasks" only if it is missing. The README commands now end with
  `cd ..`. Re-checked on a fresh database: `✓ PASS`, 11 requests, 26/26 assertions; a second run
  passed too and left one filter, which counted the open clarification tasks. The full collection
  passed: 57 requests, 118/118 assertions.
- **Swagger shows 202.** The contract now documents 202 for sign-contract, clarify-alternative and
  report-handover, plus the 400, 404 and 409 problem responses. Re-checked live: 409 and 400 come
  as `application/problem+json`, and an unknown application on `GET` is a 404 without a body. The
  script no longer needs the "Swagger documents 200" caveat.
- **"Rejection reason" on the workshop canvas.** The merge gateway in the t.BPM model and the
  feature map now has that name, as in the executable model. Both PNGs were rendered again.
- **Feature map.** A 13th sticky, a note under #1, says the 7-day timer on "Send contract" is
  workshop shorthand. The #8 and #4 stickies moved off the group edges. The validator now reports
  "13 stickies remain (1 open question)".
- **Wardley Map.** Annotation 5 shows the Adapt option of #11 (the Tasklist with our own form), and
  a note marks "managed PostgreSQL" as an assumption. Script item 4 now says `outsource`. The
  validator and the hosted `validate_model` dry run report no findings.
- **C4 component view.** The package boxes are blue now, as the text says; the PNG was rendered
  again.
- **Use case renamed** to `bike-leasing-use-case.md`, so the model id `bike-leasing` names only the
  executable BPMN.
- **Script.** A run sheet on top; step 1 and step 4 now show designIQ (repo page, deep link to an
  element); the DMN tab with the table-icon click; hints for the terminal errors, the two tasks of
  the same name, the deprecation banner, the webclient language, the "Open link" icon and the
  canvas under the palette.
- **Checks after the changes:** `./gradlew build` passed (117 tests in 46 classes, 0 failures);
  `npm run lint:bpmn` is clean; the designIQ validator still gives `12 error(s), 5 warning(s)`, all
  known. Every relative link in the 33 Markdown files outside `.context/` resolves (291 links; the
  only exception is the placeholder `xxxx-....md` in the ADR template). All 393 element-id
  citations exist in a model, and every anchor of issues #1 to #11 exists in `bike-leasing.bpmn`.


## Only after the App is installed and the branch is merged

Checklist for Thomas:

- [ ] The App "Miragon AI" is installed for `miragon-blueprints`. On 2026-10-15,
      `gh api orgs/miragon-blueprints/installations` listed only miragon-release-please, claude and
      miragon-blueprints-backup.
- [ ] The branch is merged and `main` has `designiq.yml` (404 on 2026-10-15).
- [ ] A designIQ workspace lists the repo; note its URL as `<designIQ>`. On 2026-10-15 the known
      workspace listed 36 repos, all owned by Hafflgav.
- [ ] The repo shows 35 models, including `bike-leasing`, `sourcing-map` and `checkCreditRating`.
- [ ] `bike-leasing`: "Todos" shows 11 and the elements carry badges. "Order bike from dealer"
      narrows to #2, #4, #7 and #10; "Send contract" to #1, #8 and #9. The chip of #1 reveals
      `gateway_awaitSignature`.
- [ ] "Check credit rating" links to `checkCreditRating`.
- [ ] Links that point to `main` resolve. These are the Wardley link "Leasing process model" and
      the links from the issues into `docs/`; the 6 checked in #1 and #8 all returned 404.
- [ ] GitHub renders the glossary tables, the C4 Mermaid diagrams and the PNGs in `docs/assets/`.
- [ ] Co-editing with a second person: a sticky appears in both windows, with presence avatars.
- [ ] Login to the hosted workspace works for the presenter, and the designIQ rows of the smoke
      check pass.


## Limitations found

- **The terminal shows expected errors.** The "Bike unavailable" path logs
  `ERROR … Error while processing CIB seven task 'serviceTask_orderBike'` with a 92-line stack trace
  right after the signature. Each BIKE-FAIL retry of the incident demo prints about 300 lines.
  Clear the terminal before step 5, and say that the red trace is the BPMN error "Bike unavailable".
- **Deprecation banner.** Cockpit and Tasklist show a red banner: "This version of the web
  interface is deprecated and no longer supported."
- **Two tasks with the same name.** After the demo data and Anna's request, "All tasks" holds two
  "Clarify alternative with customer" tasks with no customer name. Anna's task is the newest one, at
  the top ("Created a few seconds ago"). The webclient shows the business key on each task.
- **The webclient follows the browser language.** On this machine (`de-DE`) it appeared in German.
  Switch it to English with the globe icon.
- **Small UI points.** Swagger UI showed "202 Undocumented" (fixed since). The DMN needs a click on
  the table icon. "Open link" is an icon in the Wardley context pad, not a text label.
- **No human-like testing.** Clicks were scripted in headless Chrome. Opening the issue in a new tab
  from "Open link", and co-editing, were not tried.


## 2026-10-16: new process and frontend, script not rehearsed yet

What changed: the executable process was rebased onto a simplified
event-subprocess model ([ADR-0017](../adr/0017-rebase-onto-the-simplified-event-subprocess-process.md)),
a React frontend was added ([ADR-0009](../adr/0009-react-frontend-next-to-the-service.md)), and the
back office works both user tasks in the frontend inbox ([ADR-0010](../adr/0010-back-office-task-ui-own-inbox-in-the-frontend.md)).
The [demo script](demo-script.md) was rewritten for it. A review then added the status
`CONTRACT_SIGNED`, stricter state checks (404 for unknown ids, 409 for a forbidden state, a task
that is not open or a concurrent update) and removed the Tasklist filter from `00-demo-data` and the
Tasklist link from the frontend (commit `9d99c76`).

Much of the 2026-10-15 entry above no longer applies: there is no DMN deployed, no 14-day timer, no
"Clarify alternative with customer" task, no Tasklist form, no `BIKE-OOS` and no `HANDED_OVER`; the
Bruno folders were renumbered (the incident demo is `06-incident-demo`). Those entries stay as they
were, as a record of that day.

Checked on 2026-10-16 by the AI agents (Claude) that made the change, on the development machine:

- ✅ `./gradlew build pitest` green: mutation score 90 % (gate 80). After the later review fixes
  (`86d38c6`, `52b5e6b`) `./gradlew test` is green again.
- ✅ Bruno, full collection against the running stack (`run . --env local -r`, which includes
  `00-demo-data`): 66/66 requests, 137/137 assertions.
- ✅ Playwright, chromium, against the live stack and the dev server: 5/5 scenarios (happy path,
  invalid application, not solvent, withdrawal, application list), also with `--repeat-each=3`
  (15/15).
- ✅ designIQ validator: `sourcing-map` reports no findings. The architecture files, the C4 PNGs and
  the sourcing-map PNG were refreshed; the map PNG was exported from the Wardley Maps Modeler web
  app ("Export · PNG (2×)").

Not rehearsed yet:

- ❌ The new demo script, step by step, by a person in a browser.
- ❌ Every hosted designIQ step: the App installation and the merge to `main` are still open, so the
  checklist "Only after the App is installed and the branch is merged" above still applies, with
  these updates: the todos are now #1, #3, #4, #7, #18, #19, #20, #21 and #8, #9, #10 (11 issues);
  "Send contract" carries #1, #3, #8, #9 and #18; "Check credit rating" carries #19; there is no
  DMN in `process/`, so no business-rule link to check.

