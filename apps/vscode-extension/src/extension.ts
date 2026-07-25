import * as fs from "node:fs/promises";
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

export function activate(context: vscode.ExtensionContext) {
  context.subscriptions.push(
    vscode.commands.registerCommand("healthforge.createBriefDraft", createBriefDraft),
    vscode.commands.registerCommand("healthforge.validateFhirExample", validateFhirExample),
    vscode.commands.registerCommand("healthforge.fhirStandardsAssistant", fhirStandardsAssistant)
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
    })
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
    body: JSON.stringify({ query })
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
    "X-HealthForge-Role": String(config.get("actorRole", "reviewer"))
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

export function deactivate() {
  // no-op
}
