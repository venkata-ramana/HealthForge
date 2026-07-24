# MVP security, data boundaries, and threat model

## Non-negotiable MVP boundary

HealthForge MVP accepts only public, non-sensitive source material and non-sensitive engineering context. It must not accept, store, log, or send protected health information (PHI) to a model provider or any other third party. It is engineering decision support, not a clinical system, compliance certification service, or authorization system.

Before any PHI-enabled capability, HealthForge requires a separate security design, data inventory, access control model, retention schedule, incident response plan, vendor assessment, and applicable contractual and regulatory review.

## Data flows

| Flow | Purpose | Classification | MVP control |
| --- | --- | --- | --- |
| Public publisher → ingestion | Acquire manifest-approved regulation or implementation guidance | Public | Exact allowlist URL, expected MIME type, same-host redirect check, size limit, checksum |
| API client → local API | Submit non-sensitive question, context, and review rationale | Internal non-sensitive | Request-size limits; never use real patient data; do not persist answer packet input |
| Local API → PostgreSQL/artifact store | Preserve source provenance, Briefs, and review decisions | Public source + internal non-sensitive | Local development storage; immutable source checksum; least-privilege database credentials |
| Local API → model provider | Not implemented in MVP | No data permitted | External model calls disabled; any future integration must default-deny PHI and secrets |

## Initial threat model

| Threat | Primary control | Owner | Decision |
| --- | --- | --- | --- |
| Arbitrary or malicious document ingestion | Manifest allowlist, MIME/redirect/size validation, PDF parsing boundary | Platform maintainer | Mitigate in MVP |
| Unsupported answer interpreted as advice | Evidence-only packets, citations, `insufficient_evidence`, human review notices | Product owner | Mitigate in MVP |
| PHI included in question or context | Public/non-sensitive use policy, no model transmission, contributor guidance | Product owner | Mitigate; automated PHI detection is future work |
| Secret exposure | Environment variables only; never commit credentials; rotation after disclosure | Repository administrator | Mitigate in MVP |
| Unauthorized Brief review | Local prototype only; future identity and role authorization are required before shared deployment | Platform maintainer | Defer with deployment gate |
| Vulnerable dependencies | Dependency review and CI scanning before production deployment | Platform maintainer | Open risk; add automation in roadmap |

## Logging, retention, and secrets

- Do not log request bodies, source document text, credentials, tokens, or database connection strings.
- Retain public artifacts only while they remain needed for reproducibility; preserve checksums and provenance for any retained Brief.
- Keep local development databases and artifacts out of version control.
- Use a secret manager and defined retention/deletion schedule before any shared environment.
- Report suspected security vulnerabilities privately through the process in [`SECURITY.md`](../SECURITY.md).
