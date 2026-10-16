# Demo script: MiraVelo bike leasing, from the table to the running process

For the presenter of the live demo: about 31 minutes, then Q&A. You need no knowledge of the
repository. Each step says what to open, what to do, what to say and what the audience sees. What a
rehearsal actually showed is in [rehearsal-log.md](rehearsal-log.md).

> **Not rehearsed yet (2026-10-16):** this script was rewritten for the new process and the
> frontend ([executable-process.md][exec], [ADR-0009][a09], [ADR-0010][a10]). The stack and its tests were checked
> on that day; the walk-through and every designIQ step were not. Rehearse once before the first
> live run and record it in the rehearsal log.


## Run sheet

**Say this first**, it is the guiding question of the demo:

> "How do we get from a shared understanding of a business process to a sound decision on what we
> build, buy or adapt, to a process that really runs, and to the next feature an agent can build
> safely?"

**Four commands**, from the repo root, 30 minutes before, each long-running one in its own terminal:

```bash
docker compose -f stack/docker-compose.yml up -d                      # PostgreSQL on :5432
./gradlew bootRun                                                     # service + engine on :8081
npm --prefix frontend ci && npm --prefix frontend run dev             # frontend on :5173
cd bruno && npx --yes @usebruno/cli@4.0.0 run 00-demo-data --env local && cd ..   # six demo cases
```

**Placeholders.** `<repo>` is `<designIQ>/r/miragon-blueprints/miragon-ai-example`, where
`<designIQ>` is the hosted workspace that lists the repo (note it here: ______) or
`http://localhost:8301` with the local fallback in the appendix. BPMN models open at
`<repo>/p/<model id>`, other files at `<repo>/f/<path>`; the model id is the file name without its
extension.

```
step                             min  clock  open
0  Opening                        1     1
1  Context and glossary           3     4   <repo>, use case, glossary on GitHub
2  Wardley Map                    3     7   <repo>/f/docs/architecture/sourcing-map.owm
3  t.BPM and feature map          4    11   <repo>/p/bike-leasing-tbpm, <repo>/p/bike-leasing-feature-map
4  C4                             2    13   c4-system-context.md, c4-container.md on GitHub
5  Executable model               3    16   <repo>/p/bike-leasing, Cockpit
6  Live run in the frontend       8    24   http://localhost:5173, Cockpit
7  From a todo to a feature       3    27   feature map, <repo>/p/bike-leasing, issue #19
8  How an agent implements it     3    30   AGENTS.md, .claude/README.md
9  Closing                        1    31
```

Cockpit: http://localhost:8081/webapp/#/seven/auth/start (admin/admin; the "cockpit" link in the frontend opens it). The fallback for every
picture is [docs/assets/](../assets/). Running late? Show only the system context in step 4, and
skip the parts marked "optional" in step 6.


## Before the demo

**Once, days before:** the branch is merged to `main` (designIQ reads the default branch only); the
designIQ GitHub App "Miragon AI" is installed for the repo by an organisation admin, and the repo
appears in your workspace; the open issues #1, #3, #4, #7, #18, #19, #20, #21 and #8, #9, #10 carry
the labels `todo` and `process:bike-leasing` (#2, #5, #6 and #11 are closed by the merge); JDK 21,
Node 24 and Docker or Podman are installed; one full rehearsal is recorded in the rehearsal log.

**30 minutes before:** run the four commands. Keep the `bootRun` terminal visible: the simulated
systems only log, so this is where the audience sees "Notifying ...: Please review and sign your
leasing contract". `00-demo-data` leaves six cases (running it again adds six more):

| Customer | State | Used for |
|---|---|---|
| Clara Creditcheck | eingegangen (RECEIVED) | a credit check in the inbox |
| Carl Contract | vertrag versendet (CONTRACT_SENT) | the withdrawal in step 6 |
| Otto Ordered | bestellt (ORDERED) | waits for the handover |
| Anna Active | aktiv (ACTIVE) | a finished lease |
| Rita Rejected | abgelehnt (REJECTED) | a negative credit check |
| Walter Withdrawn | zurückgezogen (WITHDRAWN) | a reversal in the inbox |

Optional, for an incident in Cockpit, at least two minutes before you start (the retries take about
30 seconds): `cd bruno && npx --yes @usebruno/cli@4.0.0 run 06-incident-demo --env local && cd ..`

**Smoke check:** `curl -s localhost:8081/actuator/health` says `UP`; the frontend lists the cases
under "anträge" and shows two tasks under "aufgaben" (Clara's credit check, Walter's reversal);
Cockpit lists "MiraVelo Bike Leasing"; designIQ draws `bike-leasing` and, hosted only, its "Todos".

**The frontend speaks German** unless the browser is set to English; switch with "sprache" in the
header. This script names the German labels, with the English ones in brackets.

**Do not click in designIQ:** "Done" in the todo panel closes the GitHub issue, "Release → PR" opens
a real pull request, "Add todo" creates a real issue. A live edit is saved for everyone at once.

**Reset between runs:** stop `bootRun` (Ctrl+C), `docker compose -f stack/docker-compose.yml down -v`,
then `docker compose -f stack/docker-compose.yml up -d`, `./gradlew bootRun` and `00-demo-data`
again. The dev server can keep running.


## Step 1: Context and glossary (3 min)

**Open:** the designIQ repo page `<repo>`, the use case
`<repo>/f/docs/use-cases/bike-leasing/bike-leasing-use-case.md`, then the
[glossary](../glossary.md) on GitHub, where its tables render.

**Do:**

1. The repo page: one workspace holds the use case, the glossary, the Wardley Map, the workshop
   models, the C4 views, the ADRs and the BPMN the engine runs.
2. The use case: the problem, the actors (Customer, Back office, Dealer, Operations) and the
   outcomes: leasing active, application rejected, application cancelled.
3. The glossary: REJECTED (we said no) is not CANCELLED (the customer withdrew and we reversed it).
4. Open `<repo>/p/bike-leasing?element=userTask_checkCreditRating`: the executable model opens with
   "Check credit rating" selected. The glossary word is the step name; the id is what code and
   issues use. Go back after 15 seconds.

**Say:** "Requirements rarely fail on how they are written. They fail on a missing shared picture.
So there is one word per thing, from the workshop table to the code."

**Audience sees:** one workspace, the three outcomes, and "Check credit rating" in the glossary and
on the canvas.


## Step 2: Wardley Map (3 min)

**Open:** model `sourcing-map`. Fallback: [sourcing-map.png](../assets/sourcing-map.png) and
[sourcing-decisions.md](../architecture/sourcing-decisions.md).

**Do:**

1. Top-down: the anchors, then the user need "Bike leasing". Up means visible; right means evolved.
2. Left, Make (`build`): Customer frontend, Back-office task UI, Leasing application, Credit check,
   Leasing process model. Right, Buy: Process engine (CIB seven), Process monitoring (Cockpit), UI
   framework (React, Vite), Application framework (Spring Boot); Database `outsource`, an
   assumption. Partners (`market`): Dealer integration, Insurance policy (an assumption).
3. The three "Open" annotations: #8 Contract & e-signature, #9 Customer notification, #10 Dealer
   integration. Select "Contract & e-signature", then the "Open link" icon in its context pad (a
   tooltip, not a label): issue #8 opens.
4. One decision is taken: the note "Decided (ADR-0010): own inbox". The Tasklist with our own form
   was the Adapt option; the own inbox won, because task data then goes through the domain.
5. Annotation 4: the credit check is manual today. That comes back in step 7.

**Say:** "Left we build what sets us apart, right we buy what is standard. And a decision is a
recorded ADR, not a feeling: here is one that was taken."

**Audience sees:** three open decisions, one decided, and issue #8 reached from a component.


## Step 3: t.BPM and feature map (4 min)

**Open:** `bike-leasing-tbpm`, then `bike-leasing-feature-map`. Fallback: the PNGs and
[feature-map.md](../use-cases/bike-leasing/feature-map/feature-map.md).

**Do:**

1. t.BPM: the "Workshop" toggle is on (a reduced palette, plus role stickies). The happy path:
   Leasing request received → Validate application → Check credit rating → Solvent? → Send
   contract → Contract signed → Issue insurance policy and Order bike from dealer in parallel →
   Handover reported → Leasing active. The exceptions: invalid or not solvent → Rejection reason →
   Send rejection; a withdrawal at any time → Reverse application → Application cancelled.
2. Feature map: the same core with the planned features by stage. Stage 1 – Customer follow-up:
   #18 signature deadline, #1 reminder. Stage 2 – Automation and supply: #19 DMN credit check, #20
   alternative bike. Stage 3 – Withdrawal: #21 withdrawal period, #3 automatic reversal, #4 order
   cancellation. Stage 4 – Portal: #7 live dealer availability. The decision stickies #8 to #10 sit
   on the steps they affect.
3. Where the features come from: the [original process](../use-cases/bike-leasing/original/scope-comparison.md)
   could do all of this. We cut it to a core on purpose and kept the gap as issues.

**Say:** "The table-top model is the shared language, and no feature loses its process context."

**Audience sees:** the core as a table-top model, then the same core with the stages on it.


## Step 4: C4 (2 min)

**Open:** [c4-system-context.md](../architecture/c4-system-context.md), then
[c4-container.md](../architecture/c4-container.md), on GitHub (designIQ shows Mermaid as raw text).
Fallback: [c4-system-context.png](../assets/c4-system-context.png),
[c4-container.png](../assets/c4-container.png).

**Do:** system context: one box; customer and back office use the frontend, operations the Cockpit;
four dashed partners are simulated (contract system, notification channel, insurer, dealer system).
Container view: the frontend (port 5173 in development), the service with the embedded engine
(port 8081), PostgreSQL. The frontend calls only the REST API, through a client generated from the
checked-in contract.

**Say:** "Everything outside the box we do not build. A make decision pulls a box inside; buy or
adapt turns a dashed box into a real system."

**Audience sees:** three containers and four simulated partners.


## Step 5: Executable model (3 min)

**Open:** `<repo>/p/bike-leasing`, then Cockpit. Fallback:
[bike-leasing-core.png](../assets/bike-leasing-core.png) and
[executable-process.md](../use-cases/bike-leasing/process/executable-process.md).

**Do:**

1. The workshop steps, now with engine semantics: the error boundary "Application invalid", the two
   user tasks, the parallel gateway, and the event subprocess "Application withdrawn" that
   interrupts the case wherever it is.
2. Every difference from its source model is listed in `executable-process.md`.
3. Hosted designIQ only: "Todos" lists the open issues; the badge on "Send contract" narrows it to
   #1, #3, #8, #9 and #18.
4. Cockpit, "MiraVelo Bike Leasing": the same diagram, deployed from the same file, with the demo
   cases on it.

**Say:** "One file for business and engine: Gradle packages it, the engine deploys it, and a typed
process API is generated from it. Code that names a removed step no longer compiles."

**Audience sees:** the workshop picture again, now executable and running.


## Step 6: Live run in the frontend (8 min)

**Open:** http://localhost:5173. Clear the `bootRun` terminal first.

**Happy path (4 min):**

1. "neuer antrag" (new application): "Ada Audience", `ada@example.com`, age 35, monthly net income
   3500, bike "Gravel Explorer 900", "antrag absenden". Status "eingegangen" (received).
2. "aufgaben" (tasks): a "bonitätsprüfung" (credit check) for Ada with age, income and bike. Open it
   ("bearbeiten") and choose "solvent". This is our own inbox, not the Tasklist ([ADR-0010][a10]):
   the decision goes through a domain endpoint, not into process variables.
3. Ada's page (via "anträge"): "vertrag versendet" (contract sent); the terminal logs "Notifying
   ada@example.com: Please review and sign your leasing contract".
4. "vertrag unterschreiben" (sign contract): "vertrag unterschrieben" (contract signed) while the
   policy is issued and the bike is ordered in parallel, then "bestellt" (ordered), with contract,
   policy and order numbers on the page.
5. "übergabe melden" (report handover), standing in for the dealer: "aktiv" (active).

**Withdrawal and reversal (2 min):** open Carl Contract, "antrag zurückziehen" (withdraw
application): "zurückgezogen" (withdrawn). In "aufgaben", open Carl's "rückabwicklung" (reversal)
and "rückabwicklung bestätigen" (confirm reversal): "storniert" (cancelled). Say what is manual
today: the back office undoes contract and order by hand; #3 and #4 automate it.

**Optional, an invalid application (1 min):** "neuer antrag" with age 16. The case goes straight to
"abgelehnt" (rejected) without reaching the inbox: "Validate application" raised the BPMN error
"Application invalid".

**Optional, an incident (1 min):** Cockpit shows an incident on "Order bike from dealer" for the
`BIKE-FAIL` case of `06-incident-demo` (bike "Prototype X (dealer outage demo)"). The simulated
outage was retried three times, 10 seconds apart (`R3/PT10S`). The policy from the parallel branch
stays issued, and the application stays "vertrag unterschrieben". A retry in Cockpit fails again: a
systematic error is not fixed by retrying. The stack traces in the terminal are expected.

**Say:** "Customer and back office see one case in one UI, and the process decides what each of
them can do next. Operations keeps the engine's own tools."

**Audience sees:** their own case reach "aktiv", a withdrawn case reversed and cancelled, and
optionally a rejection and an incident.


## Step 7: From a todo to a feature (3 min)

**Open:** the feature map, `<repo>/p/bike-leasing`, then
[issue #19](https://github.com/miragon-blueprints/miragon-ai-example/issues/19).

**Do:**

1. Feature map: #19 "Credit check by DMN" in Stage 2, on "Check credit rating". In the executable
   model (hosted designIQ only) the badge on that step shows #19.
2. Issue #19: "Today (core)", "Desired behaviour", and the open question about a manual fallback.
3. The anchor block is an HTML comment that GitHub hides. Show it read-only:
   `gh issue view 19 -R miragon-blueprints/miragon-ai-example --json body -q .body | head -6`.
   It names `userTask_checkCreditRating` and `gateway_isSolvent`, element ids of the model.
4. The decision table already exists, frozen: `<repo>/f/docs/use-cases/bike-leasing/original/checkCreditRating.dmn`
   (age 18 to 99, monthly net income at least 1500). #19 moves it back into `process/`.

Same shape, if you prefer: #18 (signature deadline), anchored on "Send contract", "Contract signed"
and "Rejection reason".

**Say:** "Every feature knows where in the process it is needed. The link is a GitHub issue with
element ids, not a slide."

**Audience sees:** the click from the human step to the issue that automates it.


## Step 8: How an agent implements it (3 min)

**Open:** [AGENTS.md](../../AGENTS.md) and [.claude/README.md](../../.claude/README.md) on GitHub.
Do not run an agent live unless you rehearsed it; show the files and tell the path.

**Do:**

1. AGENTS.md, "Implementing a Feature": a feature follows its acceptance criteria through every
   layer (model, regenerated process API, code, tests, contract, frontend client and UI, Bruno,
   Playwright), and the feature map, glossary and `executable-process.md` change in the same PR.
2. `.claude/`: workflow skills (`implement-issue` with its plan-approval stop, `automate-process`,
   `review-change`, `test-in-browser`, `create-adr`), layer skills with a production and a test
   pattern each (`create-rest-controller`, `create-persistence-adapter`, `create-feature-slice`,
   `create-delegate`) and two review agents (`review-process`,
   `review-vertical-slice`).
3. The gates that refuse a wrong change: the typed process API, ArchUnit and Konsist, mutation
   testing at 80, the drift-gated `openapi.json` and generated client, bpmnlint, Bruno and
   Playwright against the running stack, all in CI; the designIQ validator runs locally.
4. The path for #19: the agent reads the issue and its anchors; turns the user task into a business
   rule task and moves the DMN; regenerates the process API, so the compiler lists every place that
   still names `userTask_checkCreditRating`; adapts code, tests, contract, client and inbox; runs
   the review agents; opens a PR with `Closes #19`. After the merge the badge leaves the canvas.

**Say:** "The agent is fast either way. The repository decides whether fast is also right: it tells
the agent what a feature touches, and it refuses what breaks the rules."

**Audience sees:** the instructions, the tools and the gates an agent works within.


## Closing (1 min)

1. **One language from the table to the code**: the glossary's words are the step names in t.BPM,
   in the executable model, in the code and in the frontend.
2. **Decisions and features keep their place**: Wardley components and feature stickies point to
   issues and ADRs; issues point to BPMN elements.
3. **What the business sees is what runs, and what an agent changes is checked**: every change to
   the model travels as a tested pull request.


## Appendix

### Likely questions

- **Is the BPMN in designIQ really the deployed one?** Yes: Gradle packages
  `docs/use-cases/bike-leasing/process/*.bpmn`, the engine deploys it, and the process API is
  generated from the same file ([ADR-0008][a08]).
- **What if the business edits the executable model in designIQ?** The release becomes a pull request
  and CI tests it. A developer adds engine attributes and regenerates the process API in that PR.
- **Why is the process so small?** On purpose: a model a business audience reads in one
  look ([executable-process.md][exec]). The [scope comparison](../use-cases/bike-leasing/original/scope-comparison.md)
  maps every capability of the 43-node original to the core or to an issue.
- **Why not the CIB seven Tasklist?** A Tasklist form writes process variables past the domain; the
  own inbox completes tasks through validated domain endpoints ([ADR-0010][a10]). The trade-off: no
  assignment, claiming, permissions or login yet.
- **What does the validator report?** The executable model validates clean. The known errors are on
  the frozen original (compensation false positives); workshop models warn about open stickies.
- **Are todos stored in designIQ?** No, they are GitHub issues; closing one removes the badge.
- **Why CIB seven?** A Buy decision: the open-source community fork of Camunda 7, embedded, with
  Cockpit included. Embedding it in the service is [ADR-0011][a11].

### Known limits

- designIQ shows Markdown and Mermaid as raw text, and reads the default branch only.
- Without the App there are no todo badges: show the issues on GitHub.
- The frontend has no login, and statuses update by polling every two seconds.
- Cockpit shows a red deprecation banner for the old web interface; the new webclient is `/webapp`.

### Local designIQ fallback

From the repo root, on a copy, because edits write through to the mounted folder:

```bash
DESIGNIQ_USER=your-github-login   # this mode has no login, so everyone is this user
rm -rf /tmp/miravelo-designiq && mkdir /tmp/miravelo-designiq
cp designiq.yml /tmp/miravelo-designiq/ && cp -R docs /tmp/miravelo-designiq/
(cd /tmp/miravelo-designiq && git init -q && git add -A && git -c user.name=demo -c user.email=demo@example.com commit -qm snapshot)
docker run --rm -p 8301:8080 -e LIVE_AUTH=none -e LIVE_PUBLIC_URL=http://localhost:8301 \
  -e LIVE_LOCAL_USER="$DESIGNIQ_USER" -e GITHUB_REPO=miragon-blueprints/miragon-ai-example \
  -e LIVE_HOST_CONTENT_DIR=/content -v /tmp/miravelo-designiq:/content \
  -v miravelo-designiq-data:/data ghcr.io/miragon/designiq-live-host:latest
```

Then `<designIQ>` is `http://localhost:8301`: no todos, no release pull requests, no co-editing.
Afterwards, `docker volume rm miravelo-designiq-data`.

[a08]: ../adr/0008-docs-is-the-designiq-workspace.md
[a09]: ../adr/0009-react-frontend-next-to-the-service.md
[a10]: ../adr/0010-back-office-task-ui-own-inbox-in-the-frontend.md
[a11]: ../adr/0011-embedded-cib-seven-with-delegates-and-a-generated-process-api.md
[exec]: ../use-cases/bike-leasing/process/executable-process.md
