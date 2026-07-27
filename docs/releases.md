# HealthForge release notes

This document keeps the detailed delivery history out of the main README and provides a release-style view of product progress.

## Current summary

HealthForge currently includes:

- evidence ingestion, passage extraction, and corpus snapshots
- grounded answer generation over approved public sources
- reviewable Brief workflow with approvals and audit trail
- tenant-aware review boundaries and RBAC-aligned workflows
- deterministic FHIR validation for synthetic or non-sensitive examples
- curated standards and FHIR artifact lookup
- regulation explainer and architecture review workflows
- prior-authorization workflow tools for PAS, CRD, and DTR scenarios
- richer implementation planning exports for payer/provider/shared tracks
- local web UI, API, and VS Code prototype support

## Milestone history

### Foundation

- established problem framing, source-corpus boundaries, review model, and local-first architecture
- created the initial Regulation-to-Engineering Brief workflow
- introduced retrieval, citations, review decisions, approvals, and auditability

### Standards and engineering workflows

- added pinned FHIR validation catalog support
- added curated standards artifact registry
- added synthetic FHIR fixtures and validation demos
- added architecture review and guarded starter artifact generation

### Product workflow expansion

- added FHIR knowledge assistant
- added regulation explainer
- added tracked export preview support
- added prior-authorization copilot workflows

### Enterprise and review hardening

- added organization-aware review boundaries
- added stronger role-aware controls and access review reporting
- added compliance and posture reporting
- added private deployment starter scaffolding

### Prior-authorization interoperability workflows

- added PAS/CRD/DTR workflow journeys
- added bundle-level scenario review for synthetic prior-authorization exchanges
- added policy-to-standards crosswalk generation
- added richer payer/provider/shared implementation-track exports from approved Briefs

## Phase archive

For internal planning, the project has been delivered through phased milestones. Those details are intentionally kept out of the main README, but this file can continue to carry milestone and release detail as the product grows.

As of July 27, 2026:

- Phase 7 is complete
- the next major focus area is Phase 8

## Suggested future release-note structure

As HealthForge continues to grow, this file can be extended using a simple format such as:

- release name or date
- what shipped
- why it matters
- demo-worthy additions
- breaking or compatibility notes

That keeps the main README focused on product overview while preserving a clear historical record here.
