# Phase 9 evaluation and trust

Phase 9 adds a first-class trust layer on top of HealthForge's review-first workflows.

## What Phase 9 adds

- a dedicated evaluation dashboard for retrieval, citation, evidence, review, and workflow quality
- a policy and safety report for unsupported outputs, approval policy, and governed delivery boundaries
- persisted answer telemetry so insufficient-evidence and unsupported-output patterns are visible over time
- reviewer disagreement and decision-consistency signals
- operator-friendly visibility into the latest pinned regression baseline under `evals/`

## New endpoints

- `GET /v1/evaluation/dashboard`
- `GET /v1/evaluation/policy-safety-report`

## Trust model

The new reporting layer does not claim that HealthForge is compliant or production-ready. It does three smaller and more useful things:

- shows what the latest regression baseline says
- shows how the live review workflow is behaving
- shows where source freshness, evidence sufficiency, or unsupported-output boundaries may need attention

## Important interpretation boundary

Disagreement metrics and consistency alerts are workflow-quality signals, not automated judgments about which reviewer was "right."
