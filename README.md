# HealthForge

An open-source AI engineering platform for healthcare interoperability.

HealthForge translates authoritative healthcare regulations and implementation guidance into traceable, reviewable engineering work: requirements, FHIR mappings, implementation plans, validation guidance, and evidence.

## Current phase

**Discovery and architecture.** No application code has been started. The initial product boundary and proposed architecture are documented in [`docs/`](docs/).

## Starting point

The first usable vertical slice is a **Regulation-to-Engineering Brief**:

1. An authorized source document is ingested with provenance.
2. A user asks a bounded engineering question.
3. The platform returns cited findings, affected personas/capabilities, FHIR and workflow implications, and proposed work items.
4. A human reviewer accepts, corrects, or rejects each recommendation.

This deliberately precedes code generation, automated compliance claims, and production PHI handling.

## Documentation

- [`docs/01-problem-and-product-scope.md`](docs/01-problem-and-product-scope.md)
- [`docs/02-target-architecture.md`](docs/02-target-architecture.md)
- [`docs/03-repository-structure.md`](docs/03-repository-structure.md)
- [`docs/04-first-90-days.md`](docs/04-first-90-days.md)
- [`docs/05-decision-log.md`](docs/05-decision-log.md)
- [`docs/06-mvp-source-corpus.md`](docs/06-mvp-source-corpus.md)
- [`docs/07-electronic-prior-authorization-workflow.md`](docs/07-electronic-prior-authorization-workflow.md)
- [`docs/08-regulation-to-engineering-brief-contract.md`](docs/08-regulation-to-engineering-brief-contract.md)
- [`docs/09-evaluation-and-review-rubric.md`](docs/09-evaluation-and-review-rubric.md)
- [`docs/10-ingestion-provenance-and-retrieval-contract.md`](docs/10-ingestion-provenance-and-retrieval-contract.md)
- [`knowledge/manifests/mvp-source-corpus.yaml`](knowledge/manifests/mvp-source-corpus.yaml)
- [`knowledge/fixtures/regulation-to-engineering-brief.example.json`](knowledge/fixtures/regulation-to-engineering-brief.example.json)
- [`packages/contracts/regulation-to-engineering-brief.schema.json`](packages/contracts/regulation-to-engineering-brief.schema.json)
- [`packages/contracts/knowledge-ingestion-retrieval.openapi.yaml`](packages/contracts/knowledge-ingestion-retrieval.openapi.yaml)
- [`evals/datasets/cms-0057-f-mvp-evaluation-cases.json`](evals/datasets/cms-0057-f-mvp-evaluation-cases.json)

## Local evidence service

The first executable vertical slice lives in [`apps/platform-api`](apps/platform-api/). It accepts manifest-approved, public source-ingestion requests, records immutable provenance metadata in PostgreSQL, indexes page-level PDF passages, and exposes retrieval plus deterministic cited evidence packets.

See its [local setup guide](apps/platform-api/README.md).
