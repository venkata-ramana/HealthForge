# Phase 14: private deployment and enterprise operations

Phase 14 hardens the operator story around running HealthForge in private environments.

It does not change the product boundary into PHI-ready infrastructure. Instead, it makes private deployment operations easier to inspect, explain, rehearse, and govern.

## What shipped

- configuration and secret-boundary policy surface in the admin console
- observability and retention operations view for auditors and administrators
- backup, restore, migration, and recovery guidance with validation checks
- tenant-aware usage summaries, soft quotas, and cost-control proxy signals
- operator attestation and sign-off history for important environment and policy changes

## Why it matters

Before this phase, HealthForge already had posture, compliance, access review, and deployment guidance.

Phase 14 connects those pieces into a stronger private-operations model:

- operators can explain which settings are safe to inspect
- secret references stay visible without exposing raw values
- continuity and migration steps become easier to rehearse
- enterprise evaluators get a clearer operating narrative
- sign-off history complements the Brief audit trail with operational governance

## New admin-console surfaces

### Configuration and secret policy

Shows:

- environment policy by deployment stage
- configuration classification
- secret reference tracking
- operator-safe runtime assumptions

Use it to explain:

- which settings are environment-bound
- which settings are governance-critical
- how secret-backed connectors stay outside repo history and product output

### Observability and retention operations

Shows:

- recent workflow activity
- blocked-delivery and invalid-validation pressure
- retention posture
- operator runbook entry points

Use it to explain:

- how an operator would notice delivery or workflow problems
- how retention and incident response fit together
- why HealthForge is more than a static demo surface

### Continuity and migration

Shows:

- continuity inventory
- backup guidance
- restore checks
- migration validation checks
- recovery rehearsal guidance

Use it to explain:

- what should move together during backup or restore
- what to verify after restore or promotion
- how to rehearse rollback without overstating production automation

### Usage and quota signals

Shows:

- organization-scoped usage counts
- advisory soft quotas
- cost-related proxy signals

Use it to explain:

- where private deployment demand is growing
- how future enterprise packaging can become tenant-aware
- how operators can talk about scale without promising billing or hard enforcement

### Attestations and sign-off history

Shows:

- expected governance acknowledgments
- recorded operator sign-offs
- recent operational change history

Use it to explain:

- how important changes are acknowledged
- how governance can be inspected later
- how operational history complements review and approval history

## Demo path

1. Switch to `administrator`
2. Open `Config policy`
3. Open `Continuity`
4. Open `Attestations` and record one sign-off
5. Switch to `auditor`
6. Open `Observability`
7. Open `Usage`

That gives a compact walkthrough of the new Phase 14 operations story.

## Boundaries

Phase 14 still keeps HealthForge inside its current product boundary:

- public, non-sensitive sources only
- synthetic or non-sensitive workflow and FHIR examples
- human review required
- no PHI handling
- no production-readiness claim

What changed is not the honesty of the boundary, but how clearly private deployment operations can now be described within it.
