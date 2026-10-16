---
name: verify-model-visually
description: Review a BPMN model you just edited by running the deterministic nets — bpmnlint and the designIQ validator — and a visual review of the rendered image, then report a combined verdict. Catches invisible/overlapping elements, crossing flows and broken designIQ rules (lint) plus messy, hard-to-read layout that needs human judgement (image). Use after editing a .bpmn model, or when asked to review the diagram, check the layout, or verify the model looks right.
allowed-tools: Read, Bash, Glob
---

# Skill: verify-model-visually

BPMNDI bugs are invisible in the XML and in a diff: a task can execute but never render, a shape can
overlap a neighbour. Run **all nets** and report all of them:

- **bpmnlint** — geometry and wiring of the executable model, exact and fast.
- **designIQ validator** — every model under `docs/` against the designIQ rules
  ([ADR-0016](../../../docs/adr/0016-docs-is-the-designiq-workspace.md)).
- **the rendered image** — spacing, alignment, labels, left-to-right readability.

## Key rules

- **Review the rendered image, not the XML.** Never reason about layout from `<dc:Bounds>`.
- **Report concrete, located issues** (element/flow id and the rule it breaks).
- **If everything passes, say so** and list what each net checked.

## Instructions

1. **Resolve the `.bpmn` file.** The executable model is
   `docs/use-cases/bike-leasing/process/bike-leasing.bpmn`; the documentation models are
   `tbpm/bike-leasing-tbpm.bpmn` and `feature-map/bike-leasing-feature-map.bpmn` under the same use
   case. `original/` is frozen: never review it for changes. Ask if ambiguous.

2. **Run the deterministic nets** (repo root):
   ```bash
   npm run lint:bpmn                                                    # executable models only
   npx --yes @miragon/design-iq-validator@5.3.0 --root . <model-id>     # id = file stem
   ```
   Capture each finding but continue to the visual pass. Missing bpmnlint → `npm ci` once.

3. **Render** into `.context/` (git-ignored):
   ```bash
   npx --yes bpmn-to-image@0.10.0 --scale 2 <path-to-bpmn>:.context/verify/<name>.png
   ```
   A model that won't render is itself a finding.

4. **Read the PNG** and review, in order:
   - **Visibility** — every element drawn; a flow ending in empty space signals a missing shape. *Not*
     a defect: the event subprocess *Application withdrawn* and the error boundary event *Application
     invalid* sit off the main flow by design. t.BPM stickies and feature groups are expected in
     workshop-mode models.
   - **No overlaps** — shapes, labels.
   - **No crossing flows** — flows don't cross each other or run through a shape or label.
   - **Spacing & alignment** — consistent gaps, left-to-right reading. *(judgement)*
   - **Readable labels** — legible, near their element, not clipped. *(judgement)*

5. **Report a combined verdict:** overall result, then **Geometry (bpmnlint)**, **designIQ** and
   **Judgement (image)**, each clean or a table of located findings. If the picture changed, remind
   the user to refresh the matching PNG in `docs/assets/`.

6. **Clean up** `.context/verify/`.
