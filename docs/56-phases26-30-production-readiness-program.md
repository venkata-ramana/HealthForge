# Phases 26–30 — Production-readiness program

Phases 26–30 move HealthForge from a measurable pilot platform toward a controlled shared-deployment and rollout conversation.

## The readiness gate

```text
GET /v1/enterprise/production-readiness
```

The endpoint returns five scorecards:

| Phase | Scorecard |
| --- | --- |
| 26 | Secure shared deployment |
| 27 | Reliable interoperability runtime |
| 28 | Evidence and answer quality |
| 29 | Pilot operations and customer success |
| 30 | Production-readiness gate |

Each scorecard reports checks, status, evidence, ownership, and gaps. The overall decision is one of:

- `not_ready`
- `conditionally_ready`
- `ready_for_controlled_rollout`

## Important boundary

This is an evidence-oriented readiness gate, not a certification, clinical validation, compliance approval, or guarantee that an external system completed a handoff. Local-header identity, synthetic data, and simulated connector modes remain explicitly bounded until stronger deployment controls are introduced.

## How to use it

1. Open **Production readiness** from the administrator surface.
2. Review each phase scorecard and its owner.
3. Work through the open gaps in pilot operations and release reviews.
4. Re-run the gate after security, reliability, quality, and continuity evidence changes.
