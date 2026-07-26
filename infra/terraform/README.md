# HealthForge private deployment starter

This directory is a starter Terraform scaffold for private deployment planning.

It is intentionally minimal and does not create production-ready infrastructure by itself. Its purpose is to:

- show the deployment shape expected by Phase 5;
- separate application, database, and secrets concerns;
- prepare the repository for self-hosted or private-cloud deployment work.

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

## Important notes

- do not commit real secrets
- do not treat this scaffold as a production security baseline
- use managed secrets, identity, backup, and network controls before real deployment
