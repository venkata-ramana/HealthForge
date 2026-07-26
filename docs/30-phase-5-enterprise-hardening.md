# Phase 5 enterprise hardening

Phase 5 turns the local review-and-validation platform into a stronger private-deployment foundation.

## What was added

- organization-aware persistence for Briefs, approvals, review decisions, audit events, validation telemetry, and tracker export telemetry
- expanded role groundwork:
  - reviewer
  - approver
  - auditor
  - administrator
- org-scoped Brief reads in the UI and API
- compliance dashboard API for audit and oversight scenarios
- enterprise posture API for private deployment and control inspection
- persisted validation telemetry for synthetic/non-sensitive FHIR validation activity
- persisted tracked export telemetry with explicit retention-until metadata
- synthetic FHIR data generator APIs backed by repository fixtures
- Terraform starter scaffolding for private/self-hosted deployment planning

## New API surfaces

- `GET /v1/compliance/dashboard`
- `GET /v1/enterprise/posture`
- `GET /v1/fhir-synthetic/catalog`
- `POST /v1/fhir-synthetic/generate`

Brief-related endpoints are now organization scoped and require actor headers for reads as well as writes:

- `X-HealthForge-Actor`
- `X-HealthForge-Role`
- `X-HealthForge-Organization`

## What this phase does not claim

- production PHI handling
- SSO or SCIM integration
- automated retention deletion jobs
- direct GitHub/Jira writeback
- multi-model policy routing

This phase is best understood as enterprise groundwork: the system now has clearer org boundaries, stronger role semantics, private deployment scaffolding, and demo-safe telemetry surfaces for audits and oversight.
