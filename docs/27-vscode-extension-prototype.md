# VS Code Extension Companion

The HealthForge VS Code extension packages builder-facing HealthForge workflows into a local developer tool.

## Current goals

- bring HealthForge closer to engineers
- execute at least one Brief workflow end to end
- execute at least one FHIR validation workflow
- preserve evidence links and human-review cues
- bridge approved Briefs into repo-aware implementation guidance
- provide a lightweight entry point for workspace and synthetic lab walkthroughs

## Current commands

- create Brief draft
- validate FHIR example
- query FHIR standards assistant
- open workspace overview
- list approved Briefs
- generate repo guidance
- run synthetic lab

## Security assumptions

- local-development only
- no production credentials
- no PHI
- no direct external tracker writeback
- local HealthForge API expected at `http://localhost:8080` by default
- repo suggestions are guidance only and stay bounded by approved artifacts plus visible local file inventory
