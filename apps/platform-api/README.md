# Local Platform API

This is the first executable HealthForge slice. It fetches only manifest-approved public PDFs, stores their immutable checksum-addressed artifact locally, extracts citeable page passages, and retrieves matching passages from PostgreSQL.

## Prerequisites

- Java 21 or newer
- Maven 3.9+
- Docker Desktop

## Run locally

From the repository root:

```bash
docker compose -f infra/docker/docker-compose.yml up -d
cd apps/platform-api
mvn spring-boot:run
```

The API starts on `http://localhost:8080`; health is available at `http://localhost:8080/actuator/health`.
The Docker database maps to host port `5433` by default to avoid colliding with a local PostgreSQL instance; set `HEALTHFORGE_DB_PORT` and `HEALTHFORGE_DB_URL` together if a different port is needed.

## Run as non-production containers

From the repository root:

```bash
docker compose -f infra/docker/docker-compose.yml up --build
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
- `HEALTHFORGE_ARTIFACT_DIRECTORY`
- `HEALTHFORGE_WORKSPACE_ROOT`
- `HEALTHFORGE_DB_PORT`
- `HEALTHFORGE_API_PORT`

Do not place real credentials or PHI in source control, Compose files, or logs.

## Current endpoints

- `POST /v1/ingestions` accepts only a configured public CMS source/version/content type, requires explicit allowed-use and terms-review metadata, captures an immutable artifact checksum, and creates page-level passages.
- `GET /v1/ingestions/{ingestionId}` returns the persisted job.
- `GET /v1/source-versions/{sourceVersionId}` returns persisted lifecycle and terms-review metadata for a source version.
- `POST /v1/source-versions/{sourceVersionId}/lifecycle` lets an administrator mark a source version active or withdrawn.
- `GET /v1/standards-artifacts` returns the curated standards artifact registry and supports lookup by canonical URL or artifact name.
- `POST /v1/retrieval/search` performs PostgreSQL full-text retrieval and returns citeable source/version/page metadata.
- `POST /v1/answers` builds a deterministic, cited evidence packet from retrieval results. It returns `insufficient_evidence` rather than an unsupported answer when no source passage matches.
- `POST /v1/briefs` persists a cited Brief draft, while `POST /v1/briefs/{briefId}/review-decisions` records append-only human review decisions.
- `GET /v1/briefs/{briefId}/work-item-export` exports approved Brief findings as non-sensitive JSON work items for external review without direct tracker writeback.
- `POST /v1/corpus-snapshots` pins an immutable set of current-eligible source versions by default and supports an explicit historical-reconstruction override for withdrawn or superseded versions.
- `GET /v1/fhir-validation/catalog` returns the pinned package/profile catalog, package metadata, and support status for deterministic validation.
- `POST /v1/fhir-validation/validate` validates a synthetic or explicitly non-sensitive FHIR example against a pinned R4 profile selection and returns structured findings plus evidence links.

Synthetic prior-authorization validation fixtures live under `knowledge/fixtures/fhir-validation/`, with scenario metadata under `evals/datasets/fhir-validation/`. They are useful for repeatable non-sensitive evaluation, but they do not prove payer-specific interoperability or production-ready conformance.

The answer endpoint does not call an external model or persist question/context input. Its findings reproduce retrieved source excerpts with source/version/page citations, and always require human review before a regulatory, clinical, or implementation conclusion.

Artifacts are stored outside the repository at `~/.healthforge/artifacts` by default. Set `HEALTHFORGE_ARTIFACT_DIRECTORY` to use a different local path.

## Structured errors and request IDs

The API returns structured JSON error responses and includes an `X-Request-Id` header on every response. You may provide an `X-Request-Id` header in requests to help correlate client-side and server-side debugging.
