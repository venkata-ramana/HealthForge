# Persisted Brief drafts and review decisions

`POST /v1/briefs` creates a `draft` only from a grounded evidence packet. The service stores its bounded input, cited source registry, and findings in PostgreSQL. It rejects creation when retrieval has no citeable evidence.

`POST /v1/briefs/{briefId}/review-decisions` appends a reviewer decision for a finding. Each decision records reviewer, timestamp, rationale, and any corrected statement; it does not overwrite the generated finding. A rejection, correction, or request for information moves the Brief to `changes_requested`; an acceptance moves it to `in_review`.

For the local shared-review preparation slice, authenticated write actions must include a local actor ID and role header. The service appends immutable audit events for Brief creation, evidence selection, and review decisions so a future shared deployment can replace header-based identity with stronger authentication without losing workflow traceability.

This is intentionally not an approval endpoint. Approval requires the broader contract controls: qualified reviewer authorization, corpus-snapshot validation, and no unresolved blocker questions.
