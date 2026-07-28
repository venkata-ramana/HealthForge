# End-to-end demo and contributor onboarding

This guide is the fastest path for a new evaluator or contributor to understand what HealthForge does, run it locally, and stay inside the non-sensitive boundaries of the current product.

## Who this is for

- prospective users who want to understand the product quickly
- contributors who want a safe first task and a reliable local run path
- internal champions who need a repeatable demo without author-only tribal knowledge

## Non-sensitive boundary

Use only:

- public regulations and implementation guides
- synthetic or non-sensitive project context
- synthetic FHIR examples

Do not use:

- PHI
- production credentials
- customer data
- external system tokens

## Local setup

From the repository root:

```bash
cp infra/docker/.env.example infra/docker/.env
docker compose --env-file infra/docker/.env -f infra/docker/docker-compose.yml up --build -d
```

Open:

- UI: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`

## Recommended demo flow

### 1. Reviewer path

Use the sandbox workspace in the web UI with:

- role: `reviewer`
- question: `What changes do we need for CMS prior authorization workflows?`
- context: `Synthetic provider EHR planning scenario for prior authorization APIs.`

Expected outcome:

- grounded evidence preview
- a reviewable Brief
- one or more recorded review decisions

### 2. Approver path

Switch the same session to:

- role: `approver`

Expected outcome:

- approval recording becomes available
- work-item export becomes available
- tracker-ready and governed delivery flows remain explicit and review-first

### 3. Auditor path

Switch to:

- role: `auditor`

Expected outcome:

- compliance dashboard access
- evaluation dashboard access
- policy and safety report access

### 4. Administrator path

Switch to:

- role: `administrator`

Expected outcome:

- access review
- identity directory
- deployment promotion guide
- full operator walkthrough

## Contributor onboarding path

Read these in order:

1. [`README.md`](../README.md)
2. [`docs/02-target-architecture.md`](./02-target-architecture.md)
3. [`docs/23-client-api-surface.md`](./23-client-api-surface.md)
4. [`docs/31-private-deployment-operator-guide.md`](./31-private-deployment-operator-guide.md)
5. [`docs/34-phase-9-evaluation-and-trust.md`](./34-phase-9-evaluation-and-trust.md)

## Safe first contribution ideas

- improve demo wording or operator explanations
- add test coverage for bounded APIs
- tighten docs around current capability boundaries
- improve UI consistency without expanding product claims

## What the demo does not prove

This demo does not prove:

- payer-specific interoperability
- legal or compliance correctness
- production hardening
- PHI readiness
- unattended automation approval
