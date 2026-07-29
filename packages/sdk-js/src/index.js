class HealthForgeClient {
  constructor(config = {}) {
    this.baseUrl = String(config.baseUrl || "http://localhost:8080").replace(/\/$/, "");
    this.actorId = config.actorId || "sdk.reviewer";
    this.actorRole = config.actorRole || "reviewer";
    this.organizationId = config.organizationId || "tenant.alpha";
  }

  async listBriefs() {
    return this.request("/v1/briefs", { method: "GET" });
  }

  async createBrief(payload) {
    return this.request("/v1/briefs", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  }

  async getWorkspaceOverview() {
    return this.request("/v1/workspace/overview", { method: "GET" });
  }

  async getDeveloperOverview() {
    return this.request("/v1/developer/overview", { method: "GET" });
  }

  async getRepoGuidance({ briefId, repositoryName, workspaceRoot, repositoryInventory = [], changedFiles = [] }) {
    return this.request("/v1/developer/repo-guidance", {
      method: "POST",
      body: JSON.stringify({
        brief_id: briefId,
        repository_name: repositoryName,
        workspace_root: workspaceRoot,
        repository_inventory: repositoryInventory,
        changed_files: changedFiles
      }),
      headers: this.headers({ actorRole: "approver" })
    });
  }

  async getSyntheticLabsOverview() {
    return this.request("/v1/synthetic-labs/overview", { method: "GET" });
  }

  async runSyntheticLab(templateId) {
    return this.request("/v1/synthetic-labs/runs", {
      method: "POST",
      body: JSON.stringify({ template_id: templateId })
    });
  }

  async request(path, init) {
    const response = await fetch(`${this.baseUrl}${path}`, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...this.headers(),
        ...(init.headers || {})
      }
    });
    if (!response.ok) {
      throw new Error(`HealthForge request failed: ${response.status} ${await response.text()}`);
    }
    return response.json();
  }

  headers(overrides = {}) {
    return {
      "X-HealthForge-Actor": overrides.actorId || this.actorId,
      "X-HealthForge-Role": overrides.actorRole || this.actorRole,
      "X-HealthForge-Organization": overrides.organizationId || this.organizationId
    };
  }
}

module.exports = { HealthForgeClient };
