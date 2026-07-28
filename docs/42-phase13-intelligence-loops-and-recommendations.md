# Phase 13: intelligence loops and recommendations

Phase 13 helps HealthForge learn from repeated review and evaluation patterns without replacing human judgment.

It adds a bounded advisory intelligence layer for:

- retrieval feedback
- evidence-gap summaries
- similarity clustering
- persona-aware next-step recommendations
- workflow-tuning suggestions

## What shipped

### Retrieval feedback loops

Reviewers and approvers can now capture retrieval feedback directly from Brief findings.

Supported feedback types:

- helpful
- missing evidence
- ranking issue
- duplicate result

This creates a measurable feedback loop instead of relying only on informal reviewer memory.

### Evidence-gap recommendations

HealthForge now turns insufficient-evidence and source-health signals into advisory corpus-expansion guidance.

That helps operators answer:

- where is the corpus thin?
- which public-source categories might help?
- which gaps are most urgent?

### Similarity clustering

Phase 13 introduces lightweight similarity clusters across recent Brief questions.

These clusters help teams:

- spot repeated work
- find adjacent artifacts
- reuse prior review context more intelligently

The signals stay advisory and never bypass review.

### Persona-aware recommendations

The intelligence layer now gives role-aware next-step guidance for:

- reviewers
- approvers
- auditors
- administrators

Recommendations are stage-aware and explain why the action is being suggested.

### Workflow tuning recommendations

Evaluation and trust signals now feed into product-improvement recommendations such as:

- retrieval tuning priorities
- disagreement-driven review improvements
- delivery-stage friction reduction

This makes evaluation output more actionable for operators.

## New API surface

- `GET /v1/intelligence/overview`
- `POST /v1/intelligence/retrieval-feedback`

## Product experience

Phase 13 is visible in two places:

- Brief Studio, where reviewers can capture retrieval feedback on a finding
- Admin Console, where operators can inspect the intelligence overview

## Boundaries

Phase 13 recommendations are intentionally bounded:

- advisory only
- review-first
- explainable from persisted telemetry
- not an automated decision engine

The goal is to make the next useful action clearer, not to automate judgment away.
