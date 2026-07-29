# Phase 19 multi-tenant foundations

Phase 19 turns HealthForge’s existing organization-aware model into a clearer product foundation for multiple customers and hosted-product conversations.

## What shipped

- a tenant administration API surface for customer overview, isolation boundaries, role delegation, and hosted packaging artifacts
- provisioning workflows for private customer spaces and hosted evaluation environments
- tenant analytics and usage summaries for customer-success and hosted-product discussions
- admin-console support for tenant administration directly in the web UI
- release and API documentation updates for the multi-tenant product story

## Why it matters

Before this phase, HealthForge already used organization-scoped records internally, but the product story for multiple customers was still implicit.

After this phase:

- tenant and customer administration are visible in the product
- provisioning conversations have concrete request artifacts
- isolation boundaries are easier to explain
- hosted-product discussions have better technical grounding

## New tenant administration workflows

### Tenant overview

`GET /v1/admin/tenants/overview`

This summarizes:

- customer tenant landscape
- hosted/private deployment posture
- tenant isolation boundaries
- delegated customer admin roles
- provisioning requests
- hosted packaging artifacts

### Provisioning requests

`POST /v1/admin/tenants/provisioning-requests`

Use this to describe a tenant-space setup request with:

- tenant key and name
- deployment model
- environment shape
- delegated admin owner
- requested capabilities
- onboarding summary

This is intentionally a product workflow artifact, not an infrastructure automation claim.

### Tenant analytics

`GET /v1/admin/tenants/analytics`

This adds:

- tenant counts by posture
- usage summaries
- engagement signals
- packaging-fit views for private and hosted product conversations

## Product boundaries

Phase 19 improves the multi-tenant product story, but it still stays honest about current scope:

- no production SaaS control plane
- no cross-tenant shared data access for customer artifacts
- no automated infrastructure provisioning
- no PHI or production identity claims

## Demo path

1. Switch to `administrator`.
2. Open `Tenant admin` from the admin console.
3. Review the customer tenant list and isolation boundaries.
4. Create a new provisioning request for a private customer space.
5. Walk through tenant analytics and hosted packaging artifacts.

## What this unlocks next

This phase sets up cleaner conversations around:

- enterprise customer onboarding
- hosted evaluator offerings
- delegated tenant administration
- future environment and compliance packaging work
