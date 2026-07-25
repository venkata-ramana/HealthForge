# HealthForge VS Code Extension Prototype

This is a local-development-only VS Code extension prototype for Phase 4.

It is designed to bring HealthForge workflows closer to engineers by exposing a small set of commands directly in VS Code.

## Included prototype workflows

- Create a reviewable Brief draft
- Validate a FHIR JSON example
- Query the FHIR standards assistant

## Security and boundary assumptions

- The extension expects a local HealthForge API endpoint, defaulting to `http://localhost:8080`
- It sends reviewer/admin actor headers only to the configured local API
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

## Developer notes

- The extension uses a simple webview panel for results
- It assumes the HealthForge API is already running locally
- The first version is intentionally thin and local-only
