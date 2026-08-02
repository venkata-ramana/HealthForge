# Phases 37–40 — SaaS readiness and launch gates

Phases 37–40 complete the next operating layer after tenant identity hardening. Together they make the SaaS direction explicit without overstating the current deployment as a production SaaS control plane.

## Phase 37 — Enterprise identity integration

The platform now has a tenant-scoped identity contract, trusted-proxy group mapping, and configurable active-membership enforcement. Production deployments can enable `HEALTHFORGE_AUTH_ENFORCE_MEMBERSHIP=true` while keeping local-header mode convenient for demos.

Remaining deployment work is environment-specific: connect an identity provider, use signed identity assertions, and exercise joiner/mover/leaver lifecycle controls.

## Phase 38 — Tenant provisioning lifecycle

Tenant administrators can inspect isolation boundaries, submit provisioning requests, list organization members, and create role-scoped invitations. These workflows make onboarding intent and ownership visible before infrastructure automation is introduced.

Automated infrastructure provisioning, invitation delivery, activation callbacks, and environment reconciliation remain follow-on deployment capabilities.

## Phase 39 — Usage and commercial controls

Tenant analytics, pilot funnel reporting, usage summaries, packaging views, and soft quota status provide explainable adoption and capacity signals. These are advisory controls for private pilots and evaluation work, not billing enforcement.

Hard quotas, metering retention, billing, and contract-system integration remain future work.

## Phase 40 — Security and SaaS launch gates

The production-readiness scorecard, controlled-rollout evidence registry, access-review reporting, continuity views, and operator attestations now form a single launch-gate story. The aggregate endpoint is:

```text
GET /v1/enterprise/saas-readiness
```

It is restricted to auditors and administrators and returns the delivered evidence, launch gates, limitations, and next actions for Phases 37–40.

## What “complete” means here

These phases complete the **SaaS-ready operating foundation**. They do not certify production SaaS, PHI handling, compliance, billing, or unattended automation. A real launch still requires environment-specific identity, security review, resilience rehearsal, ownership approval, and deployment controls.
