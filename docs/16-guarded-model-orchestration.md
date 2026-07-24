# Guarded model orchestration

`/v1/model-synthesis` accepts only a grounded evidence packet with complete source/version/locator citations. The provider is disabled by default (`HEALTHFORGE_MODEL_ENABLED=false`); disabled requests return a clear response before any provider call can occur. No provider adapter, credential, or external transmission exists in this MVP slice.

Before enabling an adapter, require approved provider terms, a no-PHI gate, snapshot-pinned evidence inputs, structured Brief-schema output validation, citation coverage validation, and run-record persistence for provider/model/prompt configuration.
