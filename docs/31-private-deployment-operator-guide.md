# Private deployment operator guide

This guide explains how to run HealthForge in a private, non-production or pilot-style environment with clearer boundaries around secrets, configuration, and runtime expectations.

It does **not** claim production hardening for PHI, customer traffic, or regulated operational use. It is the operator bridge between the local prototype and a safer enterprise pilot.

## What this guide covers

- which configuration belongs in environment variables
- how to treat secrets and secret references
- how Docker Compose and Terraform scaffolding should be used
- what defaults are acceptable only for local/private demos
- what to tighten before any shared pilot

## Configuration boundary

HealthForge separates operator-managed runtime configuration from committed application code.

The main operator-controlled settings are:

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

These values should be injected by the runtime environment, container orchestrator, or secret-management layer rather than committed into the repository.

## Secret-handling expectations

Minimum operator rules:

- never commit real passwords, tokens, connection strings, or customer identifiers
- never store raw secrets in Terraform source or example variable files
- use a secret manager, deployment platform secret store, or local non-committed env file
- rotate any secret that is accidentally disclosed in logs, screenshots, shell history, or Git history
- keep PHI, credentials, and model-provider secrets out of app logs and audit events

HealthForge currently uses local header identity for demo and private review flows. That means secret hardening is still necessary even before full SSO arrives, because database credentials, storage paths, and runtime flags remain operator responsibilities.

## Docker Compose guidance

The Compose stack under `infra/docker/docker-compose.yml` is intended for local validation and private demos.

Use it like this:

1. Copy `infra/docker/.env.example` to `infra/docker/.env`
2. Replace the local placeholder password values
3. Keep the `.env` file out of version control
4. Start the stack with:

```bash
docker compose --env-file infra/docker/.env -f infra/docker/docker-compose.yml up --build -d
```

Recommended private-pilot adjustments:

- replace `change-me-local-only` values before any shared environment
- keep API access limited to trusted users or trusted networks
- use host or platform storage for artifacts and backups
- keep `HEALTHFORGE_MODEL_ENABLED=false` unless separately approved
- keep `HEALTHFORGE_AUTH_MODE=local_header` only for private demo flows until a stronger identity provider is implemented

## Terraform scaffold guidance

The Terraform starter under `infra/terraform/` is intentionally lightweight. It is best used as a planning and interface contract rather than a complete enterprise deployment module.

Use the scaffold to define:

- environment name
- organization scope
- private ports and storage paths
- database username
- secret-manager reference for the database password
- auth mode and default organization behavior
- whether external model features stay disabled

The important point is that Terraform should carry **references** to secrets, not the secrets themselves.

## Safer defaults for private pilots

For a private pilot, the safest current baseline is:

- non-sensitive public source material only
- synthetic or non-sensitive FHIR validation only
- local header auth only for bounded internal demos
- external model features disabled
- database credentials injected at runtime
- artifact storage mounted outside the app image
- network exposure limited to trusted users
- short log retention and no request-body logging

## Before any shared enterprise pilot

Before moving beyond a bounded private environment, an operator should be able to answer:

- where are secrets stored and rotated?
- which networks can reach the API?
- where are artifacts stored and backed up?
- how are audit exports retained and who can access them?
- what identity mode is in use?
- are model features disabled or explicitly approved?
- what incident process applies if credentials or sensitive internal context are exposed?

## Related documents

- [`docs/19-deployment-and-observability.md`](./19-deployment-and-observability.md)
- [`docs/13-security-data-boundaries-and-threat-model.md`](./13-security-data-boundaries-and-threat-model.md)
- [`infra/terraform/README.md`](../infra/terraform/README.md)
- [`apps/platform-api/README.md`](../apps/platform-api/README.md)
