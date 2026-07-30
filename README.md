# HealthForge

HealthForge is an open-source AI engineering platform for healthcare interoperability.

It turns public regulations, implementation guides, and standards references into grounded, reviewable engineering outputs that teams can inspect, challenge, approve, and carry into downstream planning.

## Why it matters

Healthcare interoperability work usually breaks down between four steps:

- finding the right evidence
- interpreting it consistently
- getting human review and approval
- turning approved thinking into implementation-ready artifacts

HealthForge is built to close that gap without pretending an LLM should become the system of record.

## What you can do with it today

| Workflow | What HealthForge does |
| --- | --- |
| Grounded evidence search | Retrieves cited evidence from approved public-source snapshots |
| Brief creation | Turns evidence into a structured, reviewable Brief with explicit limits |
| Review and approval | Captures reviewer decisions, approvals, and audit history |
| FHIR and prior-auth planning | Supports synthetic standards lookup, validation, journey mapping, and prior-authorization analysis |
| Team workspace | Organizes work into projects, queues, assignments, saved views, and evidence collections |
| Trust and governance | Exposes evaluation, safety, compliance, and operator-readiness views |
| Implementation handoff | Prepares work-item exports, architecture guidance, solution packs, and starter artifacts |

## Product surfaces

| Surface | What it is for |
| --- | --- |
| Web workspace | Reviewer, approver, auditor, administrator, and demo workflows |
| Platform API | Local and bounded client-facing workflow APIs |
| VS Code prototype | Builder-side workflow exploration and implementation guidance |
| CLI | Scriptable local workflows for demos and engineering tasks |
| JavaScript SDK | Simple builder-facing integrations and examples |
| Documentation set | Demo, architecture, release-story, and operator walkthroughs |

## How the system works

```mermaid
flowchart LR
    A["Approved public sources"] --> B["Ingestion and provenance"]
    B --> C["Snapshots and citeable passages"]
    C --> D["Retrieval and grounded answers"]
    D --> E["Brief review, approvals, and audit"]
    E --> F["Projects, queues, and evidence workspaces"]
    F --> G["Implementation exports, integrations, and delivery artifacts"]
    F --> H["Evaluation, safety, compliance, and operator reporting"]
```

## What makes HealthForge different

- Evidence stays visible instead of disappearing behind a generated answer.
- Human review is a first-class workflow, not an afterthought.
- Outputs are bounded for synthetic and non-sensitive interoperability work.
- The platform separates research, approval, and delivery so governance stays inspectable.

## What to demo in 10 minutes

1. Ask a prior-authorization or standards-planning question.
2. Show the cited findings and create a Brief.
3. Switch roles to review, approve, and inspect audit history.
4. Close with workspace, trust, and governed delivery surfaces.

Recommended prompt:

- Question: `What changes do we need for CMS prior authorization workflows?`
- Context: `Synthetic provider EHR planning scenario for prior authorization APIs.`

Additional prompts:

- Question: `How should a provider workflow handle documentation and status exchange for prior authorization?`
  Context: `Synthetic architecture review for a provider-facing utilization management workflow.`

- Question: `What evidence-quality and approval signals should an enterprise evaluator inspect?`
  Context: `Synthetic enterprise evaluation walkthrough using the HealthForge trust layer.`

## Who it is for

- interoperability product and platform teams
- healthcare architects and implementation leads
- reviewers, approvers, and auditors who need traceability
- internal champions who need a safe, credible demo
- builders who want a local-first interoperability workflow sandbox

## Capability boundaries

HealthForge is intentionally bounded today:

- public, approved, non-sensitive sources only
- synthetic or explicitly non-sensitive FHIR examples only
- human review required for recommendations and governed actions
- no PHI handling
- no claim of production certification or compliance attestation

Those boundaries are part of the product posture. They keep the system more honest, easier to evaluate, and safer to demo.

## Quick start

Prerequisites:

- Java 21+
- Maven 3.9+
- Docker Desktop

Run the local stack:

```bash
cp infra/docker/.env.example infra/docker/.env
docker compose --env-file infra/docker/.env -f infra/docker/docker-compose.yml up --build -d
```

Open:

- UI: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`

For API-first local development:

```bash
docker compose --env-file infra/docker/.env -f infra/docker/docker-compose.yml up -d
cd apps/platform-api
mvn spring-boot:run
```

## Start here

- Want the fastest walkthrough? Read [the end-to-end demo guide](docs/36-end-to-end-demo-and-contributor-onboarding.md).
- Want a presentation-ready explanation? Read [the showcase architecture and solution narratives](docs/38-showcase-architecture-and-solution-narratives.md).
- Want a meeting-friendly story? Read [the demo and release story guide](docs/50-demo-and-release-story.md).
- Want the current product posture and next build recommendation? Read [the product readiness sweep](docs/51-product-readiness-sweep.md).
- Want the latest evidence and analyst-workspace improvements? Read [the Phase 21 evidence operations guide](docs/52-phase21-evidence-operations-and-research-quality.md) and [the Phase 22 analyst research workspace guide](docs/53-phase22-analyst-research-workspace.md).

## Documentation map

- [Platform API guide](apps/platform-api/README.md)
- [Client API surface](docs/23-client-api-surface.md)
- [End-to-end demo and contributor onboarding](docs/36-end-to-end-demo-and-contributor-onboarding.md)
- [Showcase architecture and solution narratives](docs/38-showcase-architecture-and-solution-narratives.md)
- [Demo and release story guide](docs/50-demo-and-release-story.md)
- [Product readiness sweep](docs/51-product-readiness-sweep.md)
- [Phase 21 evidence operations and research quality](docs/52-phase21-evidence-operations-and-research-quality.md)
- [Phase 22 analyst research workspace](docs/53-phase22-analyst-research-workspace.md)
- [Deployable editions and capability boundaries](docs/37-deployable-editions-and-capability-boundaries.md)
- [Private deployment operator guide](docs/31-private-deployment-operator-guide.md)
- [VS Code extension prototype](apps/vscode-extension/README.md)
- [HealthForge CLI](apps/cli/README.md)
- [HealthForge JavaScript SDK](packages/sdk-js/README.md)
- [Release notes](docs/releases.md)

## Release notes

Detailed milestone history and release-style progress updates live in [docs/releases.md](docs/releases.md).
