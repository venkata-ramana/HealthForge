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
  workspaceQuestionPack: 'reviewer',
  workspaceResearchNotebook: 'reviewer',
  workspaceReviewEscalation: 'reviewer',
  workspaceDiscoverySearch: 'reviewer',
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
  deploymentGuide: 'administrator',
  operationsConfiguration: 'administrator',
  operationsObservability: 'auditor',
  operationsContinuity: 'administrator',
  operationsUsage: 'auditor',
  operationsAttestations: 'administrator',
  pilotReadiness: 'auditor',
  solutionPacks: 'reviewer',
  stakeholderReport: 'auditor',
  futureRoadmap: 'auditor',
  pilotSuccess: 'reviewer',
  implementationBundle: 'approver',
  syntheticLabs: 'reviewer',
  tenantAdministration: 'administrator',
  regulatedReadiness: 'auditor'
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
    title: 'Phase 22 analyst research workspace',
    path: '/docs/53-phase22-analyst-research-workspace.md',
    description: 'Question packs, precedent comparison, notebooks, topic discovery, and reviewer operations cues.'
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
    title: 'Phase 14 private deployment operations',
    path: '/docs/43-phase14-private-deployment-and-enterprise-operations.md',
    description: 'Configuration boundaries, observability, continuity, quotas, and operator sign-off workflows.'
  },
  {
    title: 'Phase 15 pilot readiness and solution packs',
    path: '/docs/44-phase15-pilot-readiness-and-solution-packs.md',
    description: 'Pilot readiness, audience-tailored solution packs, stakeholder reporting, future control roadmap, and success-plan workflows.'
  },
  {
    title: 'Phase 16 implementation acceleration',
    path: '/docs/45-phase16-implementation-acceleration.md',
    description: 'Implementation bundles, richer starter code, test plans, reference patterns, engineering handoff packs, and change-impact guidance.'
  },
  {
    title: 'Phase 17 synthetic interoperability labs',
    path: '/docs/46-phase17-synthetic-interoperability-labs.md',
    description: 'Scenario-based testing labs, richer synthetic rehearsals, replay/diff tooling, assertions, and coverage views.'
  },
  {
    title: 'Phase 18 developer workflows',
    path: '/docs/47-phase18-developer-workflows.md',
    description: 'Builder-facing API, CLI, SDK, VS Code companion, and repo-aware engineering guidance.'
  },
  {
    title: 'Phase 19 multi-tenant foundations',
    path: '/docs/48-phase19-multi-tenant-foundations.md',
    description: 'Tenant administration, provisioning workflows, isolation boundaries, customer analytics, and hosted packaging artifacts.'
  },
  {
    title: 'Phase 20 regulated deployment readiness',
    path: '/docs/49-phase20-regulated-deployment-readiness.md',
    description: 'Security posture, compliance evidence packaging, deployment architecture packs, release governance, and resilience readiness.'
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
  },
  {
    title: 'Private deployment operations walkthrough',
    body: 'Inspect the new Phase 14 operator surfaces for configuration policy, observability, continuity, usage, and attestation history.',
    steps: [
      'Switch to administrator and open Config policy, Continuity, and Attestations.',
      'Switch to auditor and inspect Observability and Usage.',
      'Record one sample operator sign-off to show governance history.',
      'Use the Phase 14 doc to explain how private operations are being hardened.'
    ]
  },
  {
    title: 'Pilot readiness and buyer-pack walkthrough',
    body: 'Use the new Phase 15 surfaces to explain pilot fit, audience narratives, reporting, and success tracking.',
    steps: [
      'Switch to auditor and open Pilot readiness plus Stakeholder report.',
      'Switch to reviewer and open Solution packs plus Pilot success.',
      'Switch back to auditor and open Future roadmap.',
      'Use the Phase 15 doc to explain private pilot readiness without overstating production maturity.'
    ]
  },
  {
    title: 'Implementation acceleration walkthrough',
    body: 'Turn an approved Brief into implementation starter code, test planning, handoff artifacts, and change-impact guidance.',
    steps: [
      'Create and approve a Brief from the reviewer + approver flow.',
      'Open the approved Brief and load the Implementation pack.',
      'Inspect starter artifacts, acceptance criteria, and reference patterns.',
      'Export the implementation bundle JSON for downstream engineering handoff.'
    ]
  },
  {
    title: 'Synthetic lab walkthrough',
    body: 'Run synthetic workflow rehearsals, compare a happy path to a negative path, and inspect coverage gaps.',
    steps: [
      'Open Synthetic labs from the admin console.',
      'Run Provider PAS submission baseline.',
      'Compare it with Negative bundle structure.',
      'Use coverage and validation gaps to explain what is and is not modeled yet.'
    ]
  },
  {
    title: 'Tenant administration walkthrough',
    body: 'Explain multi-tenant product structure, delegated administration, provisioning, and hosted packaging without overstating production SaaS maturity.',
    steps: [
      'Switch to administrator and open Tenant admin from the admin console.',
      'Review customer tenants, isolation boundaries, and delegated roles.',
      'Create one provisioning request for a private customer space.',
      'Use tenant analytics and packaging views to explain hosted product direction.'
    ]
  },
  {
    title: 'Regulated deployment readiness walkthrough',
    body: 'Use the regulated-readiness pack to explain security posture, compliance evidence, deployment controls, release governance, and resilience direction.',
    steps: [
      'Switch to auditor or administrator and open Regulated readiness from the admin console.',
      'Review dependency evidence, supply-chain controls, and compliance mappings.',
      'Walk through deployment architecture, release controls, and evidence retention narratives.',
      'Use the resilience pack to distinguish current continuity posture from future regulated DR maturity.'
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
const workspaceResearchPacks = document.getElementById('workspaceResearchPacks');
const workspaceSourceOperations = document.getElementById('workspaceSourceOperations');
const workspaceConfigs = document.getElementById('workspaceConfigs');
const workspaceIdentity = document.getElementById('workspaceIdentity');
const workspaceQuestionPacks = document.getElementById('workspaceQuestionPacks');
const workspaceScenarioTemplates = document.getElementById('workspaceScenarioTemplates');
const workspacePersonaPresets = document.getElementById('workspacePersonaPresets');
const workspacePrecedentComparisons = document.getElementById('workspacePrecedentComparisons');
const workspaceDecisionPatterns = document.getElementById('workspaceDecisionPatterns');
const workspaceThemeClusters = document.getElementById('workspaceThemeClusters');
const workspaceTopics = document.getElementById('workspaceTopics');
const workspaceSearchResults = document.getElementById('workspaceSearchResults');
const workspaceResearchNotebooks = document.getElementById('workspaceResearchNotebooks');
const workspaceReviewerOperations = document.getElementById('workspaceReviewerOperations');
let currentBriefId = null;
let workspaceOverviewState = null;
let sourceOperationsState = null;

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
    buttonHtml({ label: 'Config policy', action: 'operationsConfiguration', className: 'secondary', onClick: 'openAdminPanel("operationsConfiguration")' }),
    buttonHtml({ label: 'Observability', action: 'operationsObservability', className: 'secondary', onClick: 'openAdminPanel("operationsObservability")' }),
    buttonHtml({ label: 'Continuity', action: 'operationsContinuity', className: 'secondary', onClick: 'openAdminPanel("operationsContinuity")' }),
    buttonHtml({ label: 'Usage', action: 'operationsUsage', className: 'secondary', onClick: 'openAdminPanel("operationsUsage")' }),
    buttonHtml({ label: 'Attestations', action: 'operationsAttestations', className: 'secondary', onClick: 'openAdminPanel("operationsAttestations")' }),
    buttonHtml({ label: 'Pilot readiness', action: 'pilotReadiness', className: 'secondary', onClick: 'openAdminPanel("pilotReadiness")' }),
    buttonHtml({ label: 'Solution packs', action: 'solutionPacks', className: 'secondary', onClick: 'openAdminPanel("solutionPacks")' }),
    buttonHtml({ label: 'Stakeholder report', action: 'stakeholderReport', className: 'secondary', onClick: 'openAdminPanel("stakeholderReport")' }),
    buttonHtml({ label: 'Future roadmap', action: 'futureRoadmap', className: 'secondary', onClick: 'openAdminPanel("futureRoadmap")' }),
    buttonHtml({ label: 'Pilot success', action: 'pilotSuccess', className: 'secondary', onClick: 'openAdminPanel("pilotSuccess")' }),
    buttonHtml({ label: 'Synthetic labs', action: 'syntheticLabs', className: 'secondary', onClick: 'openAdminPanel("syntheticLabs")' }),
    buttonHtml({ label: 'Tenant admin', action: 'tenantAdministration', className: 'secondary', onClick: 'openAdminPanel("tenantAdministration")' }),
    buttonHtml({ label: 'Regulated readiness', action: 'regulatedReadiness', className: 'secondary', onClick: 'openAdminPanel("regulatedReadiness")' }),
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
  document.getElementById('researchPackProject').innerHTML = projectSelectOptions(true);
  document.getElementById('questionPackProject').innerHTML = projectSelectOptions(true);
  document.getElementById('researchNotebookProject').innerHTML = projectSelectOptions(true);
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

function researchPackCard(pack) {
  return `
    <article class="demo-card">
      <h3>${esc(pack.name)}</h3>
      <p>${esc(pack.summary)}</p>
      <div class="meta-row">
        <span class="pill">${esc(pack.project_name || 'org-scoped')}</span>
        <span class="pill">${esc(pack.question_count)} questions</span>
        ${pack.next_review_at ? `<span class="pill">review ${esc(pack.next_review_at.slice(0, 10))}</span>` : ''}
      </div>
      <ul class="stack-list">${(pack.recurring_questions || []).map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
    </article>
  `;
}

function questionPackCard(pack) {
  return `
    <article class="demo-card">
      <h3>${esc(pack.name)}</h3>
      <p>${esc(pack.summary)}</p>
      <div class="meta-row">
        <span class="pill">${esc(pack.persona)}</span>
        <span class="pill">${esc(pack.template_kind)}</span>
        ${pack.project_name ? `<span class="pill">${esc(pack.project_name)}</span>` : '<span class="pill">org-scoped</span>'}
      </div>
      <div class="helper">Starter: ${esc(pack.starter_question)}</div>
      <ul class="stack-list">${(pack.question_prompts || []).map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
    </article>
  `;
}

function scenarioTemplateCard(template) {
  return `
    <article class="doc-card">
      <h3>${esc(template.title)}</h3>
      <p>${esc(template.summary)}</p>
      <div class="meta-row">
        <span class="pill">${esc(template.persona)}</span>
        <span class="pill">${esc(template.workflow_stage)}</span>
      </div>
      <ul class="stack-list">${(template.starting_points || []).map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
    </article>
  `;
}

function personaPresetCard(preset) {
  return `
    <article class="doc-card">
      <h3>${esc(preset.persona)}</h3>
      <p>${esc(preset.summary)}</p>
      <div class="meta-row">
        <span class="pill">${esc(preset.recommended_role)}</span>
        <span class="pill">${esc(preset.starting_view)}</span>
      </div>
      <ul class="stack-list">${(preset.focus_areas || []).map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
    </article>
  `;
}

function precedentComparisonCard(item) {
  return `
    <article class="demo-card">
      <h3>${esc(item.overlap_theme)}</h3>
      <p>${esc(item.advisory_summary)}</p>
      <div class="helper"><b>${esc(item.primary_brief_id)}</b> · ${esc(item.primary_question)}</div>
      <div class="helper"><b>${esc(item.related_brief_id)}</b> · ${esc(item.related_question)}</div>
      <div class="meta-row">
        ${(item.shared_signals || []).map((signal) => `<span class="pill">${esc(signal)}</span>`).join('')}
      </div>
    </article>
  `;
}

function decisionPatternCard(pattern) {
  return `
    <article class="doc-card">
      <h3>${esc(pattern.title)}</h3>
      <p>${esc(pattern.summary)}</p>
      <div class="meta-row">
        <span class="pill">${esc(pattern.pattern_type)}</span>
      </div>
      <ul class="stack-list">${(pattern.signals || []).map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
    </article>
  `;
}

function themeClusterCard(cluster) {
  return `
    <article class="doc-card">
      <h3>${esc(cluster.theme)}</h3>
      <p>${esc(cluster.summary)}</p>
      <div class="metric-grid">
        ${renderMetricCard('briefs', cluster.brief_count)}
        ${renderMetricCard('approved', cluster.approved_count)}
      </div>
      <ul class="stack-list">${(cluster.representative_questions || []).map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
    </article>
  `;
}

function topicCard(topic) {
  return `
    <article class="doc-card">
      <h3>${esc(topic.theme)}</h3>
      <p>${esc(topic.summary)}</p>
      <div class="metric-grid">
        ${renderMetricCard('briefs', topic.related_briefs)}
        ${renderMetricCard('findings', topic.related_findings)}
        ${renderMetricCard('approvals', topic.approval_count)}
      </div>
    </article>
  `;
}

function researchNotebookCard(notebook) {
  return `
    <article class="demo-card">
      <h3>${esc(notebook.title)}</h3>
      <p>${esc(notebook.summary)}</p>
      <div class="meta-row">
        ${notebook.project_name ? `<span class="pill">${esc(notebook.project_name)}</span>` : '<span class="pill">org-scoped</span>'}
        ${notebook.brief_id ? `<span class="pill">${esc(notebook.brief_id)}</span>` : ''}
        <span class="pill">${esc(notebook.evidence_bundle_name)}</span>
      </div>
      ${notebook.brief_question ? `<div class="helper">${esc(notebook.brief_question)}</div>` : ''}
      <ul class="stack-list">${(notebook.key_takeaways || []).map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
      <div class="helper"><b>Handoff:</b> ${esc(notebook.handoff_summary)}</div>
      <div class="helper"><b>Continuity:</b> ${esc(notebook.continuity_note)}</div>
    </article>
  `;
}

function reviewerOperationsCard(ops) {
  return `
    <article class="demo-card">
      <h3>Reviewer operations summary</h3>
      <div class="metric-grid">
        ${renderMetricCard('assignments', ops.total_assignments)}
        ${renderMetricCard('due soon', ops.due_soon_assignments)}
        ${renderMetricCard('stale', ops.stale_assignments)}
        ${renderMetricCard('escalated', ops.escalated_assignments)}
      </div>
    </article>
    ${(ops.sla_cues || []).map((cue) => `
      <article class="demo-card">
        <h3>${esc(cue.brief_question)}</h3>
        <p>${esc(cue.recommendation)}</p>
        <div class="meta-row">
          <span class="pill">${esc(cue.queue_name)}</span>
          <span class="pill">${esc(cue.urgency)}</span>
          <span class="pill">${esc(cue.age_days)} days</span>
        </div>
        <div class="helper">${esc(cue.assignee_actor_id)} · ${esc(cue.brief_id)}</div>
      </article>
    `).join('')}
    ${(ops.escalations || []).map((item) => `
      <article class="demo-card">
        <h3>${esc(item.brief_question)}</h3>
        <p>${esc(item.escalation_reason)}</p>
        <div class="meta-row">
          <span class="pill">${esc(item.urgency)}</span>
          <span class="pill">${esc(item.destination_queue)}</span>
          <span class="pill">${esc(item.status)}</span>
        </div>
        <div class="helper">${esc(item.note)}</div>
      </article>
    `).join('')}
  `;
}

function workspaceSearchResultCard(hit) {
  return `
    <article class="demo-card">
      <h3>${esc(hit.title)}</h3>
      <p>${esc(hit.excerpt)}</p>
      <div class="meta-row">
        <span class="pill">${esc(hit.hit_type)}</span>
        <span class="pill">${esc(hit.topic)}</span>
        <span class="pill">${esc(hit.status)}</span>
      </div>
      <div class="helper">${esc(hit.ref_id)}</div>
    </article>
  `;
}

function sourceOperationsCard(data) {
  return `
    <article class="demo-card">
      <h3>Evidence operations summary</h3>
      <p>${esc(data.summary.summary)}</p>
      <div class="metric-grid">
        ${renderMetricCard('tracked sources', data.summary.total_tracked_sources)}
        ${renderMetricCard('watchlists', data.summary.watchlisted_sources)}
        ${renderMetricCard('stale', data.summary.stale_sources)}
        ${renderMetricCard('superseded', data.summary.superseded_sources)}
      </div>
    </article>
    ${(data.watchlists || []).map((item) => `
      <article class="demo-card">
        <h3>${esc(item.title)}</h3>
        <p>${esc(item.watch_reason)}</p>
        <div class="meta-row">
          <span class="pill">${esc(item.manifest_source_id)}</span>
          <span class="pill">${esc(item.freshness_status)}</span>
          <span class="pill">${esc(item.source_age_days)} days</span>
        </div>
        <div class="helper">${esc(item.recommended_action)}</div>
      </article>
    `).join('')}
    ${(data.freshness_alerts || []).map((item) => `
      <article class="demo-card">
        <h3>${esc(item.title)}</h3>
        <p>${esc(item.alert_reason)}</p>
        <div class="meta-row">
          <span class="pill">${esc(item.freshness_status)}</span>
          <span class="pill">${esc(item.source_age_days)} days</span>
        </div>
        <div class="helper">${esc(item.change_summary)}</div>
      </article>
    `).join('')}
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
  workspaceResearchPacks.innerHTML = data.research_packs.length ? data.research_packs.map(researchPackCard).join('') : '<div class="empty-state"><h3>No research packs yet</h3><p>Create one to preserve recurring analyst questions and evidence review paths.</p></div>';
  workspaceQuestionPacks.innerHTML = data.question_packs.length ? data.question_packs.map(questionPackCard).join('') : '<div class="empty-state"><h3>No question packs yet</h3><p>Create a reusable starting point for repeated analyst research.</p></div>';
  workspaceScenarioTemplates.innerHTML = data.scenario_templates.length ? data.scenario_templates.map(scenarioTemplateCard).join('') : '<div class="empty-state"><h3>No templates yet</h3><p>Scenario templates will appear here.</p></div>';
  workspacePersonaPresets.innerHTML = data.persona_presets.length ? data.persona_presets.map(personaPresetCard).join('') : '<div class="empty-state"><h3>No persona presets yet</h3><p>Persona presets will appear here.</p></div>';
  workspacePrecedentComparisons.innerHTML = data.precedent_comparisons.length ? data.precedent_comparisons.map(precedentComparisonCard).join('') : '<div class="empty-state"><h3>No precedent comparisons yet</h3><p>Related Brief comparisons will appear as the workspace grows.</p></div>';
  workspaceDecisionPatterns.innerHTML = data.decision_patterns.length ? data.decision_patterns.map(decisionPatternCard).join('') : '<div class="empty-state"><h3>No decision patterns yet</h3><p>Review patterns will appear as more decisions are recorded.</p></div>';
  workspaceThemeClusters.innerHTML = data.theme_clusters.length ? data.theme_clusters.map(themeClusterCard).join('') : '<div class="empty-state"><h3>No theme clusters yet</h3><p>Topic clusters will appear as more related work is created.</p></div>';
  workspaceTopics.innerHTML = data.topic_browser.length ? data.topic_browser.map(topicCard).join('') : '<div class="empty-state"><h3>No topics yet</h3><p>Topic browsing will appear as more Briefs are created.</p></div>';
  workspaceResearchNotebooks.innerHTML = data.research_notebooks.length ? data.research_notebooks.map(researchNotebookCard).join('') : '<div class="empty-state"><h3>No research notebooks yet</h3><p>Create a notebook to preserve continuity between review sessions.</p></div>';
  workspaceReviewerOperations.innerHTML = data.reviewer_operations ? reviewerOperationsCard(data.reviewer_operations) : '<div class="empty-state"><h3>No reviewer operations summary yet</h3><p>Reviewer workload signals will appear here.</p></div>';
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
  workspaceSearchResults.innerHTML = '';
}

function renderSourceOperations(data) {
  sourceOperationsState = data;
  workspaceSourceOperations.innerHTML = sourceOperationsCard(data);
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
    const [data, sourceOps] = await Promise.all([
      apiJson('/v1/workspace/overview', { method: 'GET', headers: actorHeaders(false) }),
      apiJson('/v1/source-versions/operations', { method: 'GET', headers: actorHeaders(false) })
    ]);
    renderWorkspaceOverview(data);
    renderSourceOperations(sourceOps);
  } catch (error) {
    workspaceProjects.innerHTML = `<div class="alert error">${esc(error.message)}</div>`;
    workspaceSourceOperations.innerHTML = `<div class="alert error">${esc(error.message)}</div>`;
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

async function createWorkspaceResearchPack() {
  const payload = {
    project_id: document.getElementById('researchPackProject').value,
    name: document.getElementById('researchPackName').value.trim(),
    summary: document.getElementById('researchPackSummary').value.trim(),
    recurring_questions: document.getElementById('researchPackQuestions').value.trim(),
    next_review_date: document.getElementById('researchPackReviewDate').value
  };
  if (!payload.name || !payload.summary || !payload.recurring_questions) {
    alert('Research pack name, summary, and recurring questions are required.');
    return;
  }
  try {
    await apiJson('/v1/workspace/research-packs', { method: 'POST', body: JSON.stringify(payload) });
    document.getElementById('researchPackName').value = '';
    document.getElementById('researchPackSummary').value = '';
    document.getElementById('researchPackQuestions').value = '';
    document.getElementById('researchPackReviewDate').value = '';
    await loadWorkspaceOverview(true);
  } catch (error) {
    alert(error.message);
  }
}

async function createWorkspaceQuestionPack() {
  if (!can('workspaceQuestionPack')) {
    alert(permissionText('workspaceQuestionPack'));
    return;
  }
  const payload = {
    project_id: document.getElementById('questionPackProject').value,
    name: document.getElementById('questionPackName').value.trim(),
    summary: document.getElementById('questionPackSummary').value.trim(),
    persona: document.getElementById('questionPackPersona').value.trim(),
    template_kind: document.getElementById('questionPackTemplateKind').value.trim(),
    starter_question: document.getElementById('questionPackStarterQuestion').value.trim(),
    question_prompts: document.getElementById('questionPackPrompts').value.trim()
  };
  if (!payload.name || !payload.summary || !payload.starter_question || !payload.question_prompts) {
    alert('Question pack name, summary, starter question, and prompts are required.');
    return;
  }
  try {
    await apiJson('/v1/workspace/question-packs', { method: 'POST', body: JSON.stringify(payload) });
    document.getElementById('questionPackName').value = '';
    document.getElementById('questionPackSummary').value = '';
    document.getElementById('questionPackStarterQuestion').value = '';
    document.getElementById('questionPackPrompts').value = '';
    await loadWorkspaceOverview(true);
  } catch (error) {
    alert(error.message);
  }
}

async function createWorkspaceResearchNotebook() {
  if (!can('workspaceResearchNotebook')) {
    alert(permissionText('workspaceResearchNotebook'));
    return;
  }
  const payload = {
    project_id: document.getElementById('researchNotebookProject').value,
    brief_id: currentBriefId,
    title: document.getElementById('researchNotebookTitle').value.trim(),
    summary: document.getElementById('researchNotebookSummary').value.trim(),
    key_takeaways: document.getElementById('researchNotebookTakeaways').value.trim(),
    evidence_bundle_name: document.getElementById('researchNotebookBundle').value.trim(),
    handoff_summary: document.getElementById('researchNotebookHandoff').value.trim(),
    continuity_note: document.getElementById('researchNotebookContinuity').value.trim()
  };
  if (!payload.title || !payload.summary || !payload.key_takeaways || !payload.evidence_bundle_name || !payload.handoff_summary || !payload.continuity_note) {
    alert('Notebook title, summary, takeaways, evidence bundle, handoff summary, and continuity note are required.');
    return;
  }
  try {
    await apiJson('/v1/workspace/research-notebooks', { method: 'POST', body: JSON.stringify(payload) });
    document.getElementById('researchNotebookTitle').value = '';
    document.getElementById('researchNotebookSummary').value = '';
    document.getElementById('researchNotebookTakeaways').value = '';
    document.getElementById('researchNotebookBundle').value = '';
    document.getElementById('researchNotebookHandoff').value = '';
    document.getElementById('researchNotebookContinuity').value = '';
    await loadWorkspaceOverview(true);
  } catch (error) {
    alert(error.message);
  }
}

async function createWorkspaceReviewEscalation() {
  if (!can('workspaceReviewEscalation')) {
    alert(permissionText('workspaceReviewEscalation'));
    return;
  }
  if (!currentBriefId) {
    alert('Open a brief first so we know which brief is being escalated.');
    return;
  }
  const primaryAssignment = (workspaceOverviewState?.assignments || []).find((item) => item.brief_id === currentBriefId);
  const payload = {
    assignment_id: primaryAssignment?.assignment_id || '',
    brief_id: currentBriefId,
    escalation_reason: document.getElementById('reviewEscalationReason').value.trim(),
    urgency: document.getElementById('reviewEscalationUrgency').value.trim(),
    destination_queue: document.getElementById('reviewEscalationQueue').value.trim(),
    note: document.getElementById('reviewEscalationNote').value.trim()
  };
  if (!payload.escalation_reason || !payload.destination_queue || !payload.note) {
    alert('Escalation reason, destination queue, and note are required.');
    return;
  }
  try {
    await apiJson('/v1/workspace/review-escalations', { method: 'POST', body: JSON.stringify(payload) });
    document.getElementById('reviewEscalationReason').value = '';
    document.getElementById('reviewEscalationNote').value = '';
    await loadWorkspaceOverview(true);
  } catch (error) {
    alert(error.message);
  }
}

async function runWorkspaceSearch() {
  if (!can('workspaceDiscoverySearch')) {
    alert(permissionText('workspaceDiscoverySearch'));
    return;
  }
  const payload = {
    query: document.getElementById('workspaceSearchQuery').value.trim(),
    facet: document.getElementById('workspaceSearchFacet').value.trim()
  };
  if (!payload.query) {
    alert('Enter a search query first.');
    return;
  }
  workspaceSearchResults.innerHTML = '<div class="alert warning">Searching local workspace data…</div>';
  try {
    const results = await apiJson('/v1/workspace/discovery/search', { method: 'POST', body: JSON.stringify(payload) });
    workspaceSearchResults.innerHTML = results.hits.length
      ? results.hits.map(workspaceSearchResultCard).join('')
      : '<div class="empty-state"><h3>No matching workspace results</h3><p>Try a broader theme, a different facet, or create more related work first.</p></div>';
  } catch (error) {
    workspaceSearchResults.innerHTML = `<div class="alert error">${esc(error.message)}</div>`;
  }
}

async function createSourceWatchlist() {
  const payload = {
    manifest_source_id: document.getElementById('watchlistManifestSourceId').value.trim(),
    watch_reason: document.getElementById('watchlistReason').value.trim(),
    desired_check_frequency: document.getElementById('watchlistFrequency').value.trim()
  };
  if (!payload.manifest_source_id || !payload.watch_reason) {
    alert('Manifest source id and watch reason are required.');
    return;
  }
  try {
    await apiJson('/v1/source-versions/watchlists', { method: 'POST', body: JSON.stringify(payload) });
    document.getElementById('watchlistManifestSourceId').value = '';
    document.getElementById('watchlistReason').value = '';
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
      evidence.innerHTML = `
        <div class="alert warning"><b>Insufficient evidence.</b> No Brief will be created until the question has stronger cited support.</div>
        ${renderAnswerDiagnostics(data.diagnostics)}
      `;
      return;
    }
    evidence.innerHTML = `
      <div class="alert success"><b>Grounded evidence found.</b> ${data.findings.length} cited passage(s) are available for a reviewable Brief.</div>
      ${renderAnswerDiagnostics(data.diagnostics)}
      <div class="metric-grid">
        ${data.findings.slice(0, 3).map((finding) => `
          <article class="metric-card">
            <strong>${esc(finding.citation.source_id)}</strong>
            <span>${esc(finding.citation.locator)}</span>
            <small>${esc(finding.citation.freshness_status || 'unknown')} · ${esc(String(finding.citation.source_age_days || 0))}d</small>
          </article>
        `).join('')}
      </div>
    `;
  } catch (error) {
    evidence.innerHTML = `<div class="alert error">${esc(error.message)}</div>`;
  }
}

function renderAnswerDiagnostics(diagnostics) {
  if (!diagnostics) return '';
  return `
    <div class="brief-section">
      <h3>Evidence diagnostics</h3>
      <div class="meta-row">
        <span class="pill">${esc(diagnostics.sufficiency)}</span>
        <span class="pill">${esc(String(diagnostics.retrieval_result_count || 0))} retrieval results</span>
      </div>
      <ul class="stack-list">${(diagnostics.reasons || []).map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
      ${(diagnostics.query_refinements || []).length ? `<h4>Refine the question</h4><ul class="stack-list">${diagnostics.query_refinements.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>` : ''}
      ${(diagnostics.context_hints || []).length ? `<h4>Improve the context</h4><ul class="stack-list">${diagnostics.context_hints.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>` : ''}
      <p class="helper">${esc(diagnostics.next_best_action || '')}</p>
    </div>
  `;
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

function renderImplementationBundle(bundle) {
  return `
    <section class="brief-section">
      <h3>Implementation acceleration pack</h3>
      <p>${esc(bundle.handoff_summary.summary)}</p>
      <div class="metric-grid">
        ${renderMetricCard('work items', bundle.handoff_summary.work_item_count)}
        ${renderMetricCard('tracks', bundle.handoff_summary.implementation_track_count)}
        ${renderMetricCard('starter artifacts', bundle.starter_artifacts.length)}
        ${renderMetricCard('change signals', bundle.change_impact.source_change_signals.length)}
      </div>
      <div class="button-row compact">
        ${buttonHtml({ label: 'Download implementation pack', action: 'implementationBundle', className: 'secondary', onClick: `downloadJsonWithHeaders('/v1/implementation/briefs/${esc(bundle.brief_id)}/bundle','${esc(bundle.brief_id)}-implementation-pack.json')` })}
      </div>
    </section>

    <section class="brief-section">
      <h3>Reference architecture patterns</h3>
      <div class="doc-grid">
        ${bundle.architecture_patterns.map((pattern) => `
          <article class="doc-card">
            <h3>${esc(pattern.title)}</h3>
            <p>${esc(pattern.rationale)}</p>
            <div class="meta-row"><span class="pill">${esc(pattern.workflow)}</span></div>
            <ul class="stack-list">${pattern.implementation_notes.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
          </article>
        `).join('')}
      </div>
    </section>

    <section class="brief-section">
      <h3>Acceptance criteria and validation</h3>
      <ul class="stack-list">${bundle.test_plan.acceptance_criteria.map((item) => `<li><b>${esc(item.title)}</b> · ${esc(item.owner_focus)}<br><span class="helper">${esc(item.expected_outcome)}</span></li>`).join('')}</ul>
      <h4>Negative cases</h4>
      <ul class="stack-list">${bundle.test_plan.negative_cases.map((item) => `<li><b>${esc(item.title)}</b><br><span class="helper">${esc(item.expectation)}</span></li>`).join('')}</ul>
    </section>

    <section class="brief-section">
      <h3>Starter code artifacts</h3>
      <div class="doc-grid">
        ${bundle.starter_artifacts.map((artifact) => `
          <article class="doc-card">
            <h3>${esc(artifact.file_name)}</h3>
            <p>${esc(artifact.artifact_type)}</p>
            <pre>${esc(artifact.code)}</pre>
          </article>
        `).join('')}
      </div>
    </section>

    <section class="brief-section">
      <h3>Change impact</h3>
      <ul class="stack-list">${bundle.change_impact.source_change_signals.map((item) => `<li><b>${esc(item.source_id)}</b> · ${esc(item.change_status)}<br><span class="helper">${esc(item.brief_source_version)} → ${esc(item.latest_known_version)} · ${esc(item.action_hint)}</span></li>`).join('')}</ul>
    </section>
  `;
}

async function loadImplementationBundle(briefId) {
  if (!can('implementationBundle')) {
    alert(permissionText('implementationBundle'));
    return '';
  }
  try {
    const bundle = await apiJson(`/v1/implementation/briefs/${encodeURIComponent(briefId)}/bundle`, { method: 'GET', headers: actorHeaders(false) });
    return renderImplementationBundle(bundle);
  } catch (error) {
    return `<section class="brief-section"><div class="alert error">${esc(error.message)}</div></section>`;
  }
}

async function openBrief(id) {
  try {
    currentBriefId = id;
    const brief = await apiJson(`${api}/${id}`, { method: 'GET', headers: actorHeaders(false) });
    const implementationPack = brief.status === 'approved' ? await loadImplementationBundle(brief.brief_id) : '';
    const findings = brief.findings.map((finding) => `
      <article class="finding" id="finding-${esc(finding.finding_id)}">
        <div class="brief-status">${esc(finding.kind)} · ${esc(finding.confidence)}</div>
        <p>${esc(finding.statement)}</p>
        <div class="citation">
          <b>${esc(finding.citation.source_id)} ${esc(finding.citation.source_version)}</b><br>
          ${esc(finding.citation.locator)}<br>
          ${esc(finding.citation.support)}<br>
          <span class="helper">${esc(finding.citation.freshness_status || 'unknown')} · ${esc(String(finding.citation.source_age_days || 0))} days old</span><br>
          <span class="helper">${esc(finding.citation.change_summary || '')}</span>
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
          ${buttonHtml({ label: 'Implementation pack', action: 'implementationBundle', className: 'secondary', onClick: `openBrief('${esc(brief.brief_id)}')` })}
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

      ${implementationPack}
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

function attestationForm() {
  return `
    <article class="admin-card">
      <h4>Record operator sign-off</h4>
      <label>Policy area
        <input id="attestationPolicyArea" placeholder="connector-governance">
      </label>
      <label>Environment
        <input id="attestationEnvironment" placeholder="staging">
      </label>
      <label>Attestation type
        <input id="attestationType" placeholder="change_acknowledgment">
      </label>
      <label>Control IDs
        <input id="attestationControls" placeholder="connector-mode,retention-review">
      </label>
      <label>Change summary
        <textarea id="attestationSummary" placeholder="Preview-only connector path reviewed before governed enablement."></textarea>
      </label>
      <label>Acknowledgment
        <textarea id="attestationAcknowledgment" placeholder="I confirm the environment policy, rollback path, and retention expectations were reviewed."></textarea>
      </label>
      <div class="button-row">
        <button onclick="recordOperationsAttestation()">Record sign-off</button>
      </div>
    </article>
  `;
}

function pilotSuccessForm() {
  return `
    <article class="admin-card">
      <h4>Record pilot milestone</h4>
      <label>Milestone
        <input id="pilotMilestoneName" placeholder="Evaluator walkthrough completed">
      </label>
      <label>Owner role
        <select id="pilotOwnerRole">
          <option value="reviewer">reviewer</option>
          <option value="approver">approver</option>
          <option value="auditor">auditor</option>
          <option value="administrator">administrator</option>
        </select>
      </label>
      <label>Target outcome
        <textarea id="pilotTargetOutcome" placeholder="Evaluator sees a grounded brief workflow and current trust posture end to end."></textarea>
      </label>
      <label>Status
        <select id="pilotStatus">
          <option value="planned">planned</option>
          <option value="in_progress">in_progress</option>
          <option value="completed">completed</option>
        </select>
      </label>
      <label>Note
        <textarea id="pilotNote" placeholder="Capture what changed, what was learned, or what should happen next."></textarea>
      </label>
      <div class="button-row">
        <button onclick="recordPilotCheckpoint()">Save milestone</button>
      </div>
    </article>
  `;
}

function tenantProvisioningForm() {
  return `
    <article class="admin-card">
      <h4>Create tenant provisioning request</h4>
      <label>Tenant key
        <input id="tenantProvisioningKey" placeholder="tenant_delta_provider">
      </label>
      <label>Tenant name
        <input id="tenantProvisioningName" placeholder="Delta Provider Network">
      </label>
      <label>Deployment model
        <select id="tenantProvisioningModel">
          <option value="private_customer_space">private_customer_space</option>
          <option value="hosted_evaluator_workspace">hosted_evaluator_workspace</option>
        </select>
      </label>
      <label>Environment shape
        <select id="tenantProvisioningShape">
          <option value="single-tenant-private">single-tenant-private</option>
          <option value="hosted-shared-control">hosted-shared-control</option>
        </select>
      </label>
      <label>Delegated admin
        <input id="tenantProvisioningAdmin" placeholder="delta.admin">
      </label>
      <label>Capabilities
        <input id="tenantProvisioningCapabilities" placeholder="team_workspace,developer_workflows,synthetic_labs">
      </label>
      <label>Onboarding summary
        <textarea id="tenantProvisioningSummary" placeholder="Private customer space for payer/provider workflow planning with delegated enterprise admin ownership."></textarea>
      </label>
      <div class="button-row">
        <button onclick="createTenantProvisioningRequest()">Create request</button>
      </div>
    </article>
  `;
}

async function createTenantProvisioningRequest() {
  if (!can('tenantAdministration')) {
    alert(permissionText('tenantAdministration'));
    return;
  }
  const payload = {
    tenant_key: document.getElementById('tenantProvisioningKey').value.trim(),
    tenant_name: document.getElementById('tenantProvisioningName').value.trim(),
    deployment_model: document.getElementById('tenantProvisioningModel').value,
    environment_shape: document.getElementById('tenantProvisioningShape').value,
    delegated_admin: document.getElementById('tenantProvisioningAdmin').value.trim(),
    requested_capabilities: document.getElementById('tenantProvisioningCapabilities').value.split(',').map((item) => item.trim()).filter(Boolean),
    onboarding_summary: document.getElementById('tenantProvisioningSummary').value.trim()
  };
  if (!payload.tenant_key || !payload.tenant_name || !payload.delegated_admin || !payload.onboarding_summary) {
    alert('Tenant key, tenant name, delegated admin, and onboarding summary are required.');
    return;
  }
  try {
    await apiJson('/v1/admin/tenants/provisioning-requests', { method: 'POST', body: JSON.stringify(payload) });
    await openAdminPanel('tenantAdministration');
  } catch (error) {
    alert(error.message);
  }
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
            ${renderMetricCard('insufficient evidence rate', `${(dashboard.answer_readiness.insufficient_evidence_rate * 100).toFixed(0)}%`)}
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
        <article class="admin-card">
          <h4>Answer readiness</h4>
          <p>${esc(dashboard.answer_readiness.readiness_summary)}</p>
          <ul class="stack-list">${dashboard.answer_readiness.recommended_focus.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
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

    if (panel === 'operationsConfiguration') {
      if (!can('operationsConfiguration')) throw new Error(permissionText('operationsConfiguration'));
      const config = await apiJson('/v1/admin/operations/configuration', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Configuration and secret policy</h3>
          <p class="helper">${esc(config.summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('deployment tier', config.deployment_tier)}
            ${renderMetricCard('environments', config.environments.length)}
            ${renderMetricCard('config boundaries', config.config_boundaries.length)}
            ${renderMetricCard('secret refs', config.secret_references.length)}
          </div>
        </article>
        <article class="admin-card">
          <h4>Environment policies</h4>
          <div class="doc-grid">
            ${config.environments.map((environment) => `
              <article class="doc-card">
                <h3>${esc(environment.environment_name)}</h3>
                <p>${esc(environment.promotion_gate)}</p>
                <ul class="stack-list">
                  <li><b>Secrets:</b> ${esc(environment.secret_boundary)}</li>
                  <li><b>Window:</b> ${esc(environment.change_window)}</li>
                  <li><b>Data:</b> ${esc(environment.data_boundary)}</li>
                </ul>
              </article>
            `).join('')}
          </div>
        </article>
        <article class="admin-card">
          <h4>Config classification</h4>
          <ul class="stack-list">${config.config_boundaries.map((item) => `<li><b>${esc(item.key)}</b> · ${esc(item.classification)} · ${esc(item.source)}<br><span class="helper">${esc(item.exposure_policy)} — ${esc(item.rationale)}</span></li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Secret references</h4>
          <ul class="stack-list">${config.secret_references.map((item) => `<li><b>${esc(item.system)}</b> · ${esc(item.reference)}<br><span class="helper">${esc(item.rotation_expectation)} · ${esc(item.usage_boundary)}</span></li>`).join('')}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'operationsObservability') {
      if (!can('operationsObservability')) throw new Error(permissionText('operationsObservability'));
      const ops = await apiJson('/v1/admin/operations/observability', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Observability and retention operations</h3>
          <p class="helper">${esc(ops.summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('briefs (30d)', ops.health_signals.briefs_last_30_days)}
            ${renderMetricCard('answers (30d)', ops.health_signals.answers_last_30_days)}
            ${renderMetricCard('invalid validations (30d)', ops.health_signals.invalid_validations_last_30_days)}
            ${renderMetricCard('blocked deliveries', ops.health_signals.blocked_deliveries)}
            ${renderMetricCard('changes requested', ops.health_signals.changes_requested_briefs)}
            ${renderMetricCard('attestations', ops.retention_signals.attestation_events)}
          </div>
        </article>
        <article class="admin-card">
          <h4>Incident runbooks</h4>
          <ul class="stack-list">${ops.incident_runbooks.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Retention posture</h4>
          <ul class="stack-list">
            <li><b>Tracked export retention:</b> ${esc(ops.retention_signals.tracked_export_retention_days)} days</li>
            <li><b>Validation telemetry:</b> ${esc(ops.retention_signals.validation_telemetry_retention)}</li>
            <li><b>Audit evidence:</b> ${esc(ops.retention_signals.audit_evidence_retention)}</li>
          </ul>
        </article>
        <article class="admin-card">
          <h4>Operator narrative</h4>
          <ul class="stack-list">${ops.operator_narratives.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'operationsContinuity') {
      if (!can('operationsContinuity')) throw new Error(permissionText('operationsContinuity'));
      const continuity = await apiJson('/v1/admin/operations/continuity', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Backup, restore, and migration continuity</h3>
          <p class="helper">${esc(continuity.summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('briefs', continuity.continuity_inventory.total_briefs)}
            ${renderMetricCard('audit events', continuity.continuity_inventory.total_audit_events)}
            ${renderMetricCard('tracked exports', continuity.continuity_inventory.total_tracked_exports)}
            ${renderMetricCard('validation runs', continuity.continuity_inventory.total_validation_runs)}
            ${renderMetricCard('flyway', continuity.continuity_inventory.latest_flyway_version || 'n/a')}
          </div>
        </article>
        <article class="admin-card">
          <h4>Backup guidance</h4>
          <ul class="stack-list">${continuity.backup_guidance.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
          <h4>Restore checks</h4>
          <ul class="stack-list">${continuity.restore_checks.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Migration checks</h4>
          <ul class="stack-list">${continuity.migration_checks.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
          <h4>Recovery rehearsals</h4>
          <ul class="stack-list">${continuity.recovery_rehearsals.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'operationsUsage') {
      if (!can('operationsUsage')) throw new Error(permissionText('operationsUsage'));
      const usage = await apiJson('/v1/admin/operations/usage', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Tenant-aware usage and quota signals</h3>
          <p class="helper">${esc(usage.summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('orgs', usage.usage_summary.active_organizations)}
            ${renderMetricCard('briefs (30d)', usage.usage_summary.briefs_last_30_days)}
            ${renderMetricCard('answers (30d)', usage.usage_summary.answers_last_30_days)}
            ${renderMetricCard('validations (30d)', usage.usage_summary.validations_last_30_days)}
            ${renderMetricCard('exports (30d)', usage.usage_summary.tracked_exports_last_30_days)}
            ${renderMetricCard('inbound cases (30d)', usage.usage_summary.inbound_cases_last_30_days)}
          </div>
        </article>
        <article class="admin-card">
          <h4>Quota status</h4>
          <ul class="stack-list">${usage.quota_statuses.map((item) => `<li><b>${esc(item.metric)}</b> · observed ${esc(item.observed)} / limit ${esc(item.soft_limit)} · ${esc(item.status)}<br><span class="helper">${esc(item.window)} · ${esc(item.rationale)}</span></li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Cost-control signals</h4>
          <ul class="stack-list">${usage.cost_signals.map((item) => `<li><b>${esc(item.title)}</b> · ${esc(item.signal)}<br><span class="helper">${esc(item.explanation)}</span></li>`).join('')}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'tenantAdministration') {
      if (!can('tenantAdministration')) throw new Error(permissionText('tenantAdministration'));
      const [overview, analytics, provisioning] = await Promise.all([
        apiJson('/v1/admin/tenants/overview', { method: 'GET', headers: actorHeaders(false) }),
        apiJson('/v1/admin/tenants/analytics', { method: 'GET', headers: actorHeaders(false) }),
        apiJson('/v1/admin/tenants/provisioning-requests', { method: 'GET', headers: actorHeaders(false) })
      ]);
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Tenant administration and customer foundations</h3>
          <p class="helper">${esc(overview.hosted_product_posture.tenancy_summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('customer tenants', overview.customer_tenants.length)}
            ${renderMetricCard('isolation boundaries', overview.isolation_boundaries.length)}
            ${renderMetricCard('delegated roles', overview.role_delegations.length)}
            ${renderMetricCard('provisioning requests', overview.provisioning_requests.length)}
          </div>
        </article>
        <article class="admin-card">
          <h4>Customer tenants</h4>
          <ul class="stack-list">${overview.customer_tenants.map((tenant) => `<li><b>${esc(tenant.display_name)}</b> · ${esc(tenant.tenant_tier)} · ${esc(tenant.deployment_model)}<br><span class="helper">${tenant.user_count} users · ${tenant.project_count} projects · ${tenant.brief_count} briefs · ${esc(tenant.status)}</span></li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Isolation boundaries</h4>
          <ul class="stack-list">${overview.isolation_boundaries.map((item) => `<li><b>${esc(item.title)}</b><br>${esc(item.summary)}<br><span class="helper">${item.enforced_through.map((entry) => esc(entry)).join(' · ')}</span></li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Role delegation</h4>
          <ul class="stack-list">${overview.role_delegations.map((item) => `<li><b>${esc(item.delegated_admin)}</b> · ${esc(item.organization_id)}<br><span class="helper">${item.assigned_roles.map((role) => esc(role)).join(', ')} · ${esc(item.delegation_summary)}</span></li>`).join('')}</ul>
        </article>
        ${tenantProvisioningForm()}
        <article class="admin-card">
          <h4>Provisioning requests</h4>
          <ul class="stack-list">${provisioning.map((item) => `<li><b>${esc(item.tenant_name)}</b> · ${esc(item.deployment_model)} · ${esc(item.environment_shape)}<br><span class="helper">${esc(item.status)} · delegated admin ${esc(item.delegated_admin)} · ${item.requested_capabilities.map((capability) => esc(capability)).join(', ')}</span></li>`).join('') || '<li>No provisioning requests yet.</li>'}</ul>
        </article>
        <article class="admin-card">
          <h4>Tenant analytics</h4>
          <div class="metric-grid">
            ${renderMetricCard('total tenants', analytics.usage_summary.total_tenants)}
            ${renderMetricCard('active tenants', analytics.usage_summary.active_tenants)}
            ${renderMetricCard('private deployments', analytics.usage_summary.private_deployment_tenants)}
            ${renderMetricCard('hosted evaluations', analytics.usage_summary.hosted_evaluation_tenants)}
          </div>
          <ul class="stack-list">${analytics.tenant_usage.map((item) => `<li><b>${esc(item.display_name)}</b> · ${esc(item.engagement_signal)}<br><span class="helper">${item.users} users · ${item.projects} projects · ${item.briefs_last_30_days} briefs (30d) · ${esc(item.packaging_fit)}</span></li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Hosted packaging artifacts</h4>
          <ul class="stack-list">${overview.hosted_packaging_artifacts.map((item) => `<li><b>${esc(item.title)}</b> · ${esc(item.audience)}<br><span class="helper">${esc(item.summary)} · ${item.included_capabilities.map((capability) => esc(capability)).join(', ')}</span></li>`).join('')}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'regulatedReadiness') {
      if (!can('regulatedReadiness')) throw new Error(permissionText('regulatedReadiness'));
      const readiness = await apiJson('/v1/admin/regulated-readiness', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Regulated deployment readiness</h3>
          <p class="helper">${esc(readiness.regulated_deployment_narratives[0])}</p>
          <div class="metric-grid">
            ${renderMetricCard('dependency evidence', readiness.security_posture.dependency_evidence.length)}
            ${renderMetricCard('control mappings', readiness.compliance_evidence_pack.control_mappings.length)}
            ${renderMetricCard('architecture packs', readiness.deployment_architecture_pack.architecture_views.length)}
            ${renderMetricCard('release controls', readiness.release_governance_pack.release_controls.length)}
            ${renderMetricCard('recovery artifacts', readiness.resilience_readiness_pack.recovery_artifacts.length)}
          </div>
        </article>
        <article class="admin-card">
          <h4>Security posture and supply chain</h4>
          <p class="helper">${esc(readiness.security_posture.summary)}</p>
          <ul class="stack-list">${readiness.security_posture.dependency_evidence.map((item) => `<li><b>${esc(item.component)}</b> · ${esc(item.evidence_type)}<br><span class="helper">${esc(item.current_state)} · ${esc(item.enterprise_narrative)}</span></li>`).join('')}</ul>
          <h4>Supply-chain controls</h4>
          <ul class="stack-list">${readiness.security_posture.supply_chain_controls.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Compliance evidence pack</h4>
          <p class="helper">${esc(readiness.compliance_evidence_pack.summary)}</p>
          <ul class="stack-list">${readiness.compliance_evidence_pack.control_mappings.map((item) => `<li><b>${esc(item.control_id)}</b> · ${esc(item.title)}<br><span class="helper">${esc(item.mapped_surface)} · ${esc(item.evidence_artifact)} · ${esc(item.current_coverage)}</span></li>`).join('')}</ul>
          <h4>Audit-facing artifacts</h4>
          <ul class="stack-list">${readiness.compliance_evidence_pack.audit_facing_artifacts.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Deployment architecture packs</h4>
          <div class="doc-grid">
            ${readiness.deployment_architecture_pack.architecture_views.map((item) => `
              <article class="doc-card">
                <h3>${esc(item.title)}</h3>
                <p>${esc(item.summary)}</p>
                <div class="meta-row"><span class="pill">${esc(item.deployment_target)}</span></div>
                <ul class="stack-list">${item.design_notes.map((note) => `<li>${esc(note)}</li>`).join('')}</ul>
              </article>
            `).join('')}
          </div>
        </article>
        <article class="admin-card">
          <h4>Release governance</h4>
          <p class="helper">${esc(readiness.release_governance_pack.summary)}</p>
          <ul class="stack-list">${readiness.release_governance_pack.release_controls.map((item) => `<li><b>${esc(item.control_id)}</b> · ${esc(item.title)}<br><span class="helper">${esc(item.change_stage)} · ${esc(item.evidence_requirement)}</span></li>`).join('')}</ul>
          <h4>Retention and change artifacts</h4>
          <ul class="stack-list">${readiness.release_governance_pack.change_management_artifacts.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Resilience readiness</h4>
          <p class="helper">${esc(readiness.resilience_readiness_pack.summary)}</p>
          <ul class="stack-list">${readiness.resilience_readiness_pack.recovery_artifacts.map((item) => `<li><b>${esc(item.title)}</b> · ${esc(item.resilience_area)} · ${esc(item.status)}<br><span class="helper">${esc(item.summary)}</span></li>`).join('')}</ul>
          <h4>Future roadmap signals</h4>
          <ul class="stack-list">${readiness.resilience_readiness_pack.future_roadmap_signals.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'operationsAttestations') {
      if (!can('operationsAttestations')) throw new Error(permissionText('operationsAttestations'));
      const attestation = await apiJson('/v1/admin/operations/attestations', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Policy attestation and operator sign-off history</h3>
          <p class="helper">${esc(attestation.summary)}</p>
          <ul class="stack-list">${attestation.expected_attestations.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
        </article>
        ${attestationForm()}
        <article class="admin-card">
          <h4>Recent sign-offs</h4>
          <ul class="stack-list">${(attestation.recent_attestations || []).map((item) => `<li><b>${esc(item.policy_area)}</b> · ${esc(item.environment_name)} · ${esc(item.attestation_type)}<br><span class="helper">${esc(item.actor_id)} (${esc(item.actor_role)}) · ${esc(item.change_summary)}</span>${item.control_ids.length ? `<br><span class="helper">Controls: ${item.control_ids.map((controlId) => esc(controlId)).join(', ')}</span>` : ''}</li>`).join('') || '<li>No sign-offs recorded yet.</li>'}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'pilotReadiness') {
      if (!can('pilotReadiness')) throw new Error(permissionText('pilotReadiness'));
      const readiness = await apiJson('/v1/pilot/readiness', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Pilot readiness</h3>
          <p class="helper">${esc(readiness.summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('tier', readiness.readiness_summary.readiness_tier)}
            ${renderMetricCard('completed checks', readiness.readiness_summary.completed_checks, `${readiness.readiness_summary.total_checks} total`)}
            ${renderMetricCard('private pilot ready', readiness.readiness_summary.pilot_ready_for_private_evaluation ? 'yes' : 'not yet')}
          </div>
        </article>
        <article class="admin-card">
          <h4>Checklist</h4>
          <ul class="stack-list">${readiness.checklist.map((item) => `<li><b>${esc(item.title)}</b> · ${esc(item.status)} · owner ${esc(item.owner_role)}<br><span class="helper">${esc(item.rationale)}</span></li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Readiness artifacts</h4>
          <ul class="stack-list">${readiness.artifacts.map((item) => `<li><b>${esc(item.title)}</b> · ${esc(item.artifact_type)} · ${esc(item.intended_audience)}<br><span class="helper">${esc(item.description)}</span></li>`).join('')}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'solutionPacks') {
      if (!can('solutionPacks')) throw new Error(permissionText('solutionPacks'));
      const packs = await apiJson('/v1/pilot/solution-packs', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Solution packs</h3>
          <p class="helper">${esc(packs.summary)}</p>
        </article>
        <article class="admin-card">
          <div class="doc-grid">
            ${packs.packs.map((pack) => `
              <article class="doc-card">
                <h3>${esc(pack.audience)}</h3>
                <p>${esc(pack.positioning)}</p>
                <h4>Workflows</h4>
                <ul class="stack-list">${pack.workflows.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
                <h4>Demo angles</h4>
                <ul class="stack-list">${pack.demo_angles.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
                <h4>Trust angles</h4>
                <ul class="stack-list">${pack.trust_angles.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
              </article>
            `).join('')}
          </div>
        </article>
      `;
      return;
    }

    if (panel === 'stakeholderReport') {
      if (!can('stakeholderReport')) throw new Error(permissionText('stakeholderReport'));
      const report = await apiJson('/v1/pilot/stakeholder-report', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Stakeholder reporting pack</h3>
          <p class="helper">${esc(report.summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('briefs', report.executive_summary.total_briefs, `approved ${report.executive_summary.approved_briefs}`)}
            ${renderMetricCard('projects', report.executive_summary.active_projects)}
            ${renderMetricCard('blocked deliveries', report.executive_summary.blocked_deliveries)}
            ${renderMetricCard('quality gate', report.executive_summary.quality_gate_decision)}
            ${renderMetricCard('assignments', report.delivery_summary.assignments)}
            ${renderMetricCard('attestations', report.trust_summary.attestations)}
          </div>
        </article>
        <article class="admin-card">
          <h4>Reporting artifacts</h4>
          <ul class="stack-list">${report.reporting_artifacts.map((item) => `<li><b>${esc(item.title)}</b> · ${esc(item.audience)}<br><span class="helper">${esc(item.source_view)} · ${esc(item.use_case)}</span></li>`).join('')}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'futureRoadmap') {
      if (!can('futureRoadmap')) throw new Error(permissionText('futureRoadmap'));
      const roadmap = await apiJson('/v1/pilot/future-roadmap', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Future control roadmap</h3>
          <p class="helper">${esc(roadmap.summary)}</p>
        </article>
        <article class="admin-card">
          <h4>Current vs target state</h4>
          <ul class="stack-list">${roadmap.current_state.map((item) => `<li><b>${esc(item.area)}</b><br><span class="helper">Current: ${esc(item.current_state)}</span><br><span class="helper">Target: ${esc(item.target_state)}</span></li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Roadmap tracks</h4>
          <div class="doc-grid">
            ${roadmap.roadmap_tracks.map((track) => `
              <article class="doc-card">
                <h3>${esc(track.track)}</h3>
                <p>${esc(track.focus)}</p>
                <h4>Near term</h4>
                <ul class="stack-list">${track.near_term_assets.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
                <h4>Future outcomes</h4>
                <ul class="stack-list">${track.future_outcomes.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
              </article>
            `).join('')}
          </div>
        </article>
      `;
      return;
    }

    if (panel === 'pilotSuccess') {
      if (!can('pilotSuccess')) throw new Error(permissionText('pilotSuccess'));
      const success = await apiJson('/v1/pilot/success', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Pilot success plan</h3>
          <p class="helper">${esc(success.summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('projects', success.adoption_signals.active_projects)}
            ${renderMetricCard('reviewer assignments', success.adoption_signals.reviewer_assignments)}
            ${renderMetricCard('approved briefs', success.adoption_signals.approved_briefs)}
            ${renderMetricCard('tracked exports', success.adoption_signals.tracked_exports)}
            ${renderMetricCard('inbound cases', success.adoption_signals.inbound_cases)}
          </div>
        </article>
        ${pilotSuccessForm()}
        <article class="admin-card">
          <h4>Milestones</h4>
          <ul class="stack-list">${success.checkpoints.map((item) => `<li><b>${esc(item.milestone_name)}</b> · ${esc(item.status)} · owner ${esc(item.owner_role)}<br><span class="helper">${esc(item.target_outcome)}</span><br><span class="helper">${esc(item.note)}</span></li>`).join('')}</ul>
        </article>
      `;
      return;
    }

    if (panel === 'syntheticLabs') {
      if (!can('syntheticLabs')) throw new Error(permissionText('syntheticLabs'));
      const labs = await apiJson('/v1/synthetic-labs/overview', { method: 'GET', headers: actorHeaders(false) });
      enterprisePanel.innerHTML = `
        <article class="admin-card">
          <h3>Synthetic interoperability labs</h3>
          <p class="helper">${esc(labs.summary)}</p>
          <div class="metric-grid">
            ${renderMetricCard('templates', labs.coverage_summary.total_templates)}
            ${renderMetricCard('valid paths', labs.coverage_summary.valid_templates)}
            ${renderMetricCard('negative drills', labs.coverage_summary.negative_templates)}
            ${renderMetricCard('journeys', labs.coverage_summary.supported_journeys.length)}
          </div>
        </article>
        <article class="admin-card">
          <h4>Scenario templates</h4>
          <div class="doc-grid">
            ${labs.templates.map((template) => `
              <article class="doc-card">
                <h3>${esc(template.title)}</h3>
                <p>${esc(template.description)}</p>
                <div class="meta-row">
                  <span class="pill">${esc(template.journey_type)}</span>
                  <span class="pill">${esc(template.expected_validation_status)}</span>
                </div>
                <ul class="stack-list">${template.coverage_tags.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
                <div class="button-row">
                  <button onclick="runSyntheticLab('${esc(template.template_id)}')">Run lab</button>
                  <button class="secondary" onclick="compareSyntheticLabs('${esc(template.template_id)}','negative_bundle_structure')">Compare to negative</button>
                </div>
              </article>
            `).join('')}
          </div>
        </article>
        <article class="admin-card">
          <h4>Support matrix</h4>
          <ul class="stack-list">${labs.support_matrix.map((item) => `<li><b>${esc(item.workflow_area)}</b> · ${esc(item.coverage_status)}<br><span class="helper">${esc(item.notes)}</span></li>`).join('')}</ul>
        </article>
        <article class="admin-card">
          <h4>Validation gaps</h4>
          <ul class="stack-list">${labs.validation_gaps.map((item) => `<li><b>${esc(item.area)}</b> · ${esc(item.severity)}<br><span class="helper">${esc(item.gap)} · next: ${esc(item.suggested_next_template)}</span></li>`).join('')}</ul>
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

async function recordOperationsAttestation() {
  try {
    await apiJson('/v1/admin/operations/attestations', {
      method: 'POST',
      body: JSON.stringify({
        policy_area: document.getElementById('attestationPolicyArea').value.trim(),
        environment_name: document.getElementById('attestationEnvironment').value.trim(),
        attestation_type: document.getElementById('attestationType').value.trim(),
        change_summary: document.getElementById('attestationSummary').value.trim(),
        control_ids: document.getElementById('attestationControls').value.trim()
          ? document.getElementById('attestationControls').value.split(',').map((item) => item.trim()).filter(Boolean)
          : [],
        acknowledgment: document.getElementById('attestationAcknowledgment').value.trim()
      })
    });
    await openAdminPanel('operationsAttestations');
  } catch (error) {
    alert(error.message);
  }
}

async function recordPilotCheckpoint() {
  try {
    await apiJson('/v1/pilot/success', {
      method: 'POST',
      body: JSON.stringify({
        milestone_name: document.getElementById('pilotMilestoneName').value.trim(),
        owner_role: document.getElementById('pilotOwnerRole').value.trim(),
        target_outcome: document.getElementById('pilotTargetOutcome').value.trim(),
        status: document.getElementById('pilotStatus').value.trim(),
        note: document.getElementById('pilotNote').value.trim()
      })
    });
    await openAdminPanel('pilotSuccess');
  } catch (error) {
    alert(error.message);
  }
}

async function runSyntheticLab(templateId) {
  enterprisePanel.innerHTML = '<div class="alert warning">Running synthetic workflow rehearsal…</div>';
  try {
    const run = await apiJson('/v1/synthetic-labs/runs', {
      method: 'POST',
      body: JSON.stringify({ template_id: templateId })
    });
    enterprisePanel.innerHTML = `
      <article class="admin-card">
        <h3>${esc(run.title)}</h3>
        <p class="helper">${esc(run.summary)}</p>
        <div class="metric-grid">
          ${renderMetricCard('journey', run.journey.journey_type)}
          ${renderMetricCard('bundle type', run.replay_metadata.bundle_type)}
          ${renderMetricCard('assertions', run.assertions.length)}
          ${renderMetricCard('validation', run.bundle_review.validation.status)}
        </div>
        <div class="button-row compact">
          <button class="secondary" onclick="openAdminPanel('syntheticLabs')">Back to labs</button>
        </div>
      </article>
      <article class="admin-card">
        <h4>Assertions</h4>
        <ul class="stack-list">${run.assertions.map((item) => `<li><b>${esc(item.title)}</b> · ${esc(item.status)}<br><span class="helper">${esc(item.detail)}</span></li>`).join('')}</ul>
      </article>
      <article class="admin-card">
        <h4>Timeline</h4>
        <ul class="stack-list">${run.timeline.map((item) => `<li><b>${esc(item.title)}</b> · ${esc(item.owner_actor)}<br><span class="helper">${esc(item.expected_output)}</span></li>`).join('')}</ul>
      </article>
      <article class="admin-card">
        <h4>Expected outcomes</h4>
        <ul class="stack-list">${run.expected_outcomes.map((item) => `<li>${esc(item)}</li>`).join('')}</ul>
      </article>
      <article class="admin-card">
        <h4>Bundle review highlights</h4>
        <ul class="stack-list">${run.bundle_review.scenario_findings.map((item) => `<li><b>${esc(item.title)}</b> · ${esc(item.severity)}<br><span class="helper">${esc(item.detail)}</span></li>`).join('')}</ul>
      </article>
    `;
  } catch (error) {
    enterprisePanel.innerHTML = `<div class="alert error">${esc(error.message)}</div>`;
  }
}

async function compareSyntheticLabs(primaryTemplateId, comparisonTemplateId) {
  enterprisePanel.innerHTML = '<div class="alert warning">Comparing synthetic workflow rehearsals…</div>';
  try {
    const comparison = await apiJson('/v1/synthetic-labs/compare', {
      method: 'POST',
      body: JSON.stringify({
        primary_template_id: primaryTemplateId,
        comparison_template_id: comparisonTemplateId
      })
    });
    enterprisePanel.innerHTML = `
      <article class="admin-card">
        <h3>Synthetic lab comparison</h3>
        <p class="helper">${esc(comparison.summary)}</p>
        <div class="button-row compact">
          <button class="secondary" onclick="openAdminPanel('syntheticLabs')">Back to labs</button>
        </div>
      </article>
      <article class="admin-card">
        <h4>Differences</h4>
        <ul class="stack-list">${comparison.differences.map((item) => `<li><b>${esc(item.area)}</b><br><span class="helper">${esc(item.primary_value)} vs ${esc(item.comparison_value)}</span><br><span class="helper">${esc(item.impact)}</span></li>`).join('')}</ul>
      </article>
      <article class="admin-card">
        <h4>Timeline comparison</h4>
        <ul class="stack-list">${comparison.timeline_comparisons.map((item) => `<li><b>${esc(item.primary_title)}</b> ↔ <b>${esc(item.comparison_title)}</b><br><span class="helper">${esc(item.note)}</span></li>`).join('')}</ul>
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
document.getElementById('createResearchPackBtn').addEventListener('click', createWorkspaceResearchPack);
document.getElementById('createQuestionPackBtn').addEventListener('click', createWorkspaceQuestionPack);
document.getElementById('createResearchNotebookBtn').addEventListener('click', createWorkspaceResearchNotebook);
document.getElementById('createReviewEscalationBtn').addEventListener('click', createWorkspaceReviewEscalation);
document.getElementById('runWorkspaceSearchBtn').addEventListener('click', runWorkspaceSearch);
document.getElementById('createWatchlistBtn').addEventListener('click', createSourceWatchlist);
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
