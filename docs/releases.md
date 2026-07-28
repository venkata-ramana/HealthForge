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
- governed delivery scaffolding for tracker, collaboration, documentation, and webhook workflows
- trust and evaluation reporting for operators and enterprise evaluators
- a Phase 10 showcase workspace with guided demo paths and a clearer admin console
- a Phase 11 team workspace with projects, queues, assignments, saved views, reusable configs, and enterprise auth foundation
- a Phase 12 governed integration layer with connector receipts, inbound intake, recovery tooling, and orchestration templates

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

### Governed delivery and trust layer

- added governed tracker writeback with approval gates and visible retry/blocked states
- added collaboration notification, documentation export, and webhook automation scaffolding
- added evaluation dashboard and policy/safety reporting
- added runtime telemetry for insufficient-evidence and unsupported-output patterns

### Showcase and productization

- redesigned the web workspace into a clearer showcase experience
- added a guided sandbox/demo path and stronger admin/operator walkthrough surface
- clarified deployable editions and capability boundaries
- added stronger onboarding, testing paths, and presentation-ready architecture narratives
- added content and community pipeline templates tied to shipped milestones

### Team collaboration and enterprise identity foundation

- added org-scoped projects and workspaces above individual briefs
- added reviewer queues, explicit brief assignments, and handoff summaries
- added saved views and evidence collections for repeated analysis
- added reusable workflow configuration visibility for prompts, retrieval, and workflow profiles
- added a trusted-proxy authentication foundation and group-to-role mapping model alongside the local demo path

### Governed integrations and orchestration

- added a governed connector layer for tracker and documentation delivery systems
- added connector health summaries, recent receipts, retry queue visibility, and recovery actions
- added inbound case intake with optional case-to-Brief orchestration
- added reusable orchestration templates for common interoperability programs
- clarified simulated vs live-capable connector behavior through environment-managed settings

## Phase archive

For internal planning, the project has been delivered through phased milestones. Those details are intentionally kept out of the main README, but this file can continue to carry milestone and release detail as the product grows.

As of Tuesday, July 28, 2026:

- Phase 12 is complete
- the next major planning step is to execute Phases 13 through 15

## Suggested future release-note structure

As HealthForge continues to grow, this file can be extended using a simple format such as:

- release name or date
- what shipped
- why it matters
- demo-worthy additions
- breaking or compatibility notes

That keeps the main README focused on product overview while preserving a clear historical record here.
