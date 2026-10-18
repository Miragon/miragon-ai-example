# Validating a model: three nets

BPMNDI defects are invisible in the XML and in a diff: a task can execute but never render, a flow
can run backwards. Run all three nets and report all of them. Also use this after a designIQ release
PR changed a model.

## 1. bpmnlint (executable models)

```bash
npm run lint:bpmn
```

Geometry and wiring, exact and fast. Missing bpmnlint → `npm ci` once.

## 2. designIQ validator (every model under `docs/`)

```bash
npx --yes @miragon/design-iq-validator@5.3.0 --root . <model-id>   # id = file stem, e.g. bike-leasing
```

The executable model and the t.BPM model validate clean. Without a model id the run reports 0 errors
and only the feature map's 2 workshop warnings (open stickies, the 7±2 rule).

## 3. The rendered image

```bash
npx bpmn render <path-to-bpmn> --problems -o .context/verify/<name>.png
```

`--problems` numbers the layout problems bpmn-cli finds in the picture. Its drawing is close to
bpmn-js, not identical: the PNG in `docs/assets/` keeps coming from
`npx --yes bpmn-to-image@0.10.0 --scale 2 <path-to-bpmn>:<png>`. A model that does not render is a
finding. Read the PNG, not `<dc:Bounds>`, and check in order:

- **Visibility:** every element drawn; a flow ending in empty space means a missing shape. Expected off
  the main flow of the executable model: the event subprocess *Application withdrawn* and the error
  boundary *Application invalid*. Stickies and groups are expected in the feature map (workshop mode).
- **Reading direction by meaning:** the flow reads left to right. A flow pointing backwards claims
  "the process steps back"; that is right for a loop, wrong for a merge gateway parked left of what it
  feeds.
- **No overlaps, no crossings:** shapes, labels and flows don't cover each other; flows don't run
  through shapes.
- **Labels:** legible, next to their element, and a gateway's *Yes*/*No* labels match the
  `conditionExpression` of the flow they sit on.
- **Docking:** flows start and end on the shape border, no zero-length segments.

Crop a detail with `sips --cropOffset <y> <x> --cropToHeightWidth <h> <w> <png>` (macOS) instead of
judging a full-width render.

## Report

Overall verdict, then **bpmnlint**, **designIQ**, **image**: each clean or a table of findings with the
element or flow id and the rule it breaks. When fixing, move shape, waypoints and label together, then
rerun all three nets. A defect the image shows but no linter caught is worth a note: it is a missing
lint rule. Delete `.context/verify/` afterwards; remind to refresh the PNG in `docs/assets/` if the
picture changed.
