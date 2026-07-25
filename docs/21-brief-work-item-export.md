# Brief work-item export

`GET /v1/briefs/{briefId}/work-item-export` turns an **approved** Brief into a non-sensitive machine-readable implementation-work-item artifact.

This export is intentionally narrower than future tracker integrations:

- it is available only when the Brief status is `approved`;
- it includes only findings with an accepted review decision;
- it preserves evidence, approval history, and audit context; and
- it does not write to GitHub, Jira, or any external system.

The export is meant for human review outside the app before any downstream backlog or repository action is attempted. It is a structured handoff artifact, not an automated change request.

The exported work items include:

- a reviewable title and rationale;
- an affected capability label;
- standards touchpoints drawn from the Brief's technical-guidance sources when available;
- validation notes that restate the approval and non-writeback boundary; and
- source-backed evidence tied to the accepted review decision.

This keeps the HealthForge boundary intact: recommendations may be organized and exported, but a human still decides whether they become tracker items, architecture work, or code changes.
