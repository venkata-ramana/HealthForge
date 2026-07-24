# Persisted Brief drafts and review decisions

`POST /v1/briefs` creates a `draft` only from a grounded evidence packet. The service stores its bounded input, cited source registry, and findings in PostgreSQL. It rejects creation when retrieval has no citeable evidence.

`POST /v1/briefs/{briefId}/review-decisions` appends a reviewer decision for a finding. Each decision records reviewer, timestamp, rationale, and any corrected statement; it does not overwrite the generated finding. A rejection, correction, or request for information moves the Brief to `changes_requested`; an acceptance moves it to `in_review`.

This is intentionally not an approval endpoint. Approval requires the broader contract controls: qualified reviewer authorization, corpus-snapshot validation, and no unresolved blocker questions.
