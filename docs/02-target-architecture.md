# Target architecture

## Architectural stance

Build a modular monolith first: one deployable application with clear internal module boundaries and independently deployable infrastructure services where warranted. This avoids premature distributed-system complexity while preserving a path to separate services later.

The platform must separate authoritative content, retrieval, AI orchestration, domain rules, and human decisions. An LLM may synthesize and propose; it must not be the system of record for regulations, standards mappings, or approvals.

## Context diagram

```mermaid
flowchart LR
  U["Healthcare engineering team"] --> W["HealthForge web application"]
  A["Platform/API clients"] --> P["HealthForge API"]
  S["Authoritative sources\nCMS, HL7 FHIR, implementation guides"] --> I["Curated ingestion pipeline"]
  I --> K["Versioned knowledge store"]
  W --> P
  P --> O["AI orchestration and evidence assembly"]
  O --> K
  O --> M["Approved model provider"]
  P --> D["Projects, briefs, reviews, audit trail"]
  P -. "later: approved export" .-> T["Issue trackers and repositories"]
```

## MVP container view

```mermaid
flowchart TB
  UI["Next.js web UI\nbrief review and evidence"] --> API["Spring Boot API\nidentity, projects, briefs, policy"]
  API --> ORCH["AI orchestration\nretrieval, prompts, structured output"]
  API --> PG[("PostgreSQL\nprojects, provenance, reviews, audit")]
  ORCH --> VS[("Vector index\nsource chunks and embeddings")]
  ORCH --> OBJ[("Object storage\nsource documents")]
  ORCH --> LLM["Model provider"]
  ING["Ingestion worker\nparse, normalize, version, index"] --> OBJ
  ING --> PG
  ING --> VS
  API --> Q["Job queue/cache\nRedis"]
  Q --> ING
```

## Core domain objects

| Object | Purpose |
| --- | --- |
| SourceDocument / SourceVersion | Original authoritative artifact plus origin, effective dates, checksum, license, and ingestion metadata |
| SourcePassage | A stable citeable excerpt with page/section locator |
| StandardArtifact | FHIR resource, profile, value set, operation, or implementation-guide reference |
| Project | A customer's isolated engineering context and selected supported corpus |
| EngineeringBrief | The generated, versioned technical-impact output |
| Finding | A claim, recommendation, uncertainty, or requirement; linked to passages |
| ReviewDecision | Human acceptance/rejection/correction of a finding |
| EvidenceRecord | Immutable trail connecting a brief, inputs, model/run configuration, citations, and review |

## Trust and safety controls from day one

- Version and preserve sources; never cite an untraceable retrieved chunk.
- Require structured model output and validate it against schemas.
- Enforce citation coverage for claims tagged as requirements or recommendations.
- Display confidence, assumptions, applicable version, and “human review required” prominently.
- Maintain append-only audit events for ingestion, generation, export, and review.
- Do not accept PHI in the MVP. Add formal threat modeling, access controls, retention policies, and compliance work before changing this boundary.

## Evolution path

1. **MVP:** curated corpus, single organization, brief generation and review.
2. **Validation:** add FHIR payload validation using HAPI FHIR and pinned IG packages; keep it deterministic and separate from AI explanation.
3. **Workflow integrations:** signed, least-privilege exports after explicit human approval.
4. **Enterprise:** tenant isolation, SSO/RBAC, audit export, private deployment, and policy-controlled model routing.
