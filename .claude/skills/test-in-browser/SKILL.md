---
name: test-in-browser
argument-hint: "[issue number or flow, e.g. #18 or 'withdraw while the contract waits']"
description: "Check a user flow in the locally running app with the Playwright MCP browser and record evidence: verify the stack is up, seed demo data with Bruno, act as customer or back office, take a screenshot per step into .context/, watch the console, and give a PASS/FAIL/BLOCKED verdict per acceptance criterion. Use when the user asks to test or check something in the browser, to verify a UI change, or as the browser step of implement-issue. Exploratory evidence only; lasting coverage belongs in a Playwright spec under frontend/e2e."
allowed-tools: Read, Write, Glob, Bash(docker compose *), Bash(podman compose *), Bash(curl *), Bash(mkdir *), Bash(npx --yes @usebruno/cli@4.0.0 *), mcp__playwright
---

# Skill: test-in-browser

Two Playwright tools, two jobs: the **Playwright MCP** (`.mcp.json`) explores a flow and collects
evidence now; the **specs** in `frontend/e2e/` (`npm --prefix frontend run e2e`) guard it from then on.
This skill is the first. The UI is the source of truth, the console is the error channel, the
database is not part of the check.

## 1. Check the prerequisites, don't assume them

```bash
docker compose -f stack/docker-compose.yml ps        # Postgres running
curl -fs localhost:8081/actuator/health              # {"status":"UP"}
curl -fs -o /dev/null -w "%{http_code}" localhost:5173   # 200
```

If one is missing, say which and give the command from CONTRIBUTING.md; start it only if the user
agrees. A missing prerequisite makes the run **BLOCKED**, not FAILED.

## 2. Plan the run

From the issue or the request, list the acceptance criteria and the steps that show each one. Pick
the persona per step: the **customer** (application list, detail page, new application) or the
**back office** (inbox at `/aufgaben`). There is no login. If a step needs a case in a given state,
seed it:

```bash
cd bruno && npx --yes @usebruno/cli@4.0.0 run 00-demo-data --env local
```

## 3. Run it

- Open <http://localhost:5173> in the MCP browser and act like a user: click, type, read.
- After each step take a screenshot to `.context/<issue-or-flow>/NN-<what>.png` and read the console
  messages. Every PASS needs something visible in a screenshot.
- The engine works asynchronously: the pages poll every 2 s. Wait for the visible change, never for a
  fixed time.
- If a screenshot hangs (fonts), open a fresh tab and retry once.
- **Never fix code during the run.** A defect is a finding; fixing it is a separate step the user
  decides on.

## 4. Report

| # | Acceptance criterion | Steps | Verdict (PASS / FAIL / BLOCKED) | Evidence |
|---|---|---|---|---|

Then console errors, unexpected behaviour outside the criteria, and which flows deserve a lasting
spec in `frontend/e2e/` (suggest the file and its steps; write it only when asked).
