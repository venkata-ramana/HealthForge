# Shared review identity and audit

For the local Phase 2 shared-review preparation slice, HealthForge now distinguishes two write-capable roles:

- `reviewer`: may create Brief drafts from grounded evidence and record review decisions.
- `administrator`: retains reviewer capabilities and is reserved for future administrative actions such as controlled ingestion and deployment configuration.

This is intentionally a local, header-based authentication boundary rather than a production identity system. Until shared deployment work is complete, write actions must include:

- `X-HealthForge-Actor`: stable local actor identifier
- `X-HealthForge-Role`: `reviewer` or `administrator`

Current authenticated write actions:

- `POST /v1/briefs`
- `POST /v1/briefs/{briefId}/review-decisions`

Every authenticated write action appends immutable audit events to the Brief record:

- `brief_created`
- `evidence_selected`
- `review_decision_recorded`

The local UI surfaces the actor identity and role for write actions and renders the audit trail in the Brief detail view. This preserves no-PHI boundaries while preparing the workflow for future SSO/RBAC replacement in shared deployment.
