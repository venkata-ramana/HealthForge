# End-to-end demo and contributor onboarding

This guide is the fastest safe path for someone new to HealthForge to understand the product, run it locally, and demonstrate the current value without stepping outside the platform boundary.

## Who this is for

- prospective users evaluating the product
- teammates preparing a repeatable demo
- contributors who want a reliable local setup
- internal champions who need a clean story for architecture or product conversations

## Stay inside the boundary

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

## Recommended demo paths

### 5-minute walkthrough

Best for a quick product introduction.

1. Open the web workspace.
2. Use the sandbox workspace with role `reviewer`.
3. Ask:
   - question: `What changes do we need for CMS prior authorization workflows?`
   - context: `Synthetic provider EHR planning scenario for prior authorization APIs.`
4. Show the cited findings and create the Brief.
5. Open one finding, one citation, and one review action.

Expected outcome:

- grounded evidence preview
- a reviewable Brief
- visible traceability between question, evidence, and findings

### 15-minute stakeholder walkthrough

Best for product, architecture, or interoperability discussions.

1. Start with the reviewer flow above.
2. Switch to `approver`.
3. Show approval recording and export readiness.
4. Switch to `auditor`.
5. Open the evaluation dashboard and policy/safety report.
6. Close with the architecture diagram and product boundary.

Expected outcome:

- reviewers see how research becomes a governed artifact
- approvers see that approval is explicit and inspectable
- auditors see trust signals rather than hidden model behavior

### 20-minute operator / enterprise walkthrough

Best for platform, operations, or governance conversations.

1. Run the reviewer and approver path.
2. Switch to `administrator`.
3. Open:
   - access review
   - identity directory
   - deployment promotion guide
   - regulated readiness
4. Explain the difference between current bounded posture and future production ambitions.

Expected outcome:

- stakeholders see that HealthForge separates demo-safe value from enterprise claims
- the product feels more governable than a generic chat interface

## Role-based walkthrough

### Reviewer

Use role `reviewer`.

Show:

- evidence retrieval
- finding inspection
- Brief creation
- review decisions

### Approver

Use role `approver`.

Show:

- approval workflow
- approval rationale
- export and implementation handoff posture

### Auditor

Use role `auditor`.

Show:

- evaluation dashboard
- compliance dashboard
- policy and safety reporting

### Administrator

Use role `administrator`.

Show:

- identity and access review
- deployment guidance
- regulated-readiness story
- governed operations posture

## Contributor onboarding path

Read these in order:

1. [`README.md`](../README.md)
2. [`docs/02-target-architecture.md`](./02-target-architecture.md)
3. [`docs/23-client-api-surface.md`](./23-client-api-surface.md)
4. [`docs/38-showcase-architecture-and-solution-narratives.md`](./38-showcase-architecture-and-solution-narratives.md)
5. [`docs/31-private-deployment-operator-guide.md`](./31-private-deployment-operator-guide.md)

## Safe first contribution ideas

- improve demo wording or stakeholder talk tracks
- add bounded API test coverage
- tighten docs around readiness and product boundaries
- improve UI consistency without widening product claims

## What the demo does not prove

This demo does not prove:

- payer-specific interoperability
- legal or compliance correctness
- PHI readiness
- production-hardening claims
- unattended approval or autonomous workflow execution
