# FHIR Knowledge Assistant

The Phase 4 FHIR Knowledge Assistant is a bounded standards-native workflow over the curated standards artifact registry and pinned FHIR validation catalog.

## Purpose

Help developers inspect:

- resources
- profiles
- operation or workflow touchpoints
- implementation guides

without treating those artifacts as automatic implementation approval.

## Input shape

The workflow accepts:

- a query
- an optional artifact type
- an optional package filter

## Output shape

The assistant returns:

- matched curated artifacts
- matched validation packages/profiles
- support boundaries
- evidence links
- unsupported-request warnings
- human-review notice

## Boundary

This workflow:

- is not a compliance engine
- is not a conformance certification tool
- does not guarantee payer-specific support
- does not replace deployment-specific standards review
