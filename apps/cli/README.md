# HealthForge CLI

The HealthForge CLI is a local, scriptable developer surface for the builder-facing workflows added in Phase 18.

## What it helps with

- create or inspect Brief workflows without using the web UI
- inspect workspace and developer workflow surfaces
- generate repo-aware implementation guidance from approved Briefs
- run synthetic interoperability labs from scripts or CI-like local checks

## Example commands

```bash
node apps/cli/bin/healthforge.js developer:overview
node apps/cli/bin/healthforge.js brief:list
node apps/cli/bin/healthforge.js brief:create --question "What changes do we need for CMS prior authorization workflows?" --context "Synthetic provider EHR planning scenario."
node apps/cli/bin/healthforge.js repo:guide --brief-id brief_example --repo-name HealthForge --workspace-root /Users/ivn/code/HealthForge
node apps/cli/bin/healthforge.js labs:run provider_submission_baseline
```

## Boundary notes

- local-first and demo-safe
- requires a local HealthForge API endpoint
- repo guidance stays grounded in approved Briefs and optional file inventory hints
- does not replace review, approval, or implementation validation
