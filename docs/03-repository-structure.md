# Proposed repository structure

Start as a monorepo. Keep product code, infrastructure, and the regulated knowledge/evaluation assets visible but independently versioned.

```text
HealthForge/
├── README.md
├── docs/                         # Product, architecture, ADRs, runbooks
├── apps/
│   ├── web/                      # Next.js reviewer and workspace UI
│   └── platform-api/             # Java 21 / Spring Boot modular monolith
├── packages/
│   ├── contracts/                # OpenAPI, JSON Schema, shared API contracts
│   ├── fhir-artifacts/           # Pinned IG/package manifests and mappings
│   └── ui/                       # Optional shared UI system
├── knowledge/
│   ├── manifests/                # Sources, licenses, effective versions
│   ├── normalization/            # Parsing and section/citation rules
│   └── fixtures/                 # Non-sensitive sample documents and outputs
├── evals/
│   ├── datasets/                 # Expert-curated question/answer/citation cases
│   ├── rubrics/                  # Groundedness and usefulness criteria
│   └── reports/                  # Versioned evaluation results
├── infra/
│   ├── terraform/                # Azure infrastructure modules/environments
│   ├── docker/                   # Local development containers
│   └── kubernetes/               # Later AKS deployment manifests/charts
├── scripts/                      # Reproducible developer and CI utilities
├── .github/                      # CI, security, contribution automation
└── governance/                   # Contribution, security, data, and AI-use policies
```

## Boundaries inside `platform-api`

```text
platform-api/src/main/java/.../
├── identity/                     # Authentication/authorization adapters
├── projects/                     # Tenant/project context
├── knowledge/                    # Sources, versions, passages, retrieval interfaces
├── standards/                    # FHIR/IG artifact catalog and mapping
├── briefs/                       # Engineering brief lifecycle and review workflow
├── ai/                           # Model adapters, orchestration, guardrails
├── audit/                        # Evidence and immutable event records
├── integrations/                 # Future Jira/GitHub/IDE adapters
└── shared/                       # Small genuinely shared primitives only
```

Avoid creating microservices, Kafka topics, or a generic “agent framework” until real load, ownership, or deployment requirements justify them. Kafka is a plausible later integration backbone, not an MVP dependency.
