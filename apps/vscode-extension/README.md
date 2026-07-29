# HealthForge VS Code Extension Prototype

This is a local-development-only VS Code extension companion for HealthForge.

It is designed to bring HealthForge workflows closer to engineers by exposing grounded planning, repo-aware guidance, and synthetic workflow testing directly in VS Code.

## Included workflows

- Create a reviewable Brief draft
- Validate a FHIR JSON example
- Query the FHIR standards assistant
- Open the team workspace and developer workflow overview
- List approved Briefs that are ready for implementation handoff
- Generate repo-aware implementation guidance from an approved Brief
- Run a synthetic interoperability lab from the editor

## Security and boundary assumptions

- The extension expects a local HealthForge API endpoint, defaulting to `http://localhost:8080`
- It sends local actor and organization headers only to the configured local API
- It is not intended for production secrets, production PHI, or enterprise deployment
- It does not store provider credentials or external tracker tokens
- Results preserve reviewer-facing warnings and evidence cues

## Local install and run

From `apps/vscode-extension`:

```bash
npm install
npm run compile
```

Then open the project in VS Code and run:

- `Run Extension`

Use the Command Palette to run:

- `HealthForge: Create Brief Draft`
- `HealthForge: Validate FHIR Example`
- `HealthForge: FHIR Standards Assistant`
- `HealthForge: Open Workspace Overview`
- `HealthForge: List Approved Briefs`
- `HealthForge: Generate Repo Guidance`
- `HealthForge: Run Synthetic Lab`

## Developer notes

- The extension uses simple webview panels and Quick Picks for local workflows
- It assumes the HealthForge API is already running locally
- Repo guidance remains bounded by approved Briefs and local file inventory hints
- This surface is intended for engineering demos, local workflows, and safe builder onboarding
