# Deployment and observability foundation

This Phase 2 slice prepares HealthForge for non-production deployment outside a single workstation while preserving the MVP's no-PHI and least-privilege boundaries.

## Environment configuration

The local platform API reads deployment-sensitive configuration from environment variables rather than committed secrets:

- `HEALTHFORGE_DB_URL`
- `HEALTHFORGE_DB_USERNAME`
- `HEALTHFORGE_DB_PASSWORD`
- `HEALTHFORGE_AUTH_MODE`
- `HEALTHFORGE_DEFAULT_ORGANIZATION_ID`
- `HEALTHFORGE_ARTIFACT_DIRECTORY`
- `HEALTHFORGE_WORKSPACE_ROOT`
- `HEALTHFORGE_MODEL_ENABLED`
- `HEALTHFORGE_MODEL_PROVIDER`
- `HEALTHFORGE_DB_PORT`
- `HEALTHFORGE_API_PORT`

Default values remain local-development friendly, but shared or deployed environments must override credentials and storage paths through environment injection rather than source control.

## Secret-management boundary

- Do not commit credentials, tokens, or production connection strings.
- Use environment variables or deployment-secret stores only.
- Rotate any disclosed secret immediately and treat local defaults as development-only.
- Keep PHI, credentials, and model-provider secrets out of logs, audit events, and evaluation artifacts.

## Health and readiness

Operational endpoints are exposed through Spring Boot Actuator:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/info`
- `/actuator/metrics`

The Docker Compose deployment uses the readiness endpoint for API health checks and Postgres readiness checks for dependency startup ordering.

## Structured error behavior

The API now emits structured JSON error responses with:

- HTTP status
- short error category
- human-readable detail
- request path
- request ID
- validation error list where applicable

Every request receives an `X-Request-Id` header. Clients may provide one, or the service will generate one. This supports debugging without exposing stack traces or secrets in responses.

## Log redaction and retention guidance

- Logs should contain request IDs and operational events, not PHI or secrets.
- Default console logs are suitable for local and non-production use only.
- Retention should remain short in non-production unless audit or debugging needs require longer preservation.
- Audit records belong in structured application tables; diagnostic logs should not become the system of record.

## Reproducible non-production deployment path

The repository now provides a Docker Compose path under `infra/docker/docker-compose.yml` that starts:

- PostgreSQL
- the platform API container

This path is intended for local and non-production validation only. It is not a production hardening story, but it does provide a repeatable runtime with health checks and environment-driven configuration.

Use `infra/docker/.env.example` as the starting point for a non-committed local `.env` file so credentials and runtime overrides do not have to live in shell history or Compose YAML.

For the stronger operator baseline introduced in Phase 6, see [`docs/31-private-deployment-operator-guide.md`](./31-private-deployment-operator-guide.md).
