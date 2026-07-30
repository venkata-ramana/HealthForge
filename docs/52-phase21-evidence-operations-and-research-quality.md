# Phase 21 evidence operations and research quality

Phase 21 strengthens the part of HealthForge that matters most for repeated analyst use: the evidence-to-Brief loop.

## What shipped

- source watchlists, freshness alerts, and re-index recommendations for the public evidence layer
- richer grounded-answer diagnostics for insufficient-evidence scenarios
- snapshot diff and citation freshness/change visibility
- analyst research packs for recurring question sets and reusable evidence workflows
- answer-readiness reporting inside the evaluation dashboard

## Why it matters

Before this phase, HealthForge was already strong for demos, review workflow, and governed planning.

After this phase:

- weak-answer cases are more actionable
- source freshness is easier to inspect
- snapshot changes are easier to explain
- analysts can preserve repeated research patterns more cleanly
- product-readiness conversations have stronger evidence-loop support

## New workflow surfaces

### Evidence diagnostics

`POST /v1/answers`

Grounded answers now include diagnostics that explain:

- whether evidence was sufficient
- how many retrieval results were found
- how to refine the question
- how to improve project context
- what the next best action should be

### Source operations

`GET /v1/source-versions/operations`

This returns:

- monitored source watchlists
- freshness alerts
- re-index recommendations
- summary metrics for stale and superseded sources

`POST /v1/source-versions/watchlists`

This creates or updates a monitored source-family watchlist for an organization.

### Snapshot comparison

`GET /v1/corpus-snapshots/{corpusId}/{corpusVersion}/diff/{againstCorpusVersion}`

This compares two pinned snapshots and highlights:

- added sources
- removed sources
- changed lifecycle/source-version references

### Research packs

`POST /v1/workspace/research-packs`

This adds reusable analyst packs with:

- recurring question sets
- project-scoped or org-scoped research continuity
- scheduled review dates

## UI improvements

The web workspace now makes Phase 21 visible through:

- clearer insufficient-evidence guidance in Brief Studio
- citation freshness and change indicators inside findings
- research-pack creation in Team Workspace
- source operations and watchlists inside Team Workspace
- answer-readiness guidance in the evaluation dashboard

## Boundaries

- source operations are advisory workflow tooling, not automated live web monitoring
- diagnostics improve question refinement, but they do not replace reviewer judgment
- research packs preserve analyst continuity without turning the platform into an unbounded notes system
- all workflows remain bounded to public, synthetic, or non-sensitive content

## Demo path

1. Ask a broad question that may not match current corpus coverage.
2. Inspect the new evidence diagnostics and refinement guidance.
3. Open Team Workspace and show research packs plus source operations.
4. Create a watchlist for a manifest source family.
5. Open the evaluation dashboard and show answer-readiness guidance.
