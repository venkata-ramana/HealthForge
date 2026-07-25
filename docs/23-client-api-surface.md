# Client-facing API surface

This document defines the current **supported local client API boundary** for HealthForge as of Phase 4.

## Scope

The supported client-facing workflows are:

- Brief creation, review, approval, and export
- Architecture review
- FHIR validation catalog and validation execution
- Standards artifact lookup
- FHIR knowledge assistant
- Regulation explainer
- Prior-authorization copilot
- Tracked GitHub/Jira-ready export previews

The current API is intended for:

- local development;
- internal demos; and
- bounded non-sensitive client integrations.

It is **not** yet intended for:

- public internet exposure;
- production identity hardening;
- PHI-bearing workloads; or
- direct third-party tracker writeback.

## Local authentication assumptions

Write operations currently use local actor headers:

- `X-HealthForge-Actor`
- `X-HealthForge-Role`

Supported local roles are:

- `reviewer`
- `administrator`

This is a temporary local-only identity model that preserves audit traceability until stronger authentication and RBAC arrive in later phases.

## Request correlation and errors

All responses include an `X-Request-Id` header. Clients may also send `X-Request-Id` on requests to simplify local debugging and audit correlation.

Structured errors follow the shared JSON shape:

```json
{
  "timestamp": "2026-07-25T17:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "detail": "Request validation failed.",
  "path": "/v1/briefs",
  "request_id": "req_local_example",
  "errors": [
    "question: must not be blank"
  ]
}
```

## Supported workflow examples

Create a reviewable Brief:

```bash
curl -X POST http://localhost:8080/v1/briefs \
  -H 'Content-Type: application/json' \
  -H 'X-HealthForge-Actor: local.reviewer' \
  -H 'X-HealthForge-Role: reviewer' \
  -d '{
    "corpus_id": "mvp-regulatory-corpus",
    "corpus_version": "2026-07-24-expanded-web-core-v4",
    "question": "What does the rule change for prior authorization workflows?",
    "project_context": "Synthetic provider EHR planning scenario."
  }'
```

Record a review decision:

```bash
curl -X POST http://localhost:8080/v1/briefs/brief_example/review-decisions \
  -H 'Content-Type: application/json' \
  -H 'X-HealthForge-Actor: local.reviewer' \
  -H 'X-HealthForge-Role: reviewer' \
  -d '{
    "finding_id": "find_example",
    "decision": "accept",
    "reviewer": "local.reviewer",
    "rationale": "Grounded and ready for review."
  }'
```

Approve a Brief for export:

```bash
curl -X POST http://localhost:8080/v1/briefs/brief_example/approvals \
  -H 'Content-Type: application/json' \
  -H 'X-HealthForge-Actor: local.admin' \
  -H 'X-HealthForge-Role: administrator' \
  -d '{
    "rationale": "Reviewed for local planning export."
  }'
```

Open the approved work-item export:

```bash
curl http://localhost:8080/v1/briefs/brief_example/work-item-export
```

Create an architecture review:

```bash
curl -X POST http://localhost:8080/v1/architecture-reviews \
  -H 'Content-Type: application/json' \
  -d '{
    "corpus_id": "mvp-regulatory-corpus",
    "corpus_version": "2026-07-24-expanded-web-core-v4",
    "question": "How should we separate PAS submission from documentation workflow?",
    "project_context": "Synthetic provider EHR architecture design scenario."
  }'
```

Inspect the pinned validation catalog:

```bash
curl http://localhost:8080/v1/fhir-validation/catalog
```

Validate a synthetic FHIR example:

```bash
curl -X POST http://localhost:8080/v1/fhir-validation/validate \
  -H 'Content-Type: application/json' \
  -d '{
    "package_id": "hl7.fhir.r4.core",
    "package_version": "4.0.1",
    "profile_url": "http://hl7.org/fhir/StructureDefinition/Claim",
    "data_classification": "synthetic",
    "resource": {
      "resourceType": "Claim",
      "status": "active",
      "use": "preauthorization"
    }
  }'
```

Run the FHIR knowledge assistant:

```bash
curl -X POST http://localhost:8080/v1/fhir-assistant/query \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "PAS claim profile"
  }'
```

Generate a regulation explainer from a selected source:

```bash
curl -X POST http://localhost:8080/v1/regulation-explainers \
  -H 'Content-Type: application/json' \
  -d '{
    "corpus_id": "mvp-regulatory-corpus",
    "corpus_version": "2026-07-24-expanded-web-core-v4",
    "source_id": "cms-0057-f-final-rule",
    "question": "What does this source imply for prior authorization APIs?",
    "project_context": "Synthetic provider planning scenario."
  }'
```

Analyze a PAS/CRD/DTR scenario through the copilot:

```bash
curl -X POST http://localhost:8080/v1/prior-auth/copilot \
  -H 'Content-Type: application/json' \
  -d '{
    "corpus_id": "mvp-regulatory-corpus",
    "corpus_version": "2026-07-24-expanded-web-core-v4",
    "question": "How should PAS claim submission work in a provider EHR workflow?",
    "project_context": "Synthetic prior authorization workflow analysis."
  }'
```

Generate a tracked export preview from an approved Brief:

```bash
curl -X POST http://localhost:8080/v1/tracker-exports/preview \
  -H 'Content-Type: application/json' \
  -H 'X-HealthForge-Actor: local.admin' \
  -H 'X-HealthForge-Role: administrator' \
  -d '{
    "brief_id": "brief_example",
    "target_system": "github",
    "approval_acknowledgement": true,
    "writeback_requested": false,
    "export_reason": "Prepare a local engineering backlog preview."
  }'
```

## Boundary statement

This Phase 4 API surface is the supported product boundary for local clients. It is explicit about what remains local/demo only:

- header-based local identity;
- non-sensitive and synthetic-only examples;
- no direct external writeback;
- no production auth hardening; and
- no PHI or clinical decision support use.
