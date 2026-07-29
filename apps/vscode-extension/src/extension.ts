import * as fs from "node:fs/promises";
import * as path from "node:path";
import * as vscode from "vscode";

type BriefResponse = {
  brief_id: string;
  status: string;
  summary: string;
  findings: Array<{
    finding_id: string;
    statement: string;
    citation: {
      source_id: string;
      source_version: string;
      locator: string;
      support: string;
    };
  }>;
  requires_human_review: boolean;
};

type ValidationResponse = {
  status: string;
  selected_package: {
    package_id: string;
    package_version: string;
    profile_url: string;
  };
  messages: Array<{ severity: string; location: string; message: string }>;
  summary: string;
};

type AssistantResponse = {
  status: string;
  summary: string;
  artifact_matches: Array<{
    title: string;
    artifact_type: string;
    canonical_url: string;
    support_boundary: string;
    evidence_links: string[];
  }>;
  review_notice: string;
};

type WorkspaceOverviewResponse = {
  organization_id: string;
  generated_at: string;
  projects: Array<{ project_id: string; name: string; kind: string; linked_brief_count: number }>;
  queues: Array<{ queue_name: string; item_count: number; summary: string }>;
  assignments: Array<{ assignment_id: string; brief_id: string; assignee_actor_id: string; status: string }>;
  saved_views: Array<{ view_id: string; name: string; view_type: string; summary: string }>;
  evidence_collections: Array<{ collection_id: string; name: string; source_count: number; summary: string }>;
};

type DeveloperOverviewResponse = {
  organization_id: string;
  generated_at: string;
  approved_briefs: Array<{
    brief_id: string;
    status: string;
    created_at: string;
    question: string;
  }>;
  workspace_surfaces: Array<{
    surface_id: string;
    title: string;
    workflow_type: string;
    summary: string;
    supported_actions: string[];
  }>;
  automation_recipes: Array<{
    recipe_id: string;
    title: string;
    command: string;
    summary: string;
    expected_outputs: string[];
  }>;
  delivery_guardrails: string[];
};

type SyntheticLabsOverviewResponse = {
  templates: Array<{
    template_id: string;
    title: string;
    workflow_type: string;
    summary: string;
  }>;
};

type SyntheticLabRunResponse = {
  template_id: string;
  title: string;
  summary: string;
  assertions: Array<{ assertion_id: string; title: string; status: string; evidence: string }>;
  timeline: Array<{ stage: string; summary: string }>;
  expected_outcomes: string[];
};

type DeveloperRepoGuidanceResponse = {
  brief_id: string;
  repository_name: string;
  workspace_root: string;
  generated_at: string;
  summary: string;
  repo_context: {
    inventory_count: number;
    changed_file_count: number;
    detected_technology_signals: string[];
    changed_files: string[];
  };
  implementation_focus: Array<{
    work_item_id: string;
    title: string;
    workflow_stage: string;
    affected_capability: string;
    rationale: string;
    dependencies: string[];
    standards_touchpoints: string[];
    validation_notes: string[];
  }>;
  file_suggestions: Array<{
    path: string;
    match_reason: string;
    recommendation: string;
    related_work_item_ids: string[];
  }>;
  automation_steps: Array<{
    step_id: string;
    title: string;
    command_hint: string;
    expected_outcome: string;
  }>;
  traceability_notes: string[];
  delivery_guardrails: string[];
};

export function activate(context: vscode.ExtensionContext) {
  context.subscriptions.push(
    vscode.commands.registerCommand("healthforge.createBriefDraft", createBriefDraft),
    vscode.commands.registerCommand("healthforge.validateFhirExample", validateFhirExample),
    vscode.commands.registerCommand("healthforge.fhirStandardsAssistant", fhirStandardsAssistant),
    vscode.commands.registerCommand("healthforge.openWorkspaceOverview", openWorkspaceOverview),
    vscode.commands.registerCommand("healthforge.listApprovedBriefs", listApprovedBriefs),
    vscode.commands.registerCommand("healthforge.generateRepoGuidance", generateRepoGuidance),
    vscode.commands.registerCommand("healthforge.runSyntheticLab", runSyntheticLab)
  );
}

async function createBriefDraft() {
  const question = await vscode.window.showInputBox({
    prompt: "HealthForge question",
    placeHolder: "What changes do we need for CMS prior authorization workflows?"
  });
  if (!question) {
    return;
  }
  const projectContext = await vscode.window.showInputBox({
    prompt: "Project context",
    placeHolder: "Synthetic provider EHR planning scenario for prior authorization APIs."
  });
  if (!projectContext) {
    return;
  }

  const response = await callApi<BriefResponse>("/v1/briefs", {
    method: "POST",
    body: JSON.stringify({
      corpus_id: "mvp-regulatory-corpus",
      corpus_version: "2026-07-24-expanded-web-core-v4",
      question,
      project_context: projectContext
    }),
    headers: actorHeaders()
  });

  showHtmlPanel(
    "HealthForge Brief Draft",
    `
      <h1>Brief draft created</h1>
      <p><b>Status:</b> ${escapeHtml(response.status)}</p>
      <p><b>Brief ID:</b> ${escapeHtml(response.brief_id)}</p>
      <p>${escapeHtml(response.summary)}</p>
      <p style="color:#8a5a00;"><b>Human review required:</b> ${response.requires_human_review ? "Yes" : "Unknown"}</p>
      <h2>Findings</h2>
      <ul>
        ${response.findings.map(finding => `
          <li>
            <p>${escapeHtml(finding.statement)}</p>
            <p><b>Citation:</b> ${escapeHtml(finding.citation.source_id)} ${escapeHtml(finding.citation.source_version)} · ${escapeHtml(finding.citation.locator)}</p>
            <p style="color:#586069;">${escapeHtml(finding.citation.support)}</p>
          </li>
        `).join("")}
      </ul>
    `
  );
}

async function validateFhirExample() {
  const profileUrl = await vscode.window.showInputBox({
    prompt: "FHIR profile URL",
    placeHolder: "http://hl7.org/fhir/StructureDefinition/Claim"
  });
  if (!profileUrl) {
    return;
  }

  const picked = await vscode.window.showOpenDialog({
    canSelectMany: false,
    openLabel: "Select FHIR JSON example",
    filters: { JSON: ["json"] }
  });
  if (!picked || picked.length === 0) {
    return;
  }

  const contents = await fs.readFile(picked[0].fsPath, "utf8");
  const response = await callApi<ValidationResponse>("/v1/fhir-validation/validate", {
    method: "POST",
    body: JSON.stringify({
      package_id: "hl7.fhir.r4.core",
      package_version: "4.0.1",
      profile_url: profileUrl,
      data_classification: "synthetic",
      resource: JSON.parse(contents)
    }),
    headers: actorHeaders()
  });

  showHtmlPanel(
    "HealthForge FHIR Validation",
    `
      <h1>FHIR validation result</h1>
      <p><b>Status:</b> ${escapeHtml(response.status)}</p>
      <p><b>Profile:</b> ${escapeHtml(response.selected_package.profile_url)}</p>
      <p><b>Package:</b> ${escapeHtml(response.selected_package.package_id)} ${escapeHtml(response.selected_package.package_version)}</p>
      <p style="color:#8a5a00;"><b>Human review note:</b> Human review remains required before implementation use.</p>
      <p>${escapeHtml(response.summary)}</p>
      <h2>Messages</h2>
      <ul>
        ${response.messages.map(message => `
          <li>
            <b>${escapeHtml(message.severity)}</b> · ${escapeHtml(message.location)}<br/>
            ${escapeHtml(message.message)}
          </li>
        `).join("")}
      </ul>
    `
  );
}

async function fhirStandardsAssistant() {
  const query = await vscode.window.showInputBox({
    prompt: "FHIR assistant query",
    placeHolder: "PAS claim profile"
  });
  if (!query) {
    return;
  }

  const response = await callApi<AssistantResponse>("/v1/fhir-assistant/query", {
    method: "POST",
    body: JSON.stringify({ query }),
    headers: actorHeaders()
  });

  showHtmlPanel(
    "HealthForge FHIR Assistant",
    `
      <h1>FHIR standards assistant</h1>
      <p><b>Status:</b> ${escapeHtml(response.status)}</p>
      <p>${escapeHtml(response.summary)}</p>
      <p style="color:#8a5a00;"><b>Review notice:</b> ${escapeHtml(response.review_notice)}</p>
      <h2>Curated matches</h2>
      <ul>
        ${response.artifact_matches.map(match => `
          <li>
            <b>${escapeHtml(match.title)}</b> (${escapeHtml(match.artifact_type)})<br/>
            <a href="${escapeHtml(match.canonical_url)}">${escapeHtml(match.canonical_url)}</a><br/>
            ${escapeHtml(match.support_boundary)}<br/>
            ${match.evidence_links.map(link => `<a href="${escapeHtml(link)}">${escapeHtml(link)}</a>`).join("<br/>")}
          </li>
        `).join("")}
      </ul>
    `
  );
}

async function openWorkspaceOverview() {
  const [workspace, developerOverview] = await Promise.all([
    callApi<WorkspaceOverviewResponse>("/v1/workspace/overview", {
      method: "GET",
      headers: actorHeaders()
    }),
    callApi<DeveloperOverviewResponse>("/v1/developer/overview", {
      method: "GET",
      headers: actorHeaders()
    })
  ]);

  showHtmlPanel(
    "HealthForge Workspace Overview",
    `
      <h1>Workspace overview</h1>
      <p><b>Organization:</b> ${escapeHtml(workspace.organization_id)}</p>
      <p><b>Generated:</b> ${escapeHtml(workspace.generated_at)}</p>
      <h2>Projects</h2>
      <ul>
        ${workspace.projects.map(project => `
          <li><b>${escapeHtml(project.name)}</b> (${escapeHtml(project.kind)}) · linked briefs: ${project.linked_brief_count}</li>
        `).join("")}
      </ul>
      <h2>Queues</h2>
      <ul>
        ${workspace.queues.map(queue => `
          <li><b>${escapeHtml(queue.queue_name)}</b> · ${queue.item_count} items · ${escapeHtml(queue.summary)}</li>
        `).join("")}
      </ul>
      <h2>Developer surfaces</h2>
      <ul>
        ${developerOverview.workspace_surfaces.map(surface => `
          <li>
            <b>${escapeHtml(surface.title)}</b> (${escapeHtml(surface.workflow_type)})<br/>
            ${escapeHtml(surface.summary)}<br/>
            ${surface.supported_actions.map(action => `• ${escapeHtml(action)}`).join("<br/>")}
          </li>
        `).join("")}
      </ul>
      <h2>Automation recipes</h2>
      <ul>
        ${developerOverview.automation_recipes.map(recipe => `
          <li>
            <b>${escapeHtml(recipe.title)}</b><br/>
            <code>${escapeHtml(recipe.command)}</code><br/>
            ${escapeHtml(recipe.summary)}
          </li>
        `).join("")}
      </ul>
    `
  );
}

async function listApprovedBriefs() {
  const overview = await callApi<DeveloperOverviewResponse>("/v1/developer/overview", {
    method: "GET",
    headers: actorHeaders()
  });

  if (overview.approved_briefs.length === 0) {
    vscode.window.showInformationMessage("No approved briefs are available yet for repo guidance.");
    return;
  }

  showHtmlPanel(
    "HealthForge Approved Briefs",
    `
      <h1>Approved briefs</h1>
      <p>These briefs are ready for implementation-oriented workflows.</p>
      <ul>
        ${overview.approved_briefs.map(brief => `
          <li>
            <b>${escapeHtml(brief.brief_id)}</b> · ${escapeHtml(brief.status)}<br/>
            ${escapeHtml(brief.question)}<br/>
            <span style="color:#586069;">Created: ${escapeHtml(brief.created_at)}</span>
          </li>
        `).join("")}
      </ul>
    `
  );
}

async function generateRepoGuidance() {
  const overview = await callApi<DeveloperOverviewResponse>("/v1/developer/overview", {
    method: "GET",
    headers: actorHeaders()
  });
  const approved = overview.approved_briefs;
  if (approved.length === 0) {
    vscode.window.showInformationMessage("No approved briefs are available yet for repo guidance.");
    return;
  }

  const pickedBrief = await vscode.window.showQuickPick(
    approved.map(brief => ({
      label: brief.brief_id,
      description: brief.status,
      detail: brief.question,
      brief
    })),
    { title: "Select an approved brief for repo guidance" }
  );
  if (!pickedBrief) {
    return;
  }

  const repoName = vscode.workspace.name ?? "healthforge-local-workspace";
  const rootPath = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath ?? process.cwd();
  const inventory = await gatherWorkspaceFiles(rootPath);
  const activeFile = vscode.window.activeTextEditor?.document.uri.fsPath;
  const guidance = await callApi<DeveloperRepoGuidanceResponse>("/v1/developer/repo-guidance", {
    method: "POST",
    headers: actorHeaders(),
    body: JSON.stringify({
      brief_id: pickedBrief.brief.brief_id,
      repository_name: repoName,
      workspace_root: rootPath,
      repository_inventory: inventory,
      changed_files: activeFile ? [relativeToRoot(rootPath, activeFile)] : []
    })
  });

  showHtmlPanel(
    "HealthForge Repo Guidance",
    `
      <h1>Repo-aware guidance</h1>
      <p><b>Brief:</b> ${escapeHtml(guidance.brief_id)}</p>
      <p><b>Repository:</b> ${escapeHtml(guidance.repository_name)}</p>
      <p>${escapeHtml(guidance.summary)}</p>
      <h2>Repo context</h2>
      <p>Inventory files: ${guidance.repo_context.inventory_count} · changed files: ${guidance.repo_context.changed_file_count}</p>
      <p>Signals: ${guidance.repo_context.detected_technology_signals.map(escapeHtml).join(", ")}</p>
      <h2>Implementation focus</h2>
      <ul>
        ${guidance.implementation_focus.map(item => `
          <li>
            <b>${escapeHtml(item.title)}</b> (${escapeHtml(item.workflow_stage)})<br/>
            Capability: ${escapeHtml(item.affected_capability)}<br/>
            ${escapeHtml(item.rationale)}<br/>
            <span style="color:#586069;">Dependencies: ${item.dependencies.map(escapeHtml).join(", ") || "n/a"}</span>
          </li>
        `).join("")}
      </ul>
      <h2>Suggested files</h2>
      <ul>
        ${guidance.file_suggestions.map(file => `
          <li>
            <b>${escapeHtml(file.path)}</b><br/>
            ${escapeHtml(file.match_reason)}<br/>
            ${escapeHtml(file.recommendation)}<br/>
            <span style="color:#586069;">Related work items: ${file.related_work_item_ids.map(escapeHtml).join(", ")}</span>
          </li>
        `).join("")}
      </ul>
      <h2>Automation steps</h2>
      <ul>
        ${guidance.automation_steps.map(step => `
          <li>
            <b>${escapeHtml(step.title)}</b><br/>
            <code>${escapeHtml(step.command_hint)}</code><br/>
            ${escapeHtml(step.expected_outcome)}
          </li>
        `).join("")}
      </ul>
    `
  );
}

async function runSyntheticLab() {
  const overview = await callApi<SyntheticLabsOverviewResponse>("/v1/synthetic-labs/overview", {
    method: "GET",
    headers: actorHeaders()
  });
  const picked = await vscode.window.showQuickPick(
    overview.templates.map(template => ({
      label: template.title,
      description: template.workflow_type,
      detail: template.summary,
      template
    })),
    { title: "Select a synthetic lab template" }
  );
  if (!picked) {
    return;
  }

  const run = await callApi<SyntheticLabRunResponse>("/v1/synthetic-labs/runs", {
    method: "POST",
    headers: actorHeaders(),
    body: JSON.stringify({ template_id: picked.template.template_id })
  });

  showHtmlPanel(
    "HealthForge Synthetic Lab",
    `
      <h1>${escapeHtml(run.title)}</h1>
      <p>${escapeHtml(run.summary)}</p>
      <h2>Assertions</h2>
      <ul>
        ${run.assertions.map(assertion => `
          <li><b>${escapeHtml(assertion.status)}</b> · ${escapeHtml(assertion.title)}<br/>${escapeHtml(assertion.evidence)}</li>
        `).join("")}
      </ul>
      <h2>Timeline</h2>
      <ul>
        ${run.timeline.map(item => `
          <li><b>${escapeHtml(item.stage)}</b> · ${escapeHtml(item.summary)}</li>
        `).join("")}
      </ul>
      <h2>Expected outcomes</h2>
      <ul>
        ${run.expected_outcomes.map(item => `<li>${escapeHtml(item)}</li>`).join("")}
      </ul>
    `
  );
}

async function gatherWorkspaceFiles(rootPath: string): Promise<string[]> {
  const files = await vscode.workspace.findFiles(
    "**/*",
    "**/{node_modules,target,out,.git,.idea,.vscode}/**",
    250
  );
  return files.map(file => relativeToRoot(rootPath, file.fsPath));
}

function relativeToRoot(rootPath: string, absolutePath: string): string {
  const relativePath = path.relative(rootPath, absolutePath);
  return relativePath.length > 0 ? relativePath : absolutePath;
}

async function callApi<T>(path: string, init: RequestInit): Promise<T> {
  const config = vscode.workspace.getConfiguration("healthforge");
  const baseUrl = String(config.get("apiBaseUrl", "http://localhost:8080")).replace(/\/$/, "");
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init.headers ?? {})
    }
  });

  if (!response.ok) {
    const text = await response.text();
    vscode.window.showErrorMessage(`HealthForge request failed: ${response.status} ${text}`);
    throw new Error(`HealthForge request failed: ${response.status}`);
  }

  return response.json() as Promise<T>;
}

function actorHeaders(): Record<string, string> {
  const config = vscode.workspace.getConfiguration("healthforge");
  return {
    "X-HealthForge-Actor": String(config.get("actorId", "vscode.reviewer")),
    "X-HealthForge-Role": String(config.get("actorRole", "reviewer")),
    "X-HealthForge-Organization": String(config.get("organizationId", "tenant.alpha"))
  };
}

function showHtmlPanel(title: string, body: string) {
  const panel = vscode.window.createWebviewPanel(
    "healthforge",
    title,
    vscode.ViewColumn.Beside,
    { enableScripts: false }
  );
  panel.webview.html = `
    <!doctype html>
    <html>
      <body style="font-family: system-ui; padding: 16px; line-height: 1.5;">
        ${body}
      </body>
    </html>
  `;
}

function escapeHtml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;");
}
