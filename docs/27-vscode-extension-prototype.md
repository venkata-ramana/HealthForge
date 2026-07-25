# VS Code Extension Prototype

The Phase 4 VS Code extension prototype packages HealthForge workflows into a local developer tool.

## Prototype goals

- bring HealthForge closer to engineers
- execute at least one Brief workflow end to end
- execute at least one FHIR validation workflow
- preserve evidence links and human-review cues

## Current commands

- create Brief draft
- validate FHIR example
- query FHIR standards assistant

## Security assumptions

- local-development only
- no production credentials
- no PHI
- no direct external tracker writeback
- local HealthForge API expected at `http://localhost:8080` by default
