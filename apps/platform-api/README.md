# Local Platform API

This is the first executable HealthForge slice. It does not fetch or parse source documents yet. It accepts a request only when it exactly matches a curated source policy, writes the provenance request to PostgreSQL, and provides the retrieval-response shape defined by the project contract.

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

## Current endpoints

- `POST /v1/ingestions` accepts only a configured public CMS source/version/content type and records a `requested` ingestion job.
- `GET /v1/ingestions/{ingestionId}` returns the persisted job.
- `POST /v1/retrieval/search` validates a non-sensitive retrieval request and returns an empty, contract-shaped result while source parsing and indexing are not yet implemented.

The next implementation increment will fetch a manifest-approved artifact, capture a checksum, create immutable source-version/passage records, and replace the placeholder retrieval response with cited passages.
