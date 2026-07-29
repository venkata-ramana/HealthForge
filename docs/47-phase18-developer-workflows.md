# Phase 18 developer workflows

Phase 18 makes HealthForge easier to use from the places engineers already spend time: the editor, terminal, scripts, and external tool wrappers.

## What shipped

- a new developer workflow API surface for builder-facing overview and repo-aware guidance
- a richer VS Code companion with workspace overview, approved Brief browsing, repo guidance, and synthetic lab actions
- a local CLI for brief, workspace, developer, and lab workflows
- a small JavaScript SDK for external tool or demo integrations
- CI-ready validation patterns for the platform API, CLI, SDK, and VS Code extension

## Why it matters

Before this phase, HealthForge had strong web and API workflows but a thinner story for day-to-day engineering habits.

After this phase:

- planning artifacts can be carried into the repo more naturally
- developers can inspect approved Briefs and repo guidance without leaving their tools
- external demos and technical onboarding become easier
- pipeline-friendly smoke checks can prove the developer surfaces still work

## New API surface

### Developer overview

`GET /v1/developer/overview`

Use this to inspect:

- approved Briefs ready for implementation-facing workflows
- supported developer surfaces
- CLI and automation recipes
- delivery guardrails for the builder-facing story

### Repo guidance

`POST /v1/developer/repo-guidance`

Use this with an approved Brief plus optional repository inventory hints to generate:

- implementation focus items
- likely file touchpoints
- automation steps
- traceability reminders

This is intentionally guidance, not autonomous code execution.

## VS Code companion

The extension now supports:

- brief draft creation
- FHIR validation
- FHIR assistant lookup
- workspace overview
- approved Brief listing
- repo-aware guidance generation
- synthetic lab execution

That makes the extension more useful in a real engineering walkthrough because it can bridge from reviewed planning into local implementation context.

## CLI workflows

The CLI now supports commands such as:

```bash
node apps/cli/bin/healthforge.js developer:overview
node apps/cli/bin/healthforge.js brief:list
node apps/cli/bin/healthforge.js repo:guide --brief-id brief_example --repo-name HealthForge --workspace-root /Users/ivn/code/HealthForge
node apps/cli/bin/healthforge.js labs:run provider_submission_baseline
```

This is especially useful for:

- terminal-first demos
- repeatable local smoke checks
- scripts that need HealthForge outputs without driving the web UI

## SDK packaging

The JavaScript SDK is intentionally small.

Its job is to make builder integrations easier by providing:

- local API base URL handling
- actor header defaults
- helper methods for the supported Brief, workspace, developer, and synthetic-lab workflows

This gives technical teams a starter path for embedding HealthForge into custom internal tooling.

## CI and automation patterns

Phase 18 also adds a GitHub Actions workflow that demonstrates the expected automation posture:

- run platform API tests
- compile the VS Code extension
- smoke-test the CLI
- smoke-test the SDK import

This does not claim production delivery readiness. It does make the builder-facing workflow surface more trustworthy and easier to maintain.

## Demo path

If you want to show this phase in a short technical demo:

1. Open the HealthForge UI and confirm there is at least one approved Brief.
2. Run `node apps/cli/bin/healthforge.js developer:overview`.
3. Run `node apps/cli/bin/healthforge.js repo:guide --brief-id <approved-brief-id> --repo-name HealthForge --workspace-root /Users/ivn/code/HealthForge`.
4. Open the VS Code extension and run `HealthForge: Generate Repo Guidance`.
5. Run a synthetic lab from the extension or CLI to show validation-oriented builder workflows.

## Boundaries

- local-first and demo-safe
- grounded in approved Briefs and reviewed artifacts
- no autonomous repo mutation
- no production secrets or PHI support
- human review remains required for real implementation decisions
