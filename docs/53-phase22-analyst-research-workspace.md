# Phase 22 analyst research workspace

Phase 22 turns HealthForge into a stronger day-to-day analyst environment.

This phase adds:

- reusable question packs for repeated analyst starts
- scenario templates and persona presets for more consistent walkthroughs
- precedent comparison and decision-pattern views across prior Briefs
- bounded research notebooks and evidence-bundle handoff summaries
- topic browsing and cross-workspace discovery search
- reviewer workload cues and escalation summaries

## What is new

### Question packs, templates, and persona presets

The Team Workspace now supports reusable starting points for repeated work:

- question packs with starter prompts and supporting prompt sets
- scenario templates derived from those packs
- persona presets for reviewer, approver, and auditor-style workflows

These make repeated research easier without hiding the actual questions being asked.

### Precedent-aware review

HealthForge now surfaces:

- related Brief comparisons
- theme clusters
- reusable decision-pattern summaries

These are advisory only. They help teams reuse prior work, but they do not replace evidence, citations, or human review on the current Brief.

### Research continuity

Analysts can now create bounded research notebooks that preserve:

- key takeaways
- evidence bundle names
- reviewer-to-approver handoff summaries
- continuity notes for future refresh cycles

This keeps continuity inside the governed workspace instead of pushing it into ad hoc notes.

### Topic discovery and search

The workspace now supports local, organization-scoped discovery over:

- briefs
- findings
- approvals
- evidence references
- reusable workspace artifacts such as question packs and research packs

This makes the product easier to navigate by theme rather than only by individual Brief ID.

### Reviewer operations cues

Phase 22 also adds:

- workload summaries
- SLA-style aging cues
- escalation records

These are informative workflow signals, not punitive productivity scoring.

## API surface

- `GET /v1/workspace/overview`
- `POST /v1/workspace/question-packs`
- `POST /v1/workspace/research-notebooks`
- `POST /v1/workspace/review-escalations`
- `POST /v1/workspace/discovery/search`

## UI surface

The web workspace now makes Phase 22 visible through:

- question pack creation and reusable prompt cards
- scenario template and persona preset panels
- precedent comparison and topic cluster cards
- research notebook creation and handoff summaries
- reviewer operations cards and escalation cues
- cross-workspace search results

## Why this matters

Phase 22 makes HealthForge more useful between demos.

It helps analysts:

- restart repeated work faster
- compare current work with prior reviewed artifacts
- preserve continuity across sessions
- discover related evidence and decisions more easily
- manage reviewer flow with clearer operational signals
