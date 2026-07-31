# Phase 25 — Pilot success and expansion analytics

Phase 25 turns HealthForge from a feature-rich pilot surface into a more measurable pilot program.

## What shipped

- organization-scoped usage funnel metrics from Brief creation through review, approval, and implementation handoff
- completion and drop-off rates for the core workflow
- role-based activity summaries for reviewers, approvers, auditors, and administrators
- stakeholder-ready value evidence and sponsor questions
- structured feedback capture for evidence quality, reviewer confidence, and recommendation usefulness
- demo-to-pilot-to-rollout expansion scorecards with explicit gaps

## API surface

```text
GET  /v1/pilot/analytics
POST /v1/pilot/analytics/feedback
```

The read endpoint is available to auditors and administrators. Feedback can be recorded by reviewers, approvers, and administrators.

## How to interpret the metrics

The current local platform does not persist a separate raw-question event stream, so created engineering Briefs are used as the question-start proxy. Implementation handoffs count tracked exports, documentation exports, and collaboration notifications; they show an observable HealthForge handoff, not proof that an external system completed the work.

The expansion score is evidence-oriented:

1. workflow started
2. human review observed
3. approval gate exercised
4. governed handoff exercised
5. feedback loop active

Each check contributes 20 points. The score supports honest pilot conversations and does not certify production, regulatory, clinical, or external-system readiness.
