# Regulation Explainer Workflow

The Phase 4 regulation explainer is a regulation-first workflow that turns a selected approved source into a plain-English technical explainer with citations.

## Purpose

Provide a cleaner entry point when a reviewer wants to ask:

- what does this regulation say
- what does it imply for engineering
- what remains unresolved

## Input shape

The workflow is bounded by:

- corpus id
- corpus version
- selected source id
- question
- optional project context

## Output shape

The explainer returns:

- plain-English summary
- technical implications
- assumptions
- unresolved questions
- cited findings from the selected source
- follow-on workflow suggestions

## Boundary

This workflow:

- stays within the selected approved source
- does not certify legal or compliance interpretation
- keeps human review mandatory
- can feed later architecture or implementation workflows
