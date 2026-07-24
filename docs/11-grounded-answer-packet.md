# Grounded answer packet

`POST /v1/answers` is the first application-level step after retrieval. It is deliberately deterministic: it does not call an external model, persist a question or project context, or synthesize a legal, clinical, regulatory, or compliance conclusion.

## Behavior

1. The service searches the requested local corpus using the question and optional source-type filter.
2. When passages are found, it returns `grounded` with one finding per retrieved excerpt. Every finding carries its passage ID, source ID, source version, source type, canonical URL, and stable page locator.
3. When no passage is found, it returns `insufficient_evidence`, a null answer, and no findings. It never substitutes generic or plausible prose.
4. Every response includes an explicit human-review notice and limitation.

The endpoint is an evidence packet for the future Regulation-to-Engineering Brief workflow, not the Brief itself. A later orchestration slice may synthesize structured findings, implications, and work items only after enforcing the existing Brief citation and review rules.

## Input boundary

`question` and optional `project_context` are bounded, request-only inputs. They must be non-sensitive; HealthForge's MVP corpus and this endpoint are not authorized for PHI. The local endpoint does not transmit these inputs to a model provider or persist them.
