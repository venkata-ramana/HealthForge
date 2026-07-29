# HealthForge JavaScript SDK

This package provides a very small builder-facing JavaScript client for the currently supported local HealthForge workflows.

## Included helpers

- list and create Briefs
- inspect workspace and developer overviews
- request repo-aware implementation guidance from an approved Brief
- inspect and run synthetic interoperability labs

## Example

```js
const { HealthForgeClient } = require("@healthforge/sdk-js");

const client = new HealthForgeClient({
  baseUrl: "http://localhost:8080",
  actorId: "demo.approver",
  actorRole: "approver",
  organizationId: "tenant.alpha"
});

const overview = await client.getDeveloperOverview();
console.log(overview.workspaceSurfaces);
```

## Boundaries

- local-only support story today
- no production auth or PHI handling
- grounded in the currently supported HealthForge API surface only
