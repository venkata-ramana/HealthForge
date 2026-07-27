# HealthForge private deployment starter

This directory is a starter Terraform scaffold for private deployment planning.

It is intentionally minimal and does not create production-ready infrastructure by itself. Its purpose is to:

- show the deployment shape expected by Phase 5;
- separate application, database, and secrets concerns;
- prepare the repository for self-hosted or private-cloud deployment work.
- document safer operator defaults for private pilots before a full enterprise platform is introduced.

## Intended topology

- one private application host or container platform for `platform-api`
- one private PostgreSQL service
- one private artifact storage location
- security groups / firewall rules that expose only the application port to trusted networks

## Files

- `main.tf` defines the starter module shape and enterprise-oriented variables
- `variables.tf` describes the deployment inputs
- `outputs.tf` exposes the private app URL and deployment summary
- `terraform.tfvars.example` shows a local example configuration

## Operator expectations

- Keep raw secrets out of Terraform code, `.tfvars`, shell history, and pull requests.
- Pass only secret references or secret-manager paths through this scaffold.
- Treat `local_header` authentication as a private demo mode, not a shared-enterprise identity solution.
- Keep model features disabled unless a separate security, retention, and data-handling review has approved them.
- Restrict application ingress to trusted operator or pilot networks only.

## Suggested private-pilot baseline

Before using this scaffold for an enterprise pilot, define:

- a private hostname or internal load balancer for the API
- a managed PostgreSQL service or equivalent private database
- a secret manager entry for the database password
- artifact storage outside the container filesystem
- backup, retention, and rotation expectations for database and artifact storage
- an operator-owned runbook for credential rotation and incident response

## Important notes

- do not commit real secrets
- do not treat this scaffold as a production security baseline
- use managed secrets, identity, backup, and network controls before real deployment
