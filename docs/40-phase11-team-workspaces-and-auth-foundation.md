# Phase 11: team workspaces and auth foundation

Phase 11 makes HealthForge easier to use across real teams instead of only as a single-user showcase.

It adds an organization-scoped workspace layer that helps teams:

- group briefs under named projects and workspaces
- assign review work through visible queues and handoffs
- preserve repeated analysis through saved views
- inspect reusable prompt, retrieval, and workflow configurations
- explain the transition from local demo authentication to enterprise SSO

## What shipped

### Projects and workspaces

HealthForge now exposes a Team Workspace view in the web UI and a `/v1/workspace/overview` API.

That surface organizes work into:

- projects and workspaces
- linked briefs
- evidence collections
- reusable saved views

This gives demos and internal teams a cleaner way to say “these briefs belong to this initiative” instead of treating everything as one flat list.

### Reviewer queues and assignments

Phase 11 adds lightweight collaboration workflow support:

- reviewer, approver, changes-requested, and approved-artifacts queues
- explicit brief assignments
- visible handoff summaries
- org-scoped queue inspection

This is still intentionally bounded. It improves coordination, but it does not bypass the existing review and approval controls.

### Reusable workflow configurations

Operators can now inspect named workflow configurations for:

- prompt profile
- retrieval profile
- workflow profile
- config type and version

This gives HealthForge a clearer path toward governed tuning without forcing code edits for every operational change.

### Saved views and evidence workspaces

Teams can now preserve repeatable analysis paths through saved views and project-linked evidence collections.

Examples:

- “Needs reviewer action”
- “Approved and export-ready”
- “Public regulatory evidence”

This makes repeated demos and ongoing reviewer work much faster.

### Enterprise identity foundation

The local header demo path still exists and remains the default for sandbox use.

Phase 11 adds a real pluggable trusted-proxy authentication foundation so the codebase can support an enterprise SSO path later. The platform now models:

- active authentication mode
- supported authentication modes
- identity-provider profiles
- group-to-role mappings

Supported modes:

- `local_header` for demo and local development
- `trusted_proxy` for a future reverse-proxy or SSO-integrated enterprise deployment

The `trusted_proxy` path maps upstream identity groups into HealthForge roles such as reviewer, approver, auditor, and administrator.

## Why it matters

Before Phase 11, HealthForge had strong artifact-level review and governance, but less structure for how multiple people would organize and coordinate work.

Phase 11 closes that gap by introducing:

- team-oriented organization
- clearer handoff paths
- reusable operational configuration
- a more enterprise-ready identity story

## New API surface

- `GET /v1/workspace/overview`
- `POST /v1/workspace/projects`
- `POST /v1/workspace/projects/{projectId}/briefs`
- `POST /v1/workspace/assignments`
- `POST /v1/workspace/views`

## Demo path

To show this phase locally:

1. Open the Team Workspace tab in the web UI.
2. Create a new project or workspace.
3. Open a Brief in Brief Studio.
4. Link that Brief to the project.
5. Create an assignment for the next reviewer or approver.
6. Save a repeated query as a saved view.
7. Review the workflow configurations and auth foundation cards.

## Boundaries

Phase 11 is still a bounded local/private-demo foundation.

It does not yet provide:

- real external SSO login flows
- multi-tenant production hardening
- real-time collaboration or notifications from this workspace layer
- production-grade case management

Those are later-phase concerns. Phase 11 focuses on product structure and enterprise-readiness of the model.
