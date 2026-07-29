# Phase 17: synthetic interoperability labs

Phase 17 strengthens HealthForge as a sandbox-safe experimentation and testing environment for healthcare interoperability workflows.

The goal is not to simulate real patient or payer production traffic. The goal is to make synthetic workflow rehearsal more realistic, more repeatable, and easier to inspect.

## What shipped

- richer synthetic scenario templates for provider, payer, coverage-discovery, documentation, and negative validation drills
- end-to-end workflow lab runs that combine synthetic payloads, bundle review, validation, and workflow journey modeling
- scenario assertions and expected outcomes for regression-style testing
- replay and comparison views for happy-path versus negative-path workflow runs
- coverage and validation-gap reporting for the current synthetic lab surface

## Why it matters

Before this phase, HealthForge already had:

- synthetic FHIR payload generation
- bundle review
- workflow journey modeling
- deterministic validation

Phase 17 connects those into a more useful lab model:

- scenario runs are easier to reuse
- workflow stages are easier to compare
- negative drills are easier to explain
- sandbox demos gain more technical depth
- coverage gaps stay visible instead of being implied away

## New synthetic lab surfaces

### Scenario templates

HealthForge now ships reusable synthetic lab templates for:

- provider PAS submission baseline
- payer decision and follow-up bundle
- coverage discovery rehearsal
- documentation capture rehearsal
- negative bundle structure drill

These templates package:

- workflow type
- actor focus
- synthetic payload source
- expected validation behavior
- coverage tags

### Workflow lab runs

Each synthetic lab run now combines:

- synthetic scenario selection
- bundle-level review
- workflow journey rendering
- assertions
- expected outcomes
- timeline output

This makes the lab feel more like a repeatable engineering test surface than a one-off fixture browser.

### Replay and comparison

Synthetic lab runs can now be compared to one another.

This helps teams inspect:

- journey type differences
- validation differences
- resource-composition differences
- stage-by-stage workflow divergence

It is especially useful for comparing:

- happy path vs negative path
- PAS vs CRD
- CRD vs DTR

### Coverage and validation gaps

The synthetic lab overview now includes:

- support matrix by workflow area
- validation-gap summaries
- recommended next scenario directions

That makes it easier to explain what the labs already cover and what still needs to be modeled later.

## Demo path

1. Open `Synthetic labs`
2. Run `Provider PAS submission baseline`
3. Compare it with `Negative bundle structure`
4. Run `Coverage discovery rehearsal`
5. Run `Documentation capture rehearsal`
6. Review the support matrix and validation gaps

That gives a strong Phase 17 story:

- richer synthetic scenarios
- end-to-end workflow rehearsals
- repeatable assertions
- replay and diff tooling
- explicit lab coverage boundaries

## Boundaries

Phase 17 keeps the same sandbox-safe boundaries:

- synthetic and non-sensitive examples only
- no production traffic
- no PHI handling
- no counterparty support claim
- no compliance or conformance claim

What changed is that synthetic workflow testing is now easier to rehearse, compare, and explain without crossing those boundaries.
