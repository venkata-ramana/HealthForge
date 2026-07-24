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

## Current endpoints

- `POST /v1/ingestions` accepts only a configured public CMS source/version/content type, captures an immutable artifact checksum, and creates page-level passages.
- `GET /v1/ingestions/{ingestionId}` returns the persisted job.
- `POST /v1/retrieval/search` performs PostgreSQL full-text retrieval and returns citeable source/version/page metadata.

Artifacts are stored outside the repository at `~/.healthforge/artifacts` by default. Set `HEALTHFORGE_ARTIFACT_DIRECTORY` to use a different local path.
