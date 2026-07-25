# Architecture review assistant

`POST /v1/architecture-reviews` provides a bounded, non-sensitive architecture-review artifact for prior-authorization solution design.

The workflow is intentionally constrained:

- it accepts only scenario text and corpus selection that fit the current no-PHI project boundary;
- it reuses grounded evidence retrieval instead of uncited free-form generation;
- it supplements evidence with curated standards-artifact touchpoints when they match the scenario; and
- it always marks the output as human-review-required.

The response is designed to help a reviewer connect a question to likely:

- internal service boundaries;
- external integration surfaces;
- standards touchpoints;
- assumptions that still need validation;
- engineering risks; and
- review checkpoints before implementation.

This is not an auto-architecture engine. It does not provision infrastructure, choose production topology automatically, or make conformance claims. Its role is to create a structured review artifact that engineering, product, architecture, and compliance stakeholders can inspect together.
