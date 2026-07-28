# Local Platform API

This is the first executable HealthForge slice. It fetches only manifest-approved public PDFs, stores their immutable checksum-addressed artifact locally, extracts citeable page passages, and retrieves matching passages from PostgreSQL.

The Phase 10 workspace also presents these APIs through a more polished showcase UI for reviewers, operators, and enterprise evaluators.

## Prerequisites

- Java 21 or newer
- Maven 3.9+
- Docker Desktop

## Run locally

From the repository root:

```bash
cp infra/docker/.env.example infra/docker/.env
docker compose --env-file infra/docker/.env -f infra/docker/docker-compose.yml up -d
cd apps/platform-api
mvn spring-boot:run
```

The API starts on `http://localhost:8080`; health is available at `http://localhost:8080/actuator/health`.
The Docker database maps to host port `5433` by default to avoid colliding with a local PostgreSQL instance; set `HEALTHFORGE_DB_PORT` and `HEALTHFORGE_DB_URL` together if a different port is needed.

## Run as non-production containers

From the repository root:

```bash
cp infra/docker/.env.example infra/docker/.env
docker compose --env-file infra/docker/.env -f infra/docker/docker-compose.yml up --build
```

This starts PostgreSQL plus the platform API container. The API health endpoints are available at:

- `http://localhost:8080/actuator/health`
- `http://localhost:8080/actuator/health/readiness`
- `http://localhost:8080/actuator/health/liveness`
- `http://localhost:8080/actuator/metrics`

## Environment configuration

Prefer environment variables over committed secrets:

- `HEALTHFORGE_DB_URL`
- `HEALTHFORGE_DB_USERNAME`
- `HEALTHFORGE_DB_PASSWORD`
- `HEALTHFORGE_AUTH_MODE`
- `HEALTHFORGE_DEFAULT_ORGANIZATION_ID`
- `HEALTHFORGE_ARTIFACT_DIRECTORY`
- `HEALTHFORGE_WORKSPACE_ROOT`
- `HEALTHFORGE_DB_PORT`
- `HEALTHFORGE_API_PORT`

Do not place real credentials or PHI in source control, Compose files, or logs.

For private deployment and operator guidance beyond local development, see [`docs/31-private-deployment-operator-guide.md`](../docs/31-private-deployment-operator-guide.md).

The current supported authentication mode is `local_header`, which keeps the local header workflow active for demos and development while routing request identity through a pluggable provider boundary in the application.

## Current endpoints

- `POST /v1/ingestions` accepts only a configured public CMS source/version/content type, requires explicit allowed-use and terms-review metadata, captures an immutable artifact checksum, and creates page-level passages.
- `GET /v1/ingestions/{ingestionId}` returns the persisted job.
- `GET /v1/source-versions/{sourceVersionId}` returns persisted lifecycle and terms-review metadata for a source version.
- `POST /v1/source-versions/{sourceVersionId}/lifecycle` lets an administrator mark a source version active or withdrawn.
- `GET /v1/standards-artifacts` returns the curated standards artifact registry and supports lookup by canonical URL or artifact name.
- `POST /v1/fhir-assistant/query` provides a bounded standards-native lookup workflow over curated artifacts and the pinned validation catalog.
- `POST /v1/regulation-explainers` produces a source-bounded regulation explainer with citations, implications, and caveats.
- `POST /v1/prior-auth/copilot` analyzes PAS/CRD/DTR-oriented workflow questions with evidence and standards touchpoints.
- `POST /v1/prior-auth/journeys` renders an explicit PAS, CRD, or DTR workflow journey with stages, transitions, responsibilities, and candidate standards touchpoints for planning and demo use.
- `POST /v1/prior-auth/bundle-reviews` reviews a synthetic multi-resource prior-authorization bundle, combines bundle-level validation with workflow context, and returns scenario findings for reviewer inspection.
- `POST /v1/prior-auth/standards-crosswalks` turns cited policy findings into an inspectable crosswalk across workflow stages, FHIR resources, operations, and candidate implementation guides.
- `POST /v1/tracker-exports/preview` generates preview-only GitHub- or Jira-ready payloads from approved work-item exports and records an audit event.
- `POST /v1/collaboration/notifications` packages review-ready, approval-needed, or workflow-handoff notifications for governed collaboration delivery.
- `POST /v1/documentation-exports` packages approved artifacts for documentation-system delivery paths such as Notion-, SharePoint-, or Confluence-style targets.
- `POST /v1/automation/webhook-subscriptions`, `POST /v1/automation/events`, and `GET /v1/automation/status` expose the Phase 8 workflow-event and governed webhook framework.
- `GET /v1/compliance/dashboard` summarizes org-scoped Brief, validation, and export telemetry for auditors and administrators.
- `GET /v1/evaluation/dashboard` returns regression, evidence, review-quality, and workflow-quality signals for auditors and administrators.
- `GET /v1/evaluation/policy-safety-report` returns a clearer policy and safety summary around unsupported outputs, approval policy, and governed integrations.
- `GET /v1/enterprise/posture` describes the current enterprise control posture for the active organization.
- `GET /v1/enterprise/deployment-promotion-guide` returns environment promotion and rollback guidance for administrators.
- `GET /v1/admin/identity-directory` returns the current durable organization, user, membership, and role-assignment directory model for administrators.
- `GET /v1/admin/access-review` returns an organization-scoped access-review report with role assignments, audit-policy metadata, and oversight-safe access rationale for administrators.
- `POST /v1/architecture-reviews` returns a bounded architecture-review artifact for a non-sensitive scenario using grounded evidence and curated standards touchpoints.
- `POST /v1/codegen/starter-artifacts` generates example-only starter code from an approved work-item export and preserves traceability back to the reviewed artifact.
- `POST /v1/retrieval/search` performs PostgreSQL full-text retrieval and returns citeable source/version/page metadata.
- `POST /v1/answers` builds a deterministic, cited evidence packet from retrieval results. It returns `insufficient_evidence` rather than an unsupported answer when no source passage matches.
- `POST /v1/briefs` persists a cited Brief draft, while `POST /v1/briefs/{briefId}/review-decisions` records append-only human review decisions.
- `GET /v1/briefs/{briefId}/work-item-export` exports approved Brief findings as non-sensitive JSON work items for external review without direct tracker writeback.
- `POST /v1/corpus-snapshots` pins an immutable set of current-eligible source versions by default and supports an explicit historical-reconstruction override for withdrawn or superseded versions.
- `GET /v1/fhir-validation/catalog` returns the pinned package/profile catalog, package metadata, and support status for deterministic validation.
- `POST /v1/fhir-validation/validate` validates a synthetic or explicitly non-sensitive FHIR example against a pinned R4 profile selection and returns structured findings plus evidence links.
- `GET /v1/fhir-synthetic/catalog` and `POST /v1/fhir-synthetic/generate` expose reusable synthetic FHIR fixtures for demos and validation walkthroughs.

Synthetic prior-authorization validation fixtures live under `knowledge/fixtures/fhir-validation/`, with scenario metadata under `evals/datasets/fhir-validation/`. They are useful for repeatable non-sensitive evaluation, but they do not prove payer-specific interoperability or production-ready conformance.

See [`docs/23-client-api-surface.md`](../docs/23-client-api-surface.md) for the supported local client workflows, auth headers, structured error format, and example calls.

For a repeatable evaluator/contributor walkthrough, see [`docs/36-end-to-end-demo-and-contributor-onboarding.md`](../docs/36-end-to-end-demo-and-contributor-onboarding.md).

The local VS Code extension prototype lives under [`apps/vscode-extension`](../apps/vscode-extension/) and is intended for local developer use only.

The answer endpoint does not call an external model or persist question/context input. Its findings reproduce retrieved source excerpts with source/version/page citations, and always require human review before a regulatory, clinical, or implementation conclusion.

Artifacts are stored outside the repository at `~/.healthforge/artifacts` by default. Set `HEALTHFORGE_ARTIFACT_DIRECTORY` to use a different local path.

## Structured errors and request IDs

The API returns structured JSON error responses and includes an `X-Request-Id` header on every response. You may provide an `X-Request-Id` header in requests to help correlate client-side and server-side debugging.
