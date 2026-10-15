# Demo script: MiraVelo bike leasing, from the table to the running process

For the presenter of the live demo: about 29 minutes, then Q&A. You need no knowledge of the
repository. The run sheet below fits on one page; the steps after it give every click, URL and
command, and the appendix holds the likely questions, the known limits and the fallbacks. What a
rehearsal actually showed is in [rehearsal-log.md](rehearsal-log.md).

> **Open before the first live run (2026-10-15):** the designIQ GitHub App is not installed for the
> repo, and this branch is not on `main` yet. Until both are done, designIQ works only through the
> local fallback in the appendix, and without todo badges. See "Once, days before".


## Run sheet

**Say this first**, it is the guiding question of the demo:

> "How do we get from a shared understanding of a business process to a sound decision on what we
> build, buy or adapt, and to a process that really runs?"

Every step follows the same rule: open question, method, live demo, visible result, transition to
the next question. It is not a waterfall: a buy decision can change the context, so going back a
step is part of the method.

**Three commands**, from the repo root, 30 minutes before (details in "Before the demo"):

```bash
docker compose -f stack/docker-compose.yml up -d    # PostgreSQL on :5432
./gradlew bootRun                                   # own terminal; wait for "Started CibsevenBikeLeasingApplicationKt"
cd bruno && npx --yes @usebruno/cli@4.0.0 run 00-demo-data --env local && cd ..   # four cases + Tasklist filter
```

**Two placeholders.** `<designIQ>` is the designIQ workspace that lists the repo: the hosted URL
once the App is installed (note it here: ______), or `http://localhost:8301` with the local
fallback. `<repo>` is `<designIQ>/r/miragon-blueprints/miragon-ai-example`. BPMN models open at
`<repo>/p/<model id>`, any other file at `<repo>/f/<path>`; the model id is the file name without
its extension.

**Browser tabs**, in demo order:

| Tab | URL | Step |
|---|---|---|
| designIQ repo page | `<repo>` | 1 |
| designIQ use case | `<repo>/f/docs/use-cases/bike-leasing/bike-leasing-use-case.md` | 1 |
| Glossary on GitHub | [docs/glossary.md](../glossary.md) | 1 |
| designIQ Wardley Map | `<repo>/f/docs/architecture/sourcing-map.owm` | 2, 6 |
| designIQ t.BPM model | `<repo>/p/bike-leasing-tbpm` | 3 |
| designIQ feature map | `<repo>/p/bike-leasing-feature-map` | 3, 6 |
| C4 on GitHub | [c4-system-context.md](../architecture/c4-system-context.md), [c4-container.md](../architecture/c4-container.md) | 4 |
| designIQ executable process | `<repo>/p/bike-leasing` | 1, 4, 5, 6 |
| designIQ decision table | `<repo>/f/docs/use-cases/bike-leasing/process/checkCreditRating.dmn` | 5 |
| Swagger UI | http://localhost:8081/swagger-ui.html | 5 |
| Cockpit (admin/admin) | http://localhost:8081/camunda/app/cockpit/default/ | 5 |
| Tasklist (admin/admin) | http://localhost:8081/camunda/app/tasklist/default/ | 5 |
| Open todos on GitHub | https://github.com/miragon-blueprints/miragon-ai-example/issues?q=is%3Aissue+is%3Aopen+label%3Atodo | 6 |

The fallback for every picture: [docs/assets/](../assets/), open in a file viewer.

**The steps**, each with what to open, what to do, what to say and what the audience sees:

```
0  Opening                     1 min   clock  1
   Say the guiding question.

1  Context and glossary        4 min   clock  5
   Open  designIQ repo page, then the use case
   Do    deep link to "Send contract" on the executable canvas
   Say   "One word per thing, from the workshop table to the code."
   Sees  one workspace for every model; the problem; the same word in glossary and canvas

2  Wardley Map                 4 min   clock  9
   Open  sourcing-map
   Do    "Open link" on "Contract & e-signature" (issue #8)
   Say   "Build what sets us apart, buy the standard, adapt on purpose in between."
   Sees  Make left, Buy right, four open decisions, one Adapt

3  t.BPM and feature map       5 min   clock 14
   Open  bike-leasing-tbpm, then bike-leasing-feature-map
   Do    point at the Workshop toggle, then at the stage groups
   Say   "No feature loses its process context."
   Sees  the core as a table-top model, then four feature groups and the decision stickies

4  C4                          2 min   clock 16
   Open  c4-system-context.md on GitHub, then bike-leasing in designIQ
   Do    point at the steps the C4 table cites
   Say   "Everything outside the box we do not build."
   Sees  one box, three dashed partners, each traced to a process step

5  Executable process, live    8 min   clock 24
   Open  bike-leasing, checkCreditRating, then Swagger, Cockpit, Tasklist
   Do    submit Anna (BIKE-OOS), sign, clarify with BIKE-900
   Say   "The diagram the business reads is the file the engine deploys."
   Sees  Anna's case moves from "contract sent" to the human task and on to ORDERED

6  From a feature to its issue 4 min   clock 28
   Open  feature map, issue #1, Wardley Map, issue #8
   Do    click the badge on "Send contract"
   Say   "The link is a GitHub issue, not a slide."
   Sees  #1 on "Send contract"; #8 reached from the map and found on three elements

7  Closing                     1 min   clock 29
   Three things to take home.
```

Running late? In step 4 show only the system context, and skip the parts marked "optional" in
step 5.


## Before the demo

### Once, days before

- [ ] This branch is merged to `main`. designIQ reads the default branch only; anything not on
      `main` is invisible there.
- [ ] The designIQ GitHub App "Miragon AI" is installed for the repo: open
      https://github.com/apps/miragon-ai/installations/new, choose the organisation
      `miragon-blueprints`, then "Only select repositories" and `miragon-ai-example`.
      On 2026-10-15 it was **not** installed, and the personal designIQ workspace did not list
      the repo. Installing it needs an organisation admin.
- [ ] Log in to designIQ and check that `miragon-blueprints/miragon-ai-example` appears. Note its
      URL as `<designIQ>` in the run sheet. Assumption (not verified): an organisation
      installation gets its own designIQ workspace with its own URL, so the URL you know may not
      show it.
- [ ] The 11 issues #1 to #11 are open and carry the labels `todo` and `process:bike-leasing`
      (true on 2026-10-15). Without these labels there are no todo badges.
- [ ] JDK 21, Docker and Node.js 22+ are installed.
- [ ] One full rehearsal with the hosted designIQ; tick off the checklist in
      [rehearsal-log.md](rehearsal-log.md), "Only after the App is installed and the branch is
      merged".

The designIQ labels in this script were checked in a local designIQ 5.3 on 2026-10-15: "Workshop",
"Diagram | XML", "Todos", "Add todo"; on the repo page "Load latest from main", "Release" and
"New"; for the DMN "Checks" and "Simulate". "Open link" is a tooltip on an icon, not a label. Not
checked yet, because they need the hosted designIQ: the todo badges and the todo panel. "Done",
"Release → PR" and "Discard & load latest" exist but were not clicked.

### 30 minutes before

Run the three commands of the run sheet, each long-running one in its own terminal. Keep the
`bootRun` terminal visible: the simulated systems only log, so this is where the audience sees
"Notifying ...: Please review and sign your leasing contract".

`00-demo-data` takes a few seconds. It leaves four cases and creates the Tasklist filter "All
tasks" if it is missing (without a filter the Tasklist and the webclient show "No filter
available" and hide every task). Running it again is safe, but each run adds four more cases.

- Wendy Waiting: waits for the signature.
- Lena Lowincome: rejected by the credit check.
- Olga Outofstock: waits on "Clarify alternative with customer".
- Hank Handover: waits for the handover.

Optional, for an incident in Cockpit: run this at least two minutes before you start. Its last
request re-arms the retries, so the incident disappears and comes back about 30 seconds later.

```bash
cd bruno && npx --yes @usebruno/cli@4.0.0 run 08-incident-demo --env local && cd ..
```

With the local fallback, start it now (appendix, "Local designIQ fallback").

### Two-minute smoke check

| Check | Expected |
|---|---|
| `curl -s localhost:8081/actuator/health` | `{"status":"UP",...}` |
| Bruno `00-demo-data` | `Status ✓ PASS`, 11 requests, 26/26 assertions |
| Cockpit, Processes | "MiraVelo Bike Leasing" with 3 running instances (4 with the incident demo) |
| Tasklist, filter "All tasks" | 1 task: "Clarify alternative with customer" |
| Swagger UI | five operations under `/api/bike-leasing` |
| designIQ repo | the models of `docs/` are listed, including `bike-leasing`, `sourcing-map` and `checkCreditRating` |
| designIQ `bike-leasing` | the "Todos" button shows 11, and elements carry badges (hosted designIQ only) |
| GitHub todo filter | 11 open issues |

### Do not click during the demo

- "Done" in the designIQ todo panel closes the GitHub issue, and its badge disappears.
- "Release → PR" opens a real pull request.
- "Add todo" creates a real GitHub issue.
- A live edit in designIQ, such as a sticky, is saved at once for everyone. Undo it right away.
  To drop all unreleased live edits afterwards, use "Load latest from main" on the repo page, then
  "Discard & load latest". This drops everyone's unreleased edits, not only yours.

### Reset between runs

1. Stop `bootRun` with Ctrl+C.
2. `docker compose -f stack/docker-compose.yml down -v` deletes the database.
3. Start again with `docker compose -f stack/docker-compose.yml up -d` and `./gradlew bootRun`.
4. Load `00-demo-data` again. It also creates the Tasklist filter again, which the reset deleted.


## Step 1: Context and glossary (4 min)

**Open question:** "Which problem does MiraVelo have, and do we all mean the same thing by
'cancelled' or 'withdrawal'?"

**Method:** a use case (problem, actors, outcomes) and a glossary with one name per thing.

**Open:**

- designIQ: the repo page `<repo>`, then the use case
  `<repo>/f/docs/use-cases/bike-leasing/bike-leasing-use-case.md`.
- Then the glossary on GitHub, where its tables render: [docs/glossary.md](../glossary.md).
- Fallback: both files on GitHub:
  [bike-leasing-use-case.md](../use-cases/bike-leasing/bike-leasing-use-case.md).

**Show:**

1. The repo page: the folders of `docs/`. One workspace holds the use case, the glossary, the
   Wardley Map, the workshop models and the BPMN and DMN the engine runs.
2. Use case, "The problem": six things go wrong when no single process owns the case. Each one is
   answered by a rule in today's model, such as the 14-day signature deadline.
3. "Actors": Customer, Back office, Dealer, Operations. The three outcomes: ACTIVE, REJECTED,
   CANCELLED.
4. Glossary, "Say this, not that": customer, not prospect; dealer, not supplier. "Do not mix up":
   the signature deadline (14 days to sign) vs. the withdrawal period (14 days after the handover);
   REJECTED vs. CANCELLED.
5. The naming rule "One term, one name": Send contract → `serviceTask_sendContract` →
   `SendContractDelegate` → `SendContractUseCase`. Show it live: open
   `<repo>/p/bike-leasing?element=serviceTask_sendContract`. The executable model opens with "Send
   contract" selected: the glossary word is the step name, and the id is what the code uses.
   15 seconds, then go back; the canvas returns in step 5.

**Key message:** "Requirements rarely fail on how they are written. They fail on a missing shared
picture. So there is one word per thing, from the workshop table to the code."

**What designIQ contributes here:** one workspace for words and models. The use case and the
glossary are files in `docs/`, in the same Git history as the BPMN the engine runs. designIQ shows
them next to the models, and a deep link lands on the element that carries the glossary's word.

**Audience sees:** one workspace for everything, the problem list, the three outcomes, and "Send
contract" both in the glossary and on the canvas.

**Transition:** "We know the problem and the words. Before we draw the flow: which parts of it do we
build ourselves, and which do we buy?"


## Step 2: Wardley Map (4 min)

**Open question:** "What do we build, buy or adapt?"

**Method:** Wardley Mapping. Place each component by visibility (up) and evolution (right), then
decide on five criteria: differentiation, maturity, strategic relevance, market availability,
integration effort.

**Open:**

- designIQ: model `sourcing-map` (`<repo>/f/docs/architecture/sourcing-map.owm`), drawn in the
  Wardley modeler.
- Fallback: [sourcing-map.png](../assets/sourcing-map.png), and the table in
  [sourcing-decisions.md](../architecture/sourcing-decisions.md) on GitHub.

**Show:**

1. Read the map top-down. The anchors at the top are Customer, Back office and Operations; below
   them is the user need "Bike leasing".
2. Up means visible to the user. Right means more evolved: genesis, custom-built, product,
   commodity.
3. Left, Make (`build`): Leasing application, Credit rating rules, Leasing process model.
4. Right, Buy (`buy`): Process engine (CIB seven), Process monitoring (Cockpit), Application
   framework (Spring Boot). Database (PostgreSQL) is marked `outsource`: bought as a service, an
   assumption, as the note under it says.
5. Dealer integration is a partner (`market`). How we connect to the dealer is still open (#10).
6. Four numbered "Open" annotations: #8 Contract & e-signature, #9 Customer notification,
   #10 Dealer integration, #11 Back-office task UI. #11 is a pipeline with two variants: Own inbox
   (build) and CIB seven Tasklist (buy). Annotation 5 shows what Adapt means here: we use the bought
   Tasklist with our own form.
7. Select "Contract & e-signature", then click the last arrow icon in its context pad (tooltip
   "Open link (https://…/issues/8)"). Issue #8 opens in a new tab. Close it again; it comes back in
   step 6. The earlier arrow icon means "Connect to existing element"; do not use it.
8. Say it openly: the note "Assumption: evolution positions are estimates" is on the map.

**Key message:** "On the left we build what sets us apart. On the right we buy what is standard. In
between we adapt on purpose, like the Tasklist with our own form. And visibility is not importance:
the database is at the bottom, yet nothing works without it."

**What designIQ contributes here:** the map is a plain-text OWM file in Git. designIQ draws it and
lets the team move components live. The four open components carry links to their decision
issues, and "Leasing process model" links to the executable BPMN (that link works only after the
merge to `main`). The map is validated like any other model.

**Audience sees:** the map with four open decisions and one Adapt, and issue #8 reached from a
component.

**Transition:** "We build the process model ourselves. So let's lay that process out together, at
the table."


## Step 3: t.BPM and feature map (5 min)

**Open question:** "How does a lease run, step by step, in words everyone at the table understands,
and where does each planned feature fit?"

**Method:** t.BPM (tangible BPM): model together at a table with a reduced BPMN palette, happy path
first, then the exceptions. The feature map adds the planned features to the same model.

**Open:**

- designIQ: model `bike-leasing-tbpm`, then `bike-leasing-feature-map`. If the start event sits
  under the palette, drag the canvas a little to the right.
- Fallback: [bike-leasing-tbpm.png](../assets/bike-leasing-tbpm.png),
  [bike-leasing-feature-map.png](../assets/bike-leasing-feature-map.png), and the table in
  [feature-map.md](../use-cases/bike-leasing/feature-map/feature-map.md) on GitHub.

**Show, t.BPM model:**

1. The "Workshop" toggle in the toolbar is on: a reduced palette, as on a workshop table.
2. The role stickies Customer, Dealer and Back office.
3. The happy path in plain business steps: Leasing request received → Check credit rating →
   Solvent? → Send contract (with the document "Leasing contract") → Signed within 14 days? →
   Order bike from dealer → Bike available? → Handover reported → Await end of withdrawal period →
   Leasing active.
4. The two exception paths: "Rejection reason" → "Send rejection", and "Clarify alternative with
   customer" → "Alternative found?" → "Cancel contract".
5. Optional, 30 seconds, live co-editing: open the same model in a second window, or ask a
   colleague. Add a question sticky; it appears in both windows at once, with presence avatars.
   Delete it again. (Not rehearsed yet: it needs the hosted designIQ.)

**Show, feature map:**

1. Green groups "Core · runs today: ...": the six parts of the core.
2. Orange groups, each with an issue and a stage: "#1 Signature reminder · Stage 1" above "Send
   contract", "#2 Insurance policy · Stage 2" above the order, "#3 Withdrawal · Stage 3" at the
   bottom, "#4 Bike order cancellation · Stage 3" inside #3.
3. The note stickies of the features without steps of their own: #5 on "Clarify alternative with
   customer", #7 on "Order bike from dealer", and #6 on the pool corner, which means the whole
   process.
4. The decision stickies #8 to #11 sit on the steps they affect. The question sticky in #4 is still
   open.
5. The stage order: Stage 1 is the smallest change. Stage 2 comes before Stage 3, so that the
   withdrawal can also reverse the policy. #4 needs #3.

**Key message:** "The table-top model is the shared language. And no feature loses its process
context: each one knows where in the process it is needed."

**What designIQ contributes here:** t.BPM is ordinary BPMN with one document flag. The Workshop
toggle switches the palette for every participant at once. Stickies and groups are stored in the
BPMN file, so the workshop result is versioned and reviewable. The validator reports open stickies
as "open discussion" ("13 stickies remain (1 open question)" on the feature map), so unresolved
questions stay visible. The t.BPM steps reuse the names and ids of the executable model
([executable-process.md](../use-cases/bike-leasing/process/executable-process.md)).

**Audience sees:** the core as a table-top model, then the same core with four feature groups,
the feature notes and four decision notes.

**Transition:** "We know the flow and the features. Which systems carry them?"


## Step 4: C4 (2 min)

**Open question:** "Which systems carry this process, and where does the process control live?"

**Method:** C4, only as deep as needed: system context, then containers.

**Open:**

- GitHub, where Mermaid renders: [c4-system-context.md](../architecture/c4-system-context.md),
  then, if time allows, [c4-container.md](../architecture/c4-container.md).
- Then designIQ: `<repo>/p/bike-leasing`. (In designIQ the C4 files are raw text; do not show
  them there.)
- Fallback: [c4-system-context.png](../assets/c4-system-context.png),
  [c4-container.png](../assets/c4-container.png).

**Show:**

1. System context: one box in the middle, everyone involved at the edge. The dashed boxes
   (contract system, notification channel, dealer system) are simulated. There is no portal yet;
   the customer uses the REST API.
2. In designIQ, point at the steps the C4 table "From process elements to containers" cites:
   "Leasing request received" comes in over the REST API; "14 days" and "Handover reported" are
   wait states the engine keeps in PostgreSQL; "Clarify alternative with customer" is the Tasklist;
   "Send contract" and "Order bike from dealer" talk to the simulated systems outside the box.

**Key message:** "Everything outside the box we do not build. A make decision pulls a box inside;
buy or adapt turns a dashed box into a real system."

**What designIQ contributes here:** designIQ has no C4 notation and shows Mermaid as raw text; say
so plainly. What it does give: the architecture is traced to steps on the same canvas the business
reads, and every C4 row cites the BPMN element ids it comes from.

**Audience sees:** one box, three dashed partners, and each of them found again as a step of the
process.

**Transition:** "Enough pictures. Does any of this actually run?"


## Step 5: Executable process, live (8 min)

**Open question:** "What already runs, and is the diagram the business reads really what the engine
executes?"

**Method:** executable BPMN and DMN on a process engine, driven over REST.

**Open:**

- designIQ: model `bike-leasing` (`docs/use-cases/bike-leasing/process/bike-leasing.bpmn`), then
  the decision table `<repo>/f/docs/use-cases/bike-leasing/process/checkCreditRating.dmn`.
- Fallback: [bike-leasing-core.png](../assets/bike-leasing-core.png) and the element table in
  [executable-process.md](../use-cases/bike-leasing/process/executable-process.md).

Before this step, clear the `bootRun` terminal (Cmd+K) so the audience sees only Anna's lines.

**Show in designIQ (2 min):**

1. Walk the happy path. Point at "14 days" (the signature deadline), "Bike unavailable" (an error
   raised by the dealer) and "Trigger reversal" → "Cancel contract" (undo the contract).
2. Open the "Todos" panel in the toolbar: 11 open todos. Click the badge on "Order bike from
   dealer": the panel narrows to #2, #4, #7 and #10. (Hosted designIQ only.)
3. Open the `checkCreditRating` tab. It opens on the decision diagram with one box, "Check Credit
   Rating". Click the table icon in the top-left corner of that box: four rules, first match wins.
   Not solvent under 18, over 99, or with a monthly net income below 1500. Optional, not
   rehearsed: "Simulate" in the toolbar tries an input.

**Run it live (5 min), in Swagger UI:**

1. `POST /api/bike-leasing` ("submit-leasing-request-controller"), then "Try it out" with this body,
   then "Execute":

   ```json
   { "customerName": "Anna Audience", "email": "anna@example.com", "age": 35, "monthlyNetIncome": 3500, "bikeId": "BIKE-OOS" }
   ```

   The answer is 200 with an `applicationId`. Copy it. The terminal logs "Notifying
   anna@example.com: Please review and sign your leasing contract".
2. Cockpit, "MiraVelo Bike Leasing": the new instance (its business key is the `applicationId`)
   waits at the gateway after "Send contract", for "Contract signed" or "14 days".
3. Swagger: `POST /api/bike-leasing/{applicationId}/sign-contract`. The answer is 202 Accepted.
   The terminal now shows a red `ERROR … Error while processing CIB seven task
   'serviceTask_orderBike'` with a long stack trace. That is expected: it is the BPMN error "Bike
   unavailable". Point at the INFO line "Manual clarification required for application …" below
   it.
4. Cockpit, refresh: the token sits on "Clarify alternative with customer". The dealer reported
   `BIKE-OOS` as out of stock, which the process handles as the error "Bike unavailable".
5. Tasklist, filter "All tasks". Two tasks are called "Clarify alternative with customer": Olga's
   from the demo data and Anna's. Open the newest one, at the top ("Created a few seconds ago");
   the webclient at `/webapp` would show the business key on each task. Click "Claim". Tick "An
   acceptable alternative was found". The field "Alternative bike id" appears, pre-filled with
   `BIKE-OOS`. **Replace it with `BIKE-900`**, then click "Complete". If you leave `BIKE-OOS`, the
   same unavailable bike is ordered again and the task comes back.
6. Swagger: `GET /api/bike-leasing/{applicationId}` shows status `ORDERED`, bike `BIKE-900` and an
   order id.

**Optional (1 min):**

- Operations: if you ran `08-incident-demo`, Cockpit shows an incident on "Order bike from
  dealer". The dealer outage for `BIKE-FAIL` was retried three times, 10 seconds apart.
- Finish the lease: `POST .../report-handover` (202), then fire the 14-day timer as described in
  the appendix, "Fire a timer". The status becomes `ACTIVE`.

**Key message:** "The diagram the business reads in designIQ is the file the engine deploys. What
runs today is the core. Everything else is a todo on this canvas."

**What designIQ contributes here:**

- One file for business and engine: Gradle packages
  `docs/use-cases/bike-leasing/process/*.bpmn`, and the engine deploys it at start-up.
- A change made in designIQ becomes a release pull request. CI then runs bpmnlint, the model
  validation, the process tests and the Bruno scenarios before it reaches `main`.
- Todos are anchored to element ids and shown as badges.
- The DMN file name equals the decision id, so designIQ links "Check credit rating" to its
  decision table.

**Audience sees:** a request they watched being submitted moves from "contract sent" to the human
task, and on to `ORDERED`.

**Transition:** "There are eleven todos on this canvas. Where do they come from, and how does one of
them become the next feature?"


## Step 6: From a planned feature to its issue (4 min)

**Open question:** "Which features follow, and how does a sticky on the map become work on the
executable model?"

**Method:** feature and decision issues anchored to process elements (designIQ todos).

**Path A, a feature (2 min):**

1. Feature map: the orange group "#1 Signature reminder · Stage 1" above "Send contract".
2. Issue [#1 Remind the customer to sign the contract after 7 days](https://github.com/miragon-blueprints/miragon-ai-example/issues/1).
   Show "Today (core)", "Desired behaviour", "Acceptance criteria" and "Feature map".
3. The anchor block is an HTML comment, so GitHub hides it. Show it in a terminal (read-only):
   `gh issue view 1 -R miragon-blueprints/miragon-ai-example --json body -q .body | head -7`.
   It names `serviceTask_sendContract` and `gateway_awaitSignature`.
4. designIQ, `bike-leasing`: click the badge on "Send contract". The panel shows #1, #8 and #9.
   Click the element chip of #1 for `gateway_awaitSignature`, and the canvas reveals the gateway.

**Path B, an open decision (2 min):**

1. Wardley Map: the annotation "Open: make, buy or adapt? See #8", then the "Open link" icon on
   "Contract & e-signature" (step 2, item 7).
2. Issue [#8 Decide: contract issuing and e-signature](https://github.com/miragon-blueprints/miragon-ai-example/issues/8):
   the options Make, Buy and Adapt, the five criteria, the hypotheses marked as assumptions.
3. Its anchors in designIQ: "Send contract", "Contract signed" and "Cancel contract".
4. Say what happens next, but do not do it: when #8 is decided, an ADR records it, the map gets its
   decorator, the issue is closed, and the badges disappear from the canvas
   ([sourcing-decisions.md](../architecture/sourcing-decisions.md), "When a decision is taken").
   The other decisions are [#9](https://github.com/miragon-blueprints/miragon-ai-example/issues/9),
   [#10](https://github.com/miragon-blueprints/miragon-ai-example/issues/10) and
   [#11](https://github.com/miragon-blueprints/miragon-ai-example/issues/11).

**Key message:** "Every feature knows where in the process it is needed, and every decision knows
which steps it touches. The link is a GitHub issue, not a slide."

**What designIQ contributes here:** todos are plain GitHub issues with the labels `todo` and
`process:bike-leasing` and an anchor block. designIQ pins them to element ids, shows a badge per
element, filters the panel by element, and reveals the element from a chip. The Wardley components
link into the same issues. When an issue is closed, its badge disappears, so the canvas always shows
the open work.

**Audience sees:** issue #1 and its badge on "Send contract"; issue #8 reached from the map and
found again on three BPMN elements.

**Transition:** "That is the whole chain. Three things to take home."


## Closing (1 min)

1. **One language from the table to the code.** The glossary's words are the step names in t.BPM,
   in the executable model and in the code.
2. **Features and decisions keep their place.** Feature stickies and Wardley components point to
   issues, and issues point to BPMN elements.
3. **What the business sees is what runs.** The executable model lives in the design workspace,
   and every change to it travels as a tested pull request.


## Appendix

### Likely questions

- **Is the BPMN in designIQ really the deployed one?** Yes. Gradle packages
  `docs/use-cases/bike-leasing/process/*.bpmn`, the engine deploys it at start-up, and the typed
  process API is generated from the same file
  ([ADR-0016](../adr/0016-docs-is-the-designiq-workspace.md)).
- **What if the business edits the executable model in designIQ?** The release becomes a pull
  request, and CI tests it. designIQ has no properties panel for engine attributes (delegates,
  retries, timer durations), so a developer adds those in the same pull request. Developers also
  regenerate and commit the process API
  ([executable-process.md](../use-cases/bike-leasing/process/executable-process.md), "How to change
  the process safely").
- **Why does designIQ show three errors on the executable model?** They are false positives. A
  compensation handler ("Cancel contract") is linked by an association, not by a sequence flow,
  and the designIQ validator has no exemption for that yet. The engine, bpmnlint and the model tests
  accept it.
- **Why is the process so small?** It was cut to a core on purpose, to fit one demo
  ([ADR-0015](../adr/0015-reduce-the-executable-process-to-a-core.md)). The original had 43 flow
  nodes: open `original-bike-leasing`, or
  [bike-leasing-original.png](../assets/bike-leasing-original.png). Both models show the title
  "MiraVelo Bike Leasing"; the model id tells them apart (`bike-leasing` runs,
  `original-bike-leasing` is the frozen reference). The
  [scope comparison](../use-cases/bike-leasing/original/scope-comparison.md) maps every original
  capability to the core or to an issue.
- **Why does the 7-day reminder hang on an automated task?** On the feature map it is workshop
  shorthand, and a note says so. An automated task ends at once, so the engine needs another
  shape; issue #1 describes the two options.
- **Are the todos stored in designIQ?** No. They are GitHub issues. Closing the issue removes the
  badge.
- **Does designIQ work on branches?** No, only on the default branch. Edits come back as a release
  pull request.
- **Why are the C4 diagrams Markdown and Mermaid?** designIQ has no C4 notation. GitHub renders
  Mermaid, and the files sit next to the models with the same names.
- **Is there a customer portal?** No. Swagger UI and Bruno play the customer and the dealer. #6 and
  #7 add the read models a portal would need, but no issue builds a portal yet.
- **Why CIB seven?** A Buy decision: an open-source engine (the community fork of Camunda 7)
  embedded in the service, with Cockpit and Tasklist included. There is no ADR for it yet.
- **Can several people edit at once?** Yes. Edits are shared live, with presence avatars. A file
  changed both in designIQ and in Git is flagged as a conflict, and the release waits until it is
  resolved.
- **Where does AI come in?** designIQ offers an MCP interface. An AI assistant can read and
  validate the same models and propose edits, and those also reach Git only through a release pull
  request. That is not part of this demo.

### Known limitations and fallbacks

- **designIQ shows Markdown and Mermaid as raw text.** C4 and the glossary tables look like
  source. Show them on GitHub, or the PNGs in [assets/](../assets/).
- **designIQ reads the default branch only.** Without the merge, models are old or missing. There
  is no branch switch.
- **The GitHub App is missing.** The repo is not listed. Install the App (see "Once, days before"),
  or use the local fallback below together with the PNGs.
- **The validator flags the compensation handler.** It reports 3 errors on `bike-leasing` and 9 on
  the frozen original; the full check prints `12 error(s), 5 warning(s) — FAIL`. These are false
  positives, not model faults; the answer is under "Likely questions".
- **Expected warnings.** The feature map: "13 stickies remain (1 open question)" and
  "15 activities". The t.BPM model: "4 stickies remain". Open stickies are the point of a workshop
  model.
- **Todo badges need open issues with the labels `todo` and `process:bike-leasing`.** If a badge is
  missing, check the labels. Reopen an issue closed by mistake:
  `gh issue reopen <n> -R miragon-blueprints/miragon-ai-example`.
- **The timers run 14 days.** The case waits at "14 days" or "Await end of withdrawal period".
  Fire the timer with the commands below, or run Bruno `01-happy-path` or `02-signature-deadline`.
- **The Tasklist needs a filter.** `00-demo-data` creates "All tasks". If you still see "No filter
  available", run `00-demo-data` again, or use "Add a simple filter" in the Tasklist.
- **The form pre-fills the old bike id.** If you complete it with `BIKE-OOS`, the task comes back.
  Replace it with `BIKE-900` first.
- **The terminal shows expected errors.** The "Bike unavailable" path logs a red ERROR with a
  stack trace of about 90 lines; each `BIKE-FAIL` retry of the incident demo adds about 300 lines.
  Clear the terminal before step 5.
- **Cockpit and Tasklist show a red banner** "This version of the web interface is deprecated and
  no longer supported". Say up front that it refers to the old UI; the webclient at `/webapp` is
  the new one.
- **The webclient follows the browser language.** On a German system it appears in German. Switch
  it to English with the globe icon, or use a browser set to English.
- **The feature-map PNG draws the stickies only approximately**, because bpmn-js cannot draw them.
  Use it only as a fallback.

### Fire a timer

Verified on 2026-10-15. Replace `<applicationId>`; the answer must be `204`:

```bash
ID=<applicationId>
PI=$(curl -s "http://localhost:8081/engine-rest/process-instance?businessKey=$ID" | python3 -c 'import sys,json; print(json.load(sys.stdin)[0]["id"])')
JOB=$(curl -s "http://localhost:8081/engine-rest/job?processInstanceId=$PI&timers=true" | python3 -c 'import sys,json; print(json.load(sys.stdin)[0]["id"])')
curl -s -o /dev/null -w '%{http_code}\n' -X POST "http://localhost:8081/engine-rest/job/$JOB/execute"
```

Filter jobs by `processInstanceId`, as above. A `processInstanceBusinessKey` parameter on `/job`
did not narrow the result in the check.

### Local designIQ fallback

Use it if the hosted designIQ is unavailable. It was rehearsed on 2026-10-15 with
`ghcr.io/miragon/designiq-live-host:latest` (v5.3.0, digest `sha256:81a0d451…`): every model of
`docs/` opened, including the canvas, the workshop models, the Wardley Map, the DMN and the deep
links (rehearsal log, "designIQ local rehearsal").

Run it from the repo root, on a copy, because edits write through to the mounted folder:

```bash
DESIGNIQ_USER=Hafflgav   # your GitHub login; this mode has no login, so everyone is this user
rm -rf /tmp/miravelo-designiq && mkdir /tmp/miravelo-designiq
cp designiq.yml /tmp/miravelo-designiq/ && cp -R docs /tmp/miravelo-designiq/
(cd /tmp/miravelo-designiq && git init -q && git add -A && git -c user.name=demo -c user.email=demo@example.com commit -qm snapshot)
docker run --rm -p 8301:8080 -e LIVE_AUTH=none -e LIVE_PUBLIC_URL=http://localhost:8301 \
  -e LIVE_LOCAL_USER="$DESIGNIQ_USER" -e GITHUB_REPO=miragon-blueprints/miragon-ai-example \
  -e LIVE_HOST_CONTENT_DIR=/content -v /tmp/miravelo-designiq:/content \
  -v miravelo-designiq-data:/data ghcr.io/miragon/designiq-live-host:latest
```

Then `<designIQ>` is `http://localhost:8301`, so the repo page is
http://localhost:8301/r/miragon-blueprints/miragon-ai-example. Keep `GITHUB_REPO`: without it the
repo is probably listed under another name (an assumption, not tried). If the start event sits
under the palette, drag the canvas a little to the right. Limits of this mode:

- **No todos.** The "Todos" panel says "todo tracking is not configured (requires platform
  credentials)", so there are no badges. Show the issues on GitHub instead.
- **No release pull requests.**
- **No login.** Everyone is the local user, so live co-editing cannot be shown.

Afterwards, run `docker volume rm miravelo-designiq-data`.
