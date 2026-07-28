# Deployable editions and capability boundaries

HealthForge is easier to explain when the deployment stories and capability boundaries are explicit.

## Edition framing

### Showcase / demo edition

Best for:

- product walkthroughs
- technical demos
- early evaluator conversations

Includes:

- web UI
- grounded answers
- Brief workflow
- approvals and audit trail
- evaluation and safety reporting
- synthetic FHIR validation and scenario generation

Excludes:

- PHI use
- production identity
- production connector credentials

### Community / builder edition

Best for:

- contributors
- local prototyping
- open-source workflow experimentation

Includes:

- all showcase capabilities
- API-first access
- VS Code prototype
- local regression scripts
- operator-facing docs

Excludes:

- hardened multi-tenant production deployment
- customer support or managed operations

### Enterprise-oriented private deployment story

Best for:

- private pilot conversations
- architecture evaluation
- controlled environment planning

Includes:

- governed integration scaffolding
- access review and identity directory
- deployment promotion guidance
- policy and safety reporting

Still out of scope:

- PHI production use
- full SSO rollout
- secret-management automation in-product
- customer-grade unattended automation

## Why this framing matters

This lets us communicate confidently without overstating maturity:

- the showcase edition explains the product clearly
- the community edition supports contributors and open-source growth
- the enterprise-oriented story shows where private deployment becomes credible without pretending the platform is already fully productized for regulated production
