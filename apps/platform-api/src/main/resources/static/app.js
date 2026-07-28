const api = '/v1/briefs';
const defaultCorpusId = 'mvp-regulatory-corpus';
const defaultCorpusVersion = '2026-07-24-expanded-web-core-v4';

const esc = (value) => String(value ?? '').replace(/[&<>"]/g, (char) => ({
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;'
}[char]));

const roleRank = { reviewer: 1, approver: 2, auditor: 3, administrator: 4 };
const roleRequirements = {
  createBrief: 'reviewer',
  reviewFinding: 'reviewer',
  approveBrief: 'approver',
  exportWorkItems: 'approver',
  workspaceOverview: 'reviewer',
  workspaceProject: 'reviewer',
  workspaceAssignment: 'reviewer',
  workspaceSavedView: 'reviewer',
  inboundCases: 'reviewer',
  orchestrationTemplates: 'reviewer',
  intelligenceOverview: 'reviewer',
  integrationStatus: 'administrator',
  auditExport: 'auditor',
  complianceDashboard: 'auditor',
  enterprisePosture: 'auditor',
  evaluationDashboard: 'auditor',
  policySafetyReport: 'auditor',
  identityDirectory: 'administrator',
  accessReview: 'administrator',
  deploymentGuide: 'administrator'
};

const demoScenarios = [
  {
    id: 'cms-prior-auth',
    title: 'CMS prior authorization change briefing',
    role: 'reviewer',
    question: 'What changes do we need for CMS prior authorization workflows?',
    context: 'Synthetic provider EHR planning scenario for prior authorization APIs.',
    summary: 'Great first run for grounded evidence → Brief → review workflow.'
  },
  {
    id: 'workflow-architecture',
    title: 'Provider workflow architecture planning',
    role: 'reviewer',
    question: 'How should a provider workflow handle documentation and status exchange for prior authorization?',
    context: 'Synthetic architecture review for a provider-facing utilization management workflow.',
    summary: 'Highlights reviewer interpretation, evidence quality, and workflow planning.'
  },
  {
    id: 'operator-oversight',
    title: 'Operator trust and oversight tour',
    role: 'auditor',
    question: 'What evidence-quality and approval signals should an enterprise evaluator inspect?',
    context: 'Synthetic enterprise evaluation walkthrough using the HealthForge trust layer.',
    summary: 'Use this before opening the compliance and evaluation dashboards.'
  },
  {
    id: 'admin-console',
    title: 'Admin console walkthrough',
    role: 'administrator',
    question: 'How should operators inspect deployment posture, access review, and workflow controls?',
    context: 'Synthetic platform operations walkthrough for a private demo environment.',
    summary: 'Best path for access review, identity directory, deployment guide, and packaging boundaries.'
  }
];

const docLinks = [
  {
    title: 'README overview',
    path: '/README.md',
    description: 'High-level product overview, quick start, demo prompts, and capability boundaries.'
  },
  {
    title: 'Phase 11 collaboration workspace',
    path: '/docs/40-phase11-team-workspaces-and-auth-foundation.md',
    description: 'Projects, saved views, reviewer queues, reusable configs, and enterprise identity foundation.'
  },
  {
    title: 'Phase 12 governed integrations',
    path: '/docs/41-phase12-governed-integrations-and-orchestration.md',
    description: 'Governed connector status, inbound intake, recovery tooling, and orchestration templates.'
  },
  {
    title: 'Phase 13 intelligence loops',
    path: '/docs/42-phase13-intelligence-loops-and-recommendations.md',
    description: 'Retrieval feedback, evidence gaps, similarity clusters, and bounded recommendations.'
  },
  {
    title: 'Client API surface',
    path: '/docs/23-client-api-surface.md',
    description: 'Supported local workflows, auth headers, and example requests.'
  },
  {
    title: 'Phase 9 evaluation and trust',
    path: '/docs/34-phase-9-evaluation-and-trust.md',
    description: 'Trust layer, policy/safety reporting, and runtime evaluation signals.'
  },
  {
    title: 'Private deployment operator guide',
    path: '/docs/31-private-deployment-operator-guide.md',
    description: 'Operator-safe deployment expectations and configuration boundaries.'
  }
];

const testingPaths = [
  {
    title: 'End-to-end reviewer demo',
    body: 'Run a sandbox Brief, inspect evidence, record review decisions, then switch to approver to complete the approval step.',
    steps: [
      'Choose the CMS prior authorization guided demo.',
      'Find cited evidence and create the Brief.',
      'Review at least one finding and record an accept decision.',
      'Switch to approver and record approval.'
    ]
  },
  {
    title: 'Admin and enterprise evaluator walkthrough',
    body: 'Open the admin console and inspect compliance, evaluation, safety, identity, and deployment posture as a coherent operator story.',
    steps: [
      'Switch to auditor and load compliance + evaluation dashboards.',
      'Switch to administrator and load access review + identity directory.',
      'Review deployment guidance and synthetic catalog boundaries.'
    ]
  },
  {
    title: 'Regression and trust verification',
    body: 'Use the Phase 9 scripts and docs to summarize the current gate decision and inspect the latest evaluation artifacts.',
    steps: [
      'Run mvn -q test from apps/platform-api.',
      'Run scripts/summarize-evaluation-state.sh from the repo root.',
      'Compare the latest report with the pinned baseline in evals/baselines/.'
    ]
  },
  {
    title: 'Packaging and product boundaries review',
    body: 'Use the showcase docs to explain demo/community/enterprise capability boundaries without overstating the current product scope.',
    steps: [
      'Review the deployable editions doc.',
      'Walk through capability boundaries in the README.',
      'Use the showcase narrative doc for external conversations.'
    ]
  },
  {
    title: 'Governed connector operations walkthrough',
    body: 'Inspect connector health, review delivery receipts, recover blocked deliveries, and walk an inbound case into a Brief.',
    steps: [
      'Open connector health from the admin console.',
      'Review recent receipts and blocked retry items.',
      'Create an inbound case from the intake view.',
      'Inspect orchestration templates for repeatable delivery paths.'
    ]
  },
  {
    title: 'Intelligence and recommendation walkthrough',
    body: 'Capture retrieval feedback, inspect advisory clusters, and review evidence-gap and tuning recommendations.',
    steps: [
      'Open a brief and record retrieval feedback on a finding.',
      'Open the intelligence panel from the admin console.',
      'Inspect retrieval, evidence-gap, and workflow-tuning recommendations.',
      'Use the clusters and persona recommendations to explain next steps.'
    ]
  }
];

const actorId = document.getElementById('actorId');
const actorOrg = document.getElementById('actorOrg');
const actorRole = document.getElementById('actorRole');
const sandboxMode = document.getElementById('sandboxMode');
const sessionMode = document.getElementById('sessionMode');
const sessionNote = document.getElementById('sessionNote');
const question = document.getElementById('question');
const context = document.getElementById('context');
const evidence = document.getElementById('evidence');
const briefs = document.getElementById('briefs');
const content = document.getElementById('content');
const enterprisePanel = document.getElementById('enterprisePanel');
const workspaceProjects = document.getElementById('workspaceProjects');
const workspaceQueues = document.getElementById('workspaceQueues');
const workspaceAssignments = document.getElementById('workspaceAssignments');
const workspaceSavedViews = document.getElementById('workspaceSavedViews');
const workspaceCollections = document.getElementById('workspaceCollections');
const workspaceConfigs = document.getElementById('workspaceConfigs');
const workspaceIdentity = document.getElementById('workspaceIdentity');
let currentBriefId = null;
let workspaceOverviewState = null;

function actorHeaders(json = true) {
  const headers = {
    'X-HealthForge-Actor': actorId.value.trim(),
    'X-HealthForge-Role': actorRole.value.trim(),
    'X-HealthForge-Organization': actorOrg.value.trim()
  };
  if (json) {
    headers['Content-Type'] = 'application/json';
  }
  return headers;
}

function currentRole() {
  return actorRole.value.trim().toLowerCase();
}

function currentOrg() {
  return actorOrg.value.trim();
}

function can(action) {
  const requiredRole = roleRequirements[action];
  if (!requiredRole) return true;
  if (currentRole() === 'administrator') return true;
  if (action === 'createBrief' || action === 'reviewFinding') {
    return currentRole() === 'reviewer' || currentRole() === 'approver';
  }
  if (action === 'approveBrief' || action === 'exportWorkItems') {
    return currentRole() === 'approver';
  }
  if (action === 'auditExport' || action === 'complianceDashboard' || action === 'enterprisePosture'
      || action === 'evaluationDashboard' || action === 'policySafetyReport') {
    return currentRole() === 'auditor';
  }
  return roleRank[currentRole()] >= roleRank[requiredRole];
}

function permissionText(action) {
  const requiredRole = roleRequirements[action];
  return requiredRole ? `Requires ${requiredRole} access.` : '';
}

function buttonHtml({ label, action, className = '', onClick = '' }) {
  if (!can(action)) {
    return `<button class="${className}" disabled title="${esc(permissionText(action))}">${esc(label)}</button>`;
  }
  return `<button class="${className}" ${onClick ? `onclick="${onClick}"` : ''}>${esc(label)}</button>`;
}

function setView(view) {
  document.querySelectorAll('.workspace-view').forEach((element) => {
    element.classList.toggle('active', element.id === `view-${view}`);
  });
  document.querySelectorAll('.nav-button').forEach((element) => {
    element.classList.toggle('active', element.dataset.view === view);
  });
}

function refreshSessionUi() {
  sessionMode.textContent = sandboxMode.checked ? 'Sandbox mode' : 'Evaluator mode';
  const role = currentRole();
  const notes = [
    `Current session: ${actorId.value.trim()} · ${role} · org ${currentOrg()}.`
  ];
  if (sandboxMode.checked) {
    notes.push('Sandbox mode keeps the prompts, demo paths, and wording focused on synthetic-safe evaluation.');
  }
  if (!can('approveBrief')) {
    notes.push('Approvals and work-item exports become available after switching to approver or administrator.');
  }
  if (!can('evaluationDashboard')) {
    notes.push('Evaluation and safety reporting appear for auditor and administrator walkthroughs.');
  }
  if (!can('identityDirectory')) {
    notes.push('Identity directory and access review stay gated to administrators.');
  }
  if (can('workspaceOverview')) {
    notes.push('Team Workspace groups briefs into projects, queues, saved views, and reusable workflow configurations.');
  }
  sessionNote.innerHTML = notes.map((note) => `<div>${esc(note)}</div>`).join('');

  document.getElementById('briefActions').innerHTML = [
    buttonHtml({ label: 'Refresh briefs', action: 'createBrief', onClick: 'loadBriefs()' }),
    buttonHtml({ label: 'Open evaluation dashboard', action: 'evaluationDashboard', className: 'secondary', onClick: 'openAdminPanel("evaluation")' }),
    buttonHtml({ label: 'Open compliance view', action: 'complianceDashboard', className: 'secondary', onClick: 'openAdminPanel("compliance")' })
  ].join('');

  document.getElementById('adminActions').innerHTML = [
    buttonHtml({ label: 'Compliance', action: 'complianceDashboard', onClick: 'openAdminPanel("compliance")' }),
    buttonHtml({ label: 'Evaluation', action: 'evaluationDashboard', onClick: 'openAdminPanel("evaluation")' }),
    buttonHtml({ label: 'Policy & safety', action: 'policySafetyReport', onClick: 'openAdminPanel("policySafety")' }),
    buttonHtml({ label: 'Enterprise posture', action: 'enterprisePosture', onClick: 'openAdminPanel("posture")' }),
    buttonHtml({ label: 'Access review', action: 'accessReview', onClick: 'openAdminPanel("accessReview")' }),
    buttonHtml({ label: 'Identity directory', action: 'identityDirectory', onClick: 'openAdminPanel("identity")' }),
    buttonHtml({ label: 'Deployment guide', action: 'deploymentGuide', onClick: 'openAdminPanel("deployment")' }),
    buttonHtml({ label: 'Connector health', action: 'integrationStatus', className: 'secondary', onClick: 'openAdminPanel("integrations")' }),
    buttonHtml({ label: 'Inbound intake', action: 'inboundCases', className: 'secondary', onClick: 'openAdminPanel("intake")' }),
    buttonHtml({ label: 'Templates', action: 'orchestrationTemplates', className: 'secondary', onClick: 'openAdminPanel("templates")' }),
    buttonHtml({ label: 'Intelligence', action: 'intelligenceOverview', className: 'secondary', onClick: 'openAdminPanel("intelligence")' }),
    '<button class="secondary" onclick="openAdminPanel(\'synthetic\')">Synthetic catalog</button>'
  ].join('');
}

function renderDemoCards() {
  document.getElementById('demoList').innerHTML = demoScenarios.map((scenario) => `
    <article class="demo-card">
      <h3>${esc(scenario.title)}</h3>
      <p>${esc(scenario.summary)}</p>
      <div class="meta-row">
        <span class="pill">${esc(scenario.role)} role</span>
      </div>
      <div class="button-row">
        <button onclick="applyDemoScenario('${esc(scenario.id)}')">Load scenario</button>
        <button class="secondary" onclick="applyDemoRole('${esc(scenario.role)}')">Use role</button>
      </div>
    </article>
  `).join('');
}

function renderTestingCards() {
  document.getElementById('testingCards').innerHTML = testingPaths.map((path) => `
    <article class="testing-card">
      <h3>${esc(path.title)}</h3>
      <p>${esc(path.body)}</p>
      <ol class="stack-list">
        ${path.steps.map((step) => `<li>${esc(step)}</li>`).join('')}
      </ol>
    </article>
  `).join('');
}

function renderDocCards() {
  const extraCards = [
    { title: 'End-to-end demo and onboarding', path: '/docs/36-end-to-end-demo-and-contributor-onboarding.md', description: 'Repeatable local demo path and contributor onboarding flow.' },
    { title: 'Deployable editions and capability boundaries', path: '/docs/37-deployable-editions-and-capability-boundaries.md', description: 'Clear demo/community/enterprise packaging boundaries.' },
    { title: 'Showcase architecture and solution narratives', path: '/docs/38-showcase-architecture-and-solution-narratives.md', description: 'Architecture storytelling, persona narratives, and testing walkthroughs.' },
    { title: 'Content and community pipeline', path: '/docs/39-content-and-community-pipeline.md', description: 'Repeatable technical content, demo, and outreach workflow tied to shipped milestones.' },
    { title: 'Article outline template', path: '/docs/templates/article-outline.md', description: 'Reusable structure for milestone-tied technical articles.' },
    { title: 'Demo / talk outline template', path: '/docs/templates/demo-talk-outline.md', description: 'Reusable walkthrough structure for demos, talks, and meetups.' }
  ];
  document.getElementById('docCards').innerHTML = [...docLinks, ...extraCards].map((doc) => `
    <article class="doc-card">
      <h3><a href="${esc(doc.path)}" target="_blank">${esc(doc.title)}</a></h3>
      <p>${esc(doc.description)}</p>
    </article>
  `).join('');
}

function projectSelectOptions(includeBlank = false) {
  const projects = workspaceOverviewState?.projects || [];
  const options = projects.map((project) => `<option value="${esc(project.project_id)}">${esc(project.name)}</option>`).join('');
  return includeBlank ? `<option value="">No project scope</option>${options}` : options;
}

function refreshWorkspaceSelectors() {
  document.getElementById('linkProjectSelect').innerHTML = projectSelectOptions(false);
  document.getElementById('savedViewProject').innerHTML = projectSelectOptions(true);
}

function projectCard(project) {
  return `
    <article class="demo-card">
      <h3>${esc(project.name)}</h3>
      <p>${esc(project.description)}</p>
      <div class="meta-row">
        <span class="pill">${esc(project.kind)}</span>
        <span class="pill">${esc(project.brief_count)} briefs</span>
        ${project.tags.map((tag) => `<span class="pill">${esc(tag)}</span>`).join('')}
      </div>
      <div class="helper">Owner: ${esc(project.owner_actor_id)}</div>
      ${project.brief_ids.length ? `<div class="helper">Linked briefs: ${project.brief_ids.map((briefId) => esc(briefId)).join(', ')}</div>` : '<div class="helper">No linked briefs yet.</div>'}
    </article>
  `;
}

function queueCard(queue) {
  return `
    <article class="testing-card">
      <h3>${esc(queue.queue_name)}</h3>
      <div class="metric-grid">
        ${renderMetricCard('assignments', queue.total_assignments)}
        ${renderMetricCard('draft', queue.draft_briefs)}
        ${renderMetricCard('in review', queue.in_review_briefs)}
        ${renderMetricCard('changes requested', queue.changes_requested_briefs)}
        ${renderMetricCard('approved', queue.approved_briefs)}
      </div>
    </article>
  `;
}

function assignmentCard(item) {
  return `
    <article class="demo-card">
      <h3>${esc(item.brief_question)}</h3>
      <p>${esc(item.handoff_summary)}</p>
      <div class="meta-row">
        <span class="pill">${esc(item.queue_name)}</span>
        <span class="pill">${esc(item.assignee_role)}</span>
        <span class="pill">${esc(item.brief_status)}</span>
      </div>
      <div class="helper">${esc(item.assignee_actor_id)} · ${esc(item.brief_id)}</div>
    </article>
  `;
}

function savedViewCard(view) {
  return `
    <article class="demo-card">
      <h3>${esc(view.name)}</h3>
      <p>${esc(view.summary)}</p>
      <div class="meta-row">
        <span class="pill">${esc(view.view_type)}</span>
        ${view.project_name ? `<span class="pill">${esc(view.project_name)}</span>` : '<span class="pill">org-scoped</span>'}
      </div>
      <pre>${esc(view.query_text)}</pre>
    </article>
  `;
}

function collectionCard(collection) {
  return `
    <article class="demo-card">
      <h3>${esc(collection.name)}</h3>
      <p>${esc(collection.summary)}</p>
      <div class="meta-row">
        <span class="pill">${esc(collection.project_name || 'shared')}</span>
        <span class="pill">${esc(collection.source_count)} sources</span>
      </div>
    </article>
  `;
}

function configCard(config) {
  return `
    <article class="doc-card">
      <h3>${esc(config.name)}</h3>
      <p>${esc(config.summary)}</p>
      <div class="meta-row">
        <span class="pill">${esc(config.config_type)}</span>
        <span class="pill">${esc(config.version_label)}</span>
        <span class="pill">${esc(config.status)}</span>
      </div>
      <div class="helper">Prompt: ${esc(config.prompt_profile)} · Retrieval: ${esc(config.retrieval_profile)} · Workflow: ${esc(config.workflow_profile)}</div>
    </article>
  `;
}

function identityCard(identity) {
  return `
    <article class="doc-card">
      <h3>${esc(identity.display_name)}</h3>
      <p>${esc(identity.provider_type)} · ${esc(identity.status)}</p>
      <div class="helper">Fallback: ${esc(identity.fallback_mode)}</div>
    </article>
  `;
}

function groupMappingCard(mapping) {
  return `
    <article class="doc-card">
      <h3>${esc(mapping.group_name)}</h3>
      <p>Maps to <b>${esc(mapping.actor_role)}</b></p>
      <div class="helper">${esc(mapping.scope_summary)}</div>
    </article>
  `;
}

function renderWorkspaceOverview(data) {
  workspaceOverviewState = data;
  refreshWorkspaceSelectors();
  workspaceProjects.innerHTML = data.projects.length ? data.projects.map(projectCard).join('') : '<div class="empty-state"><h3>No projects yet</h3><p>Create the first team workspace for this organization.</p></div>';
  workspaceQueues.innerHTML = data.queues.length ? data.queues.map(queueCard).join('') : '<div class="empty-state"><h3>No queue activity yet</h3><p>Create a brief or assignment to populate the reviewer queues.</p></div>';
  workspaceAssignments.innerHTML = data.assignments.length ? data.assignments.map(assignmentCard).join('') : '<div class="empty-state"><h3>No assignments yet</h3><p>Assignments will show who should act next on each brief.</p></div>';
  workspaceSavedViews.innerHTML = data.saved_views.length ? data.saved_views.map(savedViewCard).join('') : '<div class="empty-state"><h3>No saved views yet</h3><p>Save a repeated query to preserve analysis paths.</p></div>';
  workspaceCollections.innerHTML = data.evidence_collections.length ? data.evidence_collections.map(collectionCard).join('') : '<div class="empty-state"><h3>No evidence workspaces yet</h3><p>Link briefs to projects to build reusable evidence collections.</p></div>';
  workspaceConfigs.innerHTML = data.workflow_configurations.length ? data.workflow_configurations.map(configCard).join('') : '<div class="empty-state"><h3>No workflow configs yet</h3><p>Configuration profiles will appear here.</p></div>';
  workspaceIdentity.innerHTML = `
    <article class="doc-card">
      <h3>Auth foundation</h3>
      <p>${esc(data.auth_foundation.mode_summary)}</p>
      <div class="meta-row">
        <span class="pill">${esc(data.auth_foundation.active_mode)}</span>
        ${data.auth_foundation.supported_modes.map((mode) => `<span class="pill">${esc(mode)}</span>`).join('')}
      </div>
    </article>
    ${data.auth_foundation.identity_providers.map(identityCard).join('')}
    ${data.auth_foundation.group_role_mappings.map(groupMappingCard).join('')}
  `;
}

async function loadWorkspaceOverview(openView = false) {
  if (!can('workspaceOverview')) {
    workspaceProjects.innerHTML = `<div class="alert warning">${esc(permissionText('workspaceOverview'))}</div>`;
    return;
  }
  if (openView) {
    setView('workspace');
  }
  workspaceProjects.innerHTML = '<div class="alert warning">Loading team workspace…</div>';
  try {
    const data = await apiJson('/v1/workspace/overview', { method: 'GET', headers: actorHeaders(false) });
    renderWorkspaceOverview(data);
  } catch (error) {
    workspaceProjects.innerHTML = `<div class="alert error">${esc(error.message)}</div>`;
  }
}

async function createWorkspaceProject() {
  if (!can('workspaceProject')) {
    alert(permissionText('workspaceProject'));
    return;
  }
  const payload = {
    name: document.getElementById('workspaceProjectName').value.trim(),
    kind: document.getElementById('workspaceProjectKind').value.trim(),
    description: document.getElementById('workspaceProjectDescription').value.trim(),
    tags: document.getElementById('workspaceProjectTags').value.trim()
  };
  if (!payload.name || !payload.description) {
    alert('Project name and description are required.');
    return;
  }
  try {
    await apiJson('/v1/workspace/projects', { method: 'POST', body: JSON.stringify(payload) });
    document.getElementById('workspaceProjectName').value = '';
    document.getElementById('workspaceProjectDescription').value = '';
    document.getElementById('workspaceProjectTags').value = '';
    await loadWorkspaceOverview(true);
  } catch (error) {
    alert(error.message);
  }
}

async function linkCurrentBriefToProject() {
  if (!currentBriefId) {
    alert('Open a brief first so we know which brief to link.');
    return;
  }
  const projectId = document.getElementById('linkProjectSelect').value;
  if (!projectId) {
    alert('Choose a project first.');
    return;
  }
  try {
    await apiJson(`/v1/workspace/projects/${encodeURIComponent(projectId)}/briefs`, {
      method: 'POST',
      body: JSON.stringify({ brief_id: currentBriefId })
    });
    await loadWorkspaceOverview(true);
  } catch (error) {
    alert(error.message);
  }
}

async function createWorkspaceAssignment() {
  if (!currentBriefId) {
    alert('Open a brief first so we know which brief to assign.');
    return;
  }
  const payload = {
    brief_id: currentBriefId,
    assignee_actor_id: document.getElementById('assignmentActorId').value.trim(),
    assignee_role: document.getElementById('assignmentRole').value.trim(),
    queue_name: document.getElementById('assignmentQueue').value.trim(),
    handoff_summary: document.getElementById('assignmentSummary').value.trim()
  };
  if (!payload.assignee_actor_id || !payload.queue_name || !payload.handoff_summary) {
    alert('Assignment actor, queue, and handoff summary are required.');
    return;
  }
  try {
    await apiJson('/v1/workspace/assignments', { method: 'POST', body: JSON.stringify(payload) });
    document.getElementById('assignmentSummary').value = '';
    await loadWorkspaceOverview(true);
  } catch (error) {
    alert(error.message);
  }
}

async function createWorkspaceSavedView() {
  const payload = {
    project_id: document.getElementById('savedViewProject').value,
    view_type: document.getElementById('savedViewType').value.trim(),
    name: document.getElementById('savedViewName').value.trim(),
    query_text: document.getElementById('savedViewQuery').value.trim(),
    summary: document.getElementById('savedViewSummary').value.trim()
  };
  if (!payload.name || !payload.query_text || !payload.summary) {
    alert('Saved view name, query, and summary are required.');
    return;
  }
  try {
    await apiJson('/v1/workspace/views', { method: 'POST', body: JSON.stringify(payload) });
    document.getElementById('savedViewName').value = '';
    document.getElementById('savedViewQuery').value = '';
    document.getElementById('savedViewSummary').value = '';
    await loadWorkspaceOverview(true);
  } catch (error) {
    alert(error.message);
  }
}

function applyDemoRole(role) {
  actorRole.value = role;
  refreshSessionUi();
}

function applyDemoScenario(id) {
  const scenario = demoScenarios.find((item) => item.id === id);
  if (!scenario) return;
  actorRole.value = scenario.role;
  question.value = scenario.question;
  context.value = scenario.context;
  refreshSessionUi();
  setView('briefs');
  evidence.innerHTML = `<div class="alert success">Loaded the <b>${esc(scenario.title)}</b> scenario. You can now preview evidence or create a Brief.</div>`;
}

function resetPrompt() {
  question.value = demoScenarios[0].question;
  context.value = demoScenarios[0].context;
  evidence.innerHTML = '';
}

async function apiJson(url, options = {}) {
  const requestHeaders = { ...(options.headers || {}) };
  const includeJsonHeader = !requestHeaders['Content-Type'] || requestHeaders['Content-Type'] === 'application/json';
  const response = await fetch(url, {
    ...options,
    headers: { ...actorHeaders(includeJsonHeader), ...requestHeaders }
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.detail || 'Request failed');
  }
  return data;
}

function briefInput() {
  return {
    corpus_id: defaultCorpusId,
    corpus_version: defaultCorpusVersion,
    question: question.value.trim(),
    project_context: context.value.trim()
  };
}

async function previewEvidence() {
  if (!can('createBrief')) {
    evidence.innerHTML = `<div class="alert warning">${esc(permissionText('createBrief'))}</div>`;
    return;
  }
  const input = briefInput();
  if (!input.question || !input.project_context) {
    evidence.innerHTML = '<div class="alert error">Question and project context are required.</div>';
    return;
  }
  evidence.innerHTML = '<div class="alert warning">Searching the selected snapshot for cited evidence…</div>';
  try {
    const data = await fetch('/v1/answers', {
      method: 'POST',
      headers: actorHeaders(true),
      body: JSON.stringify(input)
    }).then(async (response) => {
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.detail || 'Could not search the selected snapshot.');
      return payload;
    });
    if (data.status !== 'grounded') {
      evidence.innerHTML = '<div class="alert warning"><b>Insufficient evidence.</b> No Brief will be created. Refine the question or stay with one of the guided demo prompts.</div>';
      return;
    }
    evidence.innerHTML = `
      <div class="alert success"><b>Grounded evidence found.</b> ${data.findings.length} cited passage(s) are available for a reviewable Brief.</div>
      <div class="metric-grid">
        ${data.findings.slice(0, 3).map((finding) => `
          <article class="metric-card">
            <strong>${esc(finding.citation.source_id)}</strong>
            <span>${esc(finding.citation.locator)}</span>
          </article>
        `).join('')}
      </div>
    `;
  } catch (error) {
    evidence.innerHTML = `<div class="alert error">${esc(error.message)}</div>`;
  }
}

async function createBrief() {
  if (!can('createBrief')) {
    evidence.innerHTML = `<div class="alert warning">${esc(permissionText('createBrief'))}</div>`;
    return;
  }
  evidence.innerHTML = '<div class="alert warning">Creating your reviewable Brief…</div>';
  try {
    const data = await apiJson(api, { method: 'POST', body: JSON.stringify(briefInput()) });
    await loadBriefs();
    setView('briefs');
    openBrief(data.brief_id);
  } catch (error) {
    evidence.innerHTML = `<div class="alert error">Could not create Brief: ${esc(error.message)}</div>`;
  }
}

async function loadBriefs() {
  try {
    const data = await apiJson(api, { method: 'GET', headers: actorHeaders(false) });
    briefs.innerHTML = data.length ? data.map((brief) => `
      <button class="brief-card" onclick="openBrief('${esc(brief.brief_id)}')">
        <div class="brief-status">${esc(brief.status)}</div>
        <h3>${esc(brief.question)}</h3>
        <p>Brief ID: ${esc(brief.brief_id)}</p>
      </button>
    `).join('') : '<div class="empty-state"><h3>No Briefs yet</h3><p>Create one from a guided demo or your own non-sensitive question.</p></div>';
  } catch (error) {
    briefs.innerHTML = `<div class="alert error">${esc(error.message)}</div>`;
  }
}

function reviewDecisionForm(briefId, findingId) {
  return `
    <div class="review-card">
      <h3>Record review decision</h3>
      <p class="helper">Authenticated actor: <b>${esc(actorId.value.trim())}</b> (${esc(currentRole())}) · org <b>${esc(currentOrg())}</b></p>
      <label>Decision
        <select id="decision-${esc(findingId)}">
          <option value="accept">accept</option>
          <option value="reject">reject</option>
          <option value="correct">correct</option>
          <option value="needs_information">needs_information</option>
        </select>
      </label>
      <label>Rationale
        <textarea id="rationale-${esc(findingId)}"></textarea>
      </label>
      <label>Corrected statement (optional)
        <textarea id="corrected-${esc(findingId)}"></textarea>
      </label>
      <div class="button-row">
        <button onclick="submitDecision('${esc(briefId)}','${esc(findingId)}')">Save decision</button>
      </div>
    </div>
  `;
}

async function recordRetrievalFeedback(briefId, findingId, sourceId) {
  const feedbackType = prompt('Feedback type: helpful, missing_evidence, ranking_issue, duplicate_result');
  if (!feedbackType) return;
  const note = prompt('Optional feedback note') || '';
  try {
    await apiJson('/v1/intelligence/retrieval-feedback', {
      method: 'POST',
      body: JSON.stringify({
        brief_id: briefId,
        finding_id: findingId,
        feedback_type: feedbackType,
        source_id: sourceId,
        note
      })
    });
    alert('Retrieval feedback recorded.');
  } catch (error) {
    alert(error.message);
  }
}

async function openBrief(id) {
  try {
    currentBriefId = id;
    const brief = await apiJson(`${api}/${id}`, { method: 'GET', headers: actorHeaders(false) });
    const findings = brief.findings.map((finding) => `
      <article class="finding" id="finding-${esc(finding.finding_id)}">
        <div class="brief-status">${esc(finding.kind)} · ${esc(finding.confidence)}</div>
        <p>${esc(finding.statement)}</p>
        <div class="citation">
          <b>${esc(finding.citation.source_id)} ${esc(finding.citation.source_version)}</b><br>
          ${esc(finding.citation.locator)}<br>
          ${esc(finding.citation.support)}
        </div>
        <div class="button-row">
          ${buttonHtml({ label: 'Review this finding', action: 'reviewFinding', onClick: `showForm('${esc(brief.brief_id)}','${esc(finding.finding_id)}')` })}
          ${buttonHtml({ label: 'Retrieval feedback', action: 'intelligenceOverview', className: 'secondary', onClick: `recordRetrievalFeedback('${esc(brief.brief_id)}','${esc(finding.finding_id)}','${esc(finding.citation.source_id)}')` })}
        </div>
        <div class="review-slot" id="review-slot-${esc(finding.finding_id)}"></div>
      </article>
    `).join('');

    content.innerHTML = `
      <div class="brief-header">
        <div>
          <span class="eyebrow">Brief</span>
          <h2>${esc(brief.input.question)}</h2>
          <p>${esc(brief.summary)}</p>
          <div class="meta-row">
            <span class="pill">${esc(brief.status)}</span>
            <span class="pill">org ${esc(currentOrg())}</span>
            <span class="pill">role ${esc(currentRole())}</span>
          </div>
        </div>
        <div class="button-row compact">
          ${buttonHtml({ label: 'Record approval', action: 'approveBrief', onClick: `approveBrief('${esc(brief.brief_id)}')` })}
          ${buttonHtml({ label: 'Work-item export', action: 'exportWorkItems', className: 'secondary', onClick: `downloadJsonWithHeaders('${api}/${esc(brief.brief_id)}/work-item-export','${esc(brief.brief_id)}-work-items.json')` })}
          ${buttonHtml({ label: 'Audit export', action: 'auditExport', className: 'secondary', onClick: `downloadJsonWithHeaders('${api}/${esc(brief.brief_id)}/audit-export','${esc(brief.brief_id)}-audit.json')` })}
        </div>
      </div>

      <section class="brief-section">
        <h3>Project context</h3>
        <p>${esc(brief.input.project_context)}</p>
        <div class="meta-row">
          <span class="pill">${esc(brief.input.corpus_id)}</span>
          <span class="pill">${esc(brief.input.corpus_version)}</span>
        </div>
      </section>

      <section class="brief-section">
        <h3>Sources</h3>
        <ul class="source-list">
          ${brief.sources.map((source) => `<li><a href="${esc(source.canonical_url)}" target="_blank">${esc(source.title)}</a> · ${esc(source.source_version)} · ${esc(source.source_type)}</li>`).join('')}
        </ul>
      </section>

      <section class="brief-section">
        <h3>Findings</h3>
        ${findings}
      </section>

      <section class="brief-section">
        <div class="metric-grid">
          <article class="metric-card">
            <strong>${brief.review_decisions.length}</strong>
            <span>review decisions</span>
          </article>
          <article class="metric-card">
            <strong>${brief.approvals.length}</strong>
            <span>approvals</span>
          </article>
          <article class="metric-card">
            <strong>${brief.audit_events.length}</strong>
            <span>audit events</span>
          </article>
        </div>
      </section>

      <section class="brief-section">
        <h3>Review history</h3>
        <ul class="note-list">
          ${(brief.review_decisions || []).map((decision) => `<li><b>${esc(decision.decision)}</b> by ${esc(decision.reviewer)} — ${esc(decision.rationale)}</li>`).join('') || '<li>No decisions yet.</li>'}
        </ul>
      </section>

      <section class="brief-section">
        <h3>Approvals</h3>
        <ul class="note-list">
          ${(brief.approvals || []).map((approval) => `<li><b>${esc(approval.approver)}</b> (${esc(approval.approver_role)}) — ${esc(approval.rationale)}</li>`).join('') || '<li>No approvals yet.</li>'}
        </ul>
      </section>

      <section class="brief-section">
        <h3>Audit trail</h3>
        <ul class="audit-list">
          ${(brief.audit_events || []).map((event) => `<li><b>${esc(event.event_type)}</b> · ${esc(event.actor_id)} (${esc(event.actor_role)})<br>${esc(event.summary)}${event.details ? `<br><span class="helper">${esc(event.details)}</span>` : ''}</li>`).join('') || '<li>No audit events yet.</li>'}
        </ul>
      </section>
    `;
    setView('briefs');
  } catch (error) {
    content.innerHTML = `<div class="alert error">${esc(error.message)}</div>`;
  }
}

function showForm(briefId, findingId) {
  if (!can('reviewFinding')) {
    alert(permissionText('reviewFinding'));
    return;
  }
  document.querySelectorAll('.review-slot').forEach((element) => {
    element.innerHTML = '';
  });
  document.querySelectorAll('.finding').forEach((element) => {
    element.classList.remove('active');
  });
  const slot = document.getElementById(`review-slot-${findingId}`);
  const finding = document.getElementById(`finding-${findingId}`);
  if (!slot || !finding) return;
  slot.innerHTML = reviewDecisionForm(briefId, findingId);
  finding.classList.add('active');
  slot.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

async function submitDecision(briefId, findingId) {
  if (!can('reviewFinding')) {
    alert(permissionText('reviewFinding'));
    return;
  }
  const decision = document.getElementById(`decision-${findingId}`).value;
  const rationale = document.getElementById(`rationale-${findingId}`).value;
  const corrected = document.getElementById(`corrected-${findingId}`).value;
  const body = {
    finding_id: findingId,
    reviewer: actorId.value.trim(),
    decision,
    rationale,
    corrected_statement: corrected || null
  };
  try {
    await apiJson(`${api}/${briefId}/review-decisions`, { method: 'POST', body: JSON.stringify(body) });
    await loadBriefs();
    await openBrief(briefId);
  } catch (error) {
    alert(error.message);
  }
}

async function approveBrief(briefId) {
  if (!can('approveBrief')) {
    alert(permissionText('approveBrief'));
    return;
  }
  const rationale = prompt('Approval rationale');
  if (!rationale) return;
  try {
    await apiJson(`${api}/${briefId}/approvals`, { method: 'POST', body: JSON.stringify({ rationale }) });
    await loadBriefs();
    await openBrief(briefId);
  } catch (error) {
    alert(error.message);
  }
}

async function downloadJsonWithHeaders(url, fileName) {
  try {
    const response = await fetch(url, { headers: actorHeaders(false) });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.detail || 'Download failed');
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
  } catch (error) {
    alert(error.message);
  }
}

function renderMetricCard(label, value, note) {
  return `<article class="metric-card"><strong>${esc(value)}</strong><span>${esc(label)}</span>${note ? `<span>${esc(note)}</span>` : ''}</article>`;
}

async function openAdminPanel(panel) {
  setView('admin');
  enterprisePanel.innerHTML = '<div class="alert warning">Loading operator view…</div>';
  try {
    if (panel === 'compliance') {
      if (!can('complianceDashboard')) throw new Error(permissionText('complianceDashboard'));
      const dashboard = await apiJson('/v1/compliance/dashboard', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Compliance dashboard</h3>
          <p class="helper">${esc(dashboard.summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('briefs', dashboard.brief_metrics.total_briefs, `approved ${dashboard.brief_metrics.approved_briefs}`)}
            ${renderMetricCard('validation runs', dashboard.validation_metrics.total_runs, `valid ${dashboard.validation_metrics.valid_runs}`)}
            ${renderMetricCard('tracked exports', dashboard.export_metrics.total_tracked_exports, `blocked ${dashboard.export_metrics.blocked_writebacks}`)}
            ${renderMetricCard('answers', dashboard.evaluation_metrics.total_answers, `gate ${dashboard.evaluation_metrics.latest_quality_gate_decision}`)}
          </div>
        </article>
        <article class="admin-card">
          <h4>Controls</h4>
          <ul class="stack-list">${dashboard.controls.map((control) => `<li>${esc(control)}</li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Recent audit events</h4>
          <ul class="stack-list">${(dashboard.recent_audit_events || []).map((event) => `<li><b>${esc(event.event_type)}</b> · ${esc(event.actor_id)} · ${esc(event.brief_id)}</li>`).join('') || '<li>No audit events yet.</li>'}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'evaluation') {
      if (!can('evaluationDashboard')) throw new Error(permissionText('evaluationDashboard'));
      const dashboard = await apiJson('/v1/evaluation/dashboard', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Evaluation dashboard</h3>
          <p class="helper">${esc(dashboard.summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('retrieval recall', dashboard.quality_gate.retrieval_recall.toFixed(2), dashboard.quality_gate.decision)}
            ${renderMetricCard('citation coverage', dashboard.quality_gate.citation_coverage_rate.toFixed(2))}
            ${renderMetricCard('unsupported pass rate', dashboard.quality_gate.unsupported_answer_pass_rate.toFixed(2))}
            ${renderMetricCard('source age (days)', dashboard.source_health.average_source_age_days.toFixed(1))}
            ${renderMetricCard('review disagreements', dashboard.review_quality.disagreement_findings)}
            ${renderMetricCard('governed deliveries', dashboard.workflow_quality.governed_deliveries)}
          </div>
        </article>
        <article class="admin-card">
          <h4>Regression signals</h4>
          <ul class="stack-list">${dashboard.quality_gate.regression_signals.map((signal) => `<li>${esc(signal)}</li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Highlighted failures</h4>
          <ul class="stack-list">${(dashboard.quality_gate.highlighted_failures || []).map((failure) => `<li><b>${esc(failure.case_id)}</b> · ${esc(failure.category)} · ${esc(failure.severity)} · answer ${esc(failure.answer_status)}</li>`).join('') || '<li>No highlighted failures.</li>'}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'policySafety') {
      if (!can('policySafetyReport')) throw new Error(permissionText('policySafetyReport'));
      const report = await apiJson('/v1/evaluation/policy-safety-report', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Policy and safety report</h3>
          <p class="helper">${esc(report.summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('insufficient evidence', report.unsupported_output_summary.insufficient_evidence_answers, `${(report.unsupported_output_summary.insufficient_evidence_rate * 100).toFixed(0)}% rate`)}
            ${renderMetricCard('unsupported triggers', report.unsupported_output_summary.unsupported_triggered_answers)}
            ${renderMetricCard('approved briefs', report.approval_policy_summary.approved_briefs)}
            ${renderMetricCard('blocked tracker writes', report.integration_policy_summary.blocked_tracker_writebacks)}
          </div>
        </article>
        <article class="admin-card">
          <h4>Enabled controls</h4>
          <ul class="stack-list">${report.enabled_controls.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Known limitations</h4>
          <ul class="stack-list">${report.known_limitations.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'posture') {
      if (!can('enterprisePosture')) throw new Error(permissionText('enterprisePosture'));
      const posture = await apiJson('/v1/enterprise/posture', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Enterprise posture</h3>
          <div class="metric-grid">
            ${renderMetricCard('deployment mode', posture.deployment_mode)}
            ${renderMetricCard('identity mode', posture.identity_mode)}
            ${renderMetricCard('supported roles', posture.supported_roles.length)}
            ${renderMetricCard('approval required for exports', posture.audit_policy.approval_required_for_exports ? 'yes' : 'no')}
          </div>
        </article>
        <article class="admin-card">
          <h4>Active controls</h4>
          <ul class="stack-list">${posture.active_controls.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Current boundaries</h4>
          <ul class="stack-list">${posture.current_boundaries.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'accessReview') {
      if (!can('accessReview')) throw new Error(permissionText('accessReview'));
      const report = await apiJson('/v1/admin/access-review', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Access review</h3>
          <p class="helper">${esc(report.summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('users', report.access_summary.total_users)}
            ${renderMetricCard('memberships', report.access_summary.total_memberships)}
            ${renderMetricCard('reviewers', report.access_summary.reviewer_assignments)}
            ${renderMetricCard('administrators', report.access_summary.administrator_assignments)}
          </div>
        </article>
        <article class="admin-card">
          <h4>Assignments</h4>
          <ul class="stack-list">${report.access_assignments.map((assignment) => `<li><b>${esc(assignment.actor_user_id)}</b> · ${esc(assignment.actor_role)} · granted by ${esc(assignment.granted_by)}</li>`).join('')}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'identity') {
      if (!can('identityDirectory')) throw new Error(permissionText('identityDirectory'));
      const directory = await apiJson(`/v1/admin/identity-directory?organization_id=${encodeURIComponent(currentOrg())}`, { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Identity directory</h3>
          <div class="metric-grid">
            ${renderMetricCard('organizations', directory.organizations.length)}
            ${renderMetricCard('users', directory.users.length)}
            ${renderMetricCard('memberships', directory.memberships.length)}
            ${renderMetricCard('role assignments', directory.role_assignments.length)}
          </div>
        </article>
        <article class="admin-card">
          <h4>Role assignments</h4>
          <ul class="stack-list">${directory.role_assignments.map((assignment) => `<li><b>${esc(assignment.actor_user_id)}</b> · ${esc(assignment.actor_role)} · ${esc(assignment.organization_id)}</li>`).join('') || '<li>No role assignments recorded yet.</li>'}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'deployment') {
      if (!can('deploymentGuide')) throw new Error(permissionText('deploymentGuide'));
      const guide = await apiJson('/v1/enterprise/deployment-promotion-guide', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Deployment promotion guide</h3>
          <p class="helper">Deployment mode: ${esc(guide.deployment_mode)}</p>
          <div class="doc-grid">
            ${guide.environments.map((environment) => `
              <article class="metric-card">
                <strong>${esc(environment.name)}</strong>
                <span>${esc(environment.purpose)}</span>
                <span>Checks: ${esc(environment.operator_checks.join(' · '))}</span>
              </article>
            `).join('')}
          </div>
        </article>
        <article class="admin-card">
          <h4>Promotion steps</h4>
          <ol class="stack-list">${guide.promotion_steps.map((step) => `<li>${esc(step)}</li>`).join('')}</ol>
          <h4>Rollback steps</h4>
          <ol class="stack-list">${guide.rollback_steps.map((step) => `<li>${esc(step)}</li>`).join('')}</ol>
        </article>
      `;
      return;
    }

    if (panel === 'synthetic') {
      const catalog = await apiJson('/v1/fhir-synthetic/catalog', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Synthetic FHIR catalog</h3>
          <p class="helper">Demo-safe validation scenarios that stay separate from enterprise evaluation and production claims.</p>
          <div class="doc-grid">
            ${catalog.scenarios.map((scenario) => `
              <article class="metric-card">
                <strong>${esc(scenario.title)}</strong>
                <span>${esc(scenario.description)}</span>
                <span>${esc(scenario.expected_status)} · ${esc(scenario.package_id)} ${esc(scenario.package_version)}</span>
                <div class="button-row"><button onclick="generateSynthetic('${esc(scenario.scenario_id)}')">Generate payload</button></div>
              </article>
            `).join('')}
          </div>
        </article>
      `;
      return;
    }

    if (panel === 'integrations') {
      if (!can('integrationStatus')) throw new Error(permissionText('integrationStatus'));
      const status = await apiJson('/v1/integrations/status', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Connector health and receipts</h3>
          <p class="helper">Operators can distinguish simulated vs live-capable connectors, inspect receipts, and see retry pressure.</p>
          <div class="doc-grid">
            ${status.connectors.map((connector) => `
              <article class="metric-card">
                <strong>${esc(connector.connector_type)}</strong>
                <span>${esc(connector.execution_mode)} · ${connector.enabled ? 'enabled' : 'disabled'}</span>
                <span>success ${esc(connector.success_count)} · blocked ${esc(connector.blocked_count)} · retries ${esc(connector.retry_count)}</span>
                <span>${esc(connector.operator_summary)}</span>
              </article>
            `).join('')}
          </div>
        </article>
        <article class="admin-card">
          <h4>Recent receipts</h4>
          <ul class="stack-list">${status.recent_receipts.map((receipt) => `<li><b>${esc(receipt.connector_type)}</b> · ${esc(receipt.status)} · ${esc(receipt.source_id)}${receipt.external_reference ? ` · ${esc(receipt.external_reference)}` : ''}</li>`).join('') || '<li>No receipts yet.</li>'}</ul>
        </article>
        <article class="admin-card">
          <h4>Retry queue</h4>
          <ul class="stack-list">${status.retry_queue.map((item) => `<li><b>${esc(item.connector_type)}</b> · ${esc(item.current_status)} · ${esc(item.source_id)}<br><span class="helper">${esc(item.retry_hint)}</span></li>`).join('') || '<li>No blocked or retryable items right now.</li>'}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'intake') {
      if (!can('inboundCases')) throw new Error(permissionText('inboundCases'));
      const cases = await apiJson('/v1/intake/cases', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Inbound case intake</h3>
          <p class="helper">This shows the reverse direction: inbound requests can become structured HealthForge review workflows.</p>
          <div class="button-row">
            <button onclick="createSampleInboundCase()">Create sample intake case</button>
          </div>
        </article>
        <article class="admin-card">
          <ul class="stack-list">${cases.map((item) => `<li><b>${esc(item.title)}</b> · ${esc(item.source_system)} · ${esc(item.intake_status)}${item.linked_brief_id ? ` · linked ${esc(item.linked_brief_id)}` : ''}</li>`).join('') || '<li>No inbound cases yet.</li>'}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'templates') {
      if (!can('orchestrationTemplates')) throw new Error(permissionText('orchestrationTemplates'));
      const templates = await apiJson('/v1/orchestration/templates', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Orchestration templates</h3>
          <p class="helper">Repeatable program paths for prior authorization and interoperability workflows.</p>
          <div class="doc-grid">
            ${templates.map((template) => `
              <article class="doc-card">
                <h3>${esc(template.name)}</h3>
                <p>${esc(template.summary)}</p>
                <div class="meta-row">
                  <span class="pill">${esc(template.template_type)}</span>
                  <span class="pill">${esc(template.default_queue)}</span>
                  <span class="pill">${esc(template.default_target_system)}</span>
                </div>
                <ul class="stack-list">${template.guardrails.map((guardrail) => `<li>${esc(guardrail)}</li>`).join('')}</ul>
              </article>
            `).join('')}
          </div>
        </article>
      `;
      return;
    }

    if (panel === 'intelligence') {
      if (!can('intelligenceOverview')) throw new Error(permissionText('intelligenceOverview'));
      const intel = await apiJson('/v1/intelligence/overview', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Advisory intelligence overview</h3>
          <p class="helper">${esc(intel.summary)}</p>
          <ul class="stack-list">${intel.guardrails.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Retrieval improvements</h4>
          <div class="doc-grid">
            ${intel.retrieval_improvements.map((item) => `
              <article class="doc-card">
                <h3>${esc(item.title)}</h3>
                <p>${esc(item.rationale)}</p>
                <div class="meta-row"><span class="pill">${esc(item.priority)}</span></div>
                <ul class="stack-list">${item.evidence.map((evidence) => `<li>${esc(evidence)}</li>`).join('')}</ul>
              </article>
            `).join('')}
          </div>
        </article>
        <article class="admin-card">
          <h4>Evidence gaps</h4>
          <div class="doc-grid">
            ${intel.evidence_gaps.map((gap) => `
              <article class="doc-card">
                <h3>${esc(gap.summary)}</h3>
                <p>Severity: ${esc(gap.severity)}</p>
                <ul class="stack-list">${gap.suggested_sources.map((source) => `<li>${esc(source)}</li>`).join('')}</ul>
              </article>
            `).join('')}
          </div>
        </article>
        <article class="admin-card">
          <h4>Similarity clusters</h4>
          <ul class="stack-list">${intel.similarity_clusters.map((cluster) => `<li><b>${esc(cluster.theme)}</b> · ${esc(cluster.artifact_count)} related artifacts · ${cluster.brief_ids.map((briefId) => esc(briefId)).join(', ')}</li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Persona recommendations</h4>
          <ul class="stack-list">${intel.persona_recommendations.map((item) => `<li><b>${esc(item.persona)}</b> · ${esc(item.next_action)}<br><span class="helper">${esc(item.explanation)}</span></li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Workflow tuning recommendations</h4>
          <ul class="stack-list">${intel.workflow_tuning_recommendations.map((item) => `<li><b>${esc(item.title)}</b> · ${esc(item.priority)}<br><span class="helper">${esc(item.summary)}</span></li>`).join('')}</ul>
        </article>
      `;
      return;
    }
  } catch (error) {
    enterprisePanel.innerHTML = `<div class="alert error">${esc(error.message)}</div>`;
  }
}

async function createSampleInboundCase() {
  try {
    await apiJson('/v1/intake/cases', {
      method: 'POST',
      body: JSON.stringify({
        source_system: 'jira',
        external_case_id: `DEMO-${Date.now()}`,
        title: 'Inbound prior auth workflow request',
        summary: 'Create a grounded workflow brief from a synthetic inbound interoperability planning request.',
        requested_role: 'reviewer',
        requested_assignee: actorId.value.trim(),
        source_locator: 'DEMO/healthforge',
        create_brief: true,
        corpus_id: defaultCorpusId,
        corpus_version: defaultCorpusVersion,
        brief_question: 'How should a provider workflow handle documentation and status exchange for prior authorization?',
        project_context: 'Synthetic inbound case routed into the HealthForge team workspace.'
      })
    });
    await openAdminPanel('intake');
  } catch (error) {
    alert(error.message);
  }
}

async function generateSynthetic(scenarioId) {
  enterprisePanel.innerHTML = '<div class="alert warning">Generating synthetic FHIR payload…</div>';
  try {
    const payload = await apiJson('/v1/fhir-synthetic/generate', {
      method: 'POST',
      body: JSON.stringify({ scenario_id: scenarioId })
    });
    enterprisePanel.innerHTML = `
      <article class="admin-card">
        <h3>${esc(payload.title)}</h3>
        <p class="helper">${esc(payload.description)}</p>
        <div class="meta-row">
          <span class="pill">${esc(payload.expected_validation_status)}</span>
          <span class="pill">${esc(payload.validation_recommendation.profile_url)}</span>
        </div>
        <pre>${esc(JSON.stringify(payload.validation_request, null, 2))}</pre>
      </article>
    `;
  } catch (error) {
    enterprisePanel.innerHTML = `<div class="alert error">${esc(error.message)}</div>`;
  }
}

document.querySelectorAll('.nav-button').forEach((button) => {
  button.addEventListener('click', () => setView(button.dataset.view));
});

document.getElementById('loadBriefsBtn').addEventListener('click', loadBriefs);
document.getElementById('openWorkspaceBtn').addEventListener('click', () => loadWorkspaceOverview(true));
document.getElementById('openAdminBtn').addEventListener('click', () => openAdminPanel('evaluation'));
document.getElementById('previewEvidenceBtn').addEventListener('click', previewEvidence);
document.getElementById('createBriefBtn').addEventListener('click', createBrief);
document.getElementById('resetPromptBtn').addEventListener('click', resetPrompt);
document.getElementById('refreshWorkspaceBtn').addEventListener('click', () => loadWorkspaceOverview(true));
document.getElementById('createProjectBtn').addEventListener('click', createWorkspaceProject);
document.getElementById('linkBriefBtn').addEventListener('click', linkCurrentBriefToProject);
document.getElementById('createAssignmentBtn').addEventListener('click', createWorkspaceAssignment);
document.getElementById('createSavedViewBtn').addEventListener('click', createWorkspaceSavedView);
actorId.addEventListener('change', refreshSessionUi);
actorOrg.addEventListener('change', refreshSessionUi);
actorRole.addEventListener('change', refreshSessionUi);
sandboxMode.addEventListener('change', refreshSessionUi);

document.getElementById('corpusIdLabel').textContent = defaultCorpusId;
document.getElementById('corpusVersionLabel').textContent = defaultCorpusVersion;

renderDemoCards();
renderTestingCards();
renderDocCards();
refreshSessionUi();
resetPrompt();
loadBriefs();
loadWorkspaceOverview();
