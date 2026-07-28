# Environment promotion and rollbacks

HealthForge now exposes a deployment-promotion guide API and aligns the private deployment story around three stages:

- demo
- staging
- private production

## Promotion principle

Promote the same application artifact forward and vary configuration by environment.

## Required controls

- keep integration targets and secrets outside source control
- enable collaboration, documentation, and webhook delivery per environment
- preserve rollback tags before enabling governed send or publish modes
- retain audit/export telemetry for post-change review

## Demo path

Use preview-friendly connectors and synthetic datasets.

## Staging path

Exercise governed send/publish behavior with operator review and rollback rehearsal.

## Private production path

Use customer- or enterprise-hosted infrastructure with explicit approval traceability, environment-scoped targets, and secret management.
