# Guarded starter code generation

`POST /v1/codegen/starter-artifacts` is the first guarded code-generation prototype in HealthForge.

Its scope is intentionally narrow:

- input must reference an **approved** Brief and an exported approved work item;
- output is limited to clearly labeled **example starter code**;
- the generated artifact preserves traceability to the source Brief, work item, findings, standards touchpoints, and validation notes; and
- no repository writeback, deployment, or production-readiness claim is made.

The current prototype supports a small starter-artifact set:

- `spring_boot_endpoint_stub`
- `spring_service_stub`

The generated code is meant to help reviewers and engineers start a conversation, not skip review. It should be treated as scaffolding that still requires:

- human engineering review;
- architecture review;
- security review;
- interoperability and standards review; and
- implementation-specific adaptation before use.

This keeps HealthForge aligned with the project boundary: the platform may suggest and structure implementation work, but it does not become the system of record for autonomous production code changes.
