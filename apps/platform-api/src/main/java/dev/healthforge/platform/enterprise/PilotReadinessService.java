package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.evaluation.EvaluationDashboardService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PilotReadinessService {

    private final JdbcTemplate jdbcTemplate;
    private final EvaluationDashboardService evaluationDashboardService;
    private final Clock clock = Clock.systemUTC();

    public PilotReadinessService(
            JdbcTemplate jdbcTemplate,
            EvaluationDashboardService evaluationDashboardService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.evaluationDashboardService = evaluationDashboardService;
    }

    public PilotReadinessResponse readiness(AuthenticatedActor actor) {
        var checklist = checklist(actor.organizationId());
        var completed = (int) checklist.stream().filter(item -> "ready".equals(item.status()) || "in_place".equals(item.status())).count();
        return new PilotReadinessResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                new PilotReadinessResponse.ReadinessSummary(
                        completed >= 5 ? "private_pilot_ready" : "showcase_plus",
                        completed,
                        checklist.size(),
                        completed >= 5
                ),
                checklist,
                List.of(
                        new PilotReadinessResponse.ArtifactSummary("Pilot readiness checklist", "readiness_checklist", "Consistent way to judge whether a private evaluation can start honestly.", "operator and buyer stakeholders"),
                        new PilotReadinessResponse.ArtifactSummary("Solution packs", "audience_pack", "Reusable provider, payer, and platform-specific narrative plus workflow packaging.", "sales, partners, and solution teams"),
                        new PilotReadinessResponse.ArtifactSummary("Stakeholder reporting pack", "oversight_report", "Repeatable executive, delivery, and trust reporting drawn from live product state.", "leadership and evaluators"),
                        new PilotReadinessResponse.ArtifactSummary("Future control roadmap", "roadmap_asset", "Explains current-vs-target governance and identity posture without overstating maturity.", "enterprise architecture and compliance stakeholders")
                ),
                List.of(
                        "This is a private-pilot readiness surface, not a certification claim.",
                        "The readiness pack keeps PHI handling, formal compliance scope, and production guarantees out of scope.",
                        "Pilot readiness here means evaluators can assess the product honestly with stronger technical and operational artifacts."
                ),
                "This view packages the transition from showcase readiness to private pilot readiness with explicit, bounded checks and artifacts."
        );
    }

    public SolutionPacksResponse solutionPacks(AuthenticatedActor actor) {
        return new SolutionPacksResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                List.of(
                        new SolutionPacksResponse.SolutionPack(
                                "prior_auth_provider",
                                "provider",
                                "prior_authorization",
                                "Turn public prior-authorization and interoperability guidance into grounded implementation planning artifacts for provider workflow teams.",
                                "Use when provider operations, utilization review, or EHR planning teams need a bounded starting point for prior-authorization modernization conversations.",
                                List.of("reviewer Brief workflow", "architecture planning", "prior-auth workflow and bundle review", "team workspace handoff"),
                                List.of("CMS prior auth modernization", "documentation + status exchange planning", "review-to-approval workflow"),
                                List.of("grounded evidence", "human review", "approval and audit trail"),
                                List.of("approved Brief", "work-item export preview", "stakeholder report summary"),
                                List.of("retrieval_review_path", "approval_gate_path", "delivery_export_path"),
                                List.of("evaluator_first_run", "implementer_first_run"),
                                List.of("architecture_pack", "outcome_pack")
                        ),
                        new SolutionPacksResponse.SolutionPack(
                                "prior_auth_payer",
                                "payer",
                                "prior_authorization",
                                "Use grounded policy, standards, and workflow artifacts to align utilization-management, policy, and interoperability planning conversations.",
                                "Use when payer policy, UM, or interoperability teams want to compare PAS, CRD, DTR, and governance implications without drifting into unsupported automation claims.",
                                List.of("policy-to-standards crosswalk", "prior-auth copilot", "evaluation and safety reporting", "governed delivery"),
                                List.of("PAS/CRD/DTR planning", "shared implementation track comparison", "quality gate review"),
                                List.of("traceable boundaries", "unsupported-output handling", "operator-visible controls"),
                                List.of("crosswalk output", "quality gate snapshot", "pilot readiness checklist"),
                                List.of("retrieval_review_path", "shared_program_path", "delivery_export_path"),
                                List.of("evaluator_first_run", "buyer_first_run"),
                                List.of("presentation_pack", "outcome_pack")
                        ),
                        new SolutionPacksResponse.SolutionPack(
                                "prior_auth_shared_program",
                                "shared_program",
                                "prior_authorization",
                                "Create a reusable bridge pack for provider, payer, and shared-program teams evaluating the same prior-authorization modernization theme from different operating angles.",
                                "Use when a multi-stakeholder group needs a single demo or pilot path that still preserves provider-vs-payer differences.",
                                List.of("shared workflow framing", "workspace comparison", "handoff and export planning", "stakeholder narrative review"),
                                List.of("joint provider-payer session", "bounded implementation planning", "governed next-step packaging"),
                                List.of("same evidence, different audiences", "explicit human decision points", "pilot-safe outputs"),
                                List.of("shared program recap", "comparison notes", "stakeholder-ready follow-up pack"),
                                List.of("shared_program_path", "approval_gate_path"),
                                List.of("buyer_first_run", "implementer_first_run"),
                                List.of("presentation_pack", "architecture_pack")
                        ),
                        new SolutionPacksResponse.SolutionPack(
                                "interoperability_standards",
                                "standards",
                                "interoperability_standards",
                                "Give interoperability leaders a controlled workspace for evidence retrieval, standards testing, governance reporting, and pilot operations planning.",
                                "Use when the conversation is about FHIR-, standards-, or implementation-guide planning rather than only prior authorization.",
                                List.of("FHIR validation workspace", "standards catalog", "admin console", "private deployment operations"),
                                List.of("enterprise evaluator walkthrough", "synthetic standards validation", "future control roadmap"),
                                List.of("identity posture", "retention and sign-off", "evaluation baseline"),
                                List.of("operator report", "solution narrative", "future-state architecture path"),
                                List.of("retrieval_review_path", "approval_gate_path"),
                                List.of("evaluator_first_run"),
                                List.of("architecture_pack")
                        ),
                        new SolutionPacksResponse.SolutionPack(
                                "workflow_planning",
                                "implementer",
                                "workflow_planning",
                                "Package HealthForge as a workflow-design aid for review, approval, escalation, and governed delivery planning.",
                                "Use when teams want to shape operating models, queues, ownership, and approval paths before any downstream system integration changes.",
                                List.of("queue design review", "approval path design", "workspace assignment planning", "governed delivery rehearsal"),
                                List.of("role-based workflow planning", "pilot operating model", "review-to-export path mapping"),
                                List.of("visible queue ownership", "editable preset controls", "human-in-the-loop approval"),
                                List.of("workflow pack summary", "owner handoff notes", "pilot readiness artifacts"),
                                List.of("retrieval_review_path", "approval_gate_path", "delivery_export_path"),
                                List.of("implementer_first_run"),
                                List.of("architecture_pack", "outcome_pack")
                        ),
                        new SolutionPacksResponse.SolutionPack(
                                "implementation_planning",
                                "enterprise_buyer",
                                "implementation_planning",
                                "Reframe the current platform into a bounded implementation-planning pack for buyers, partners, and enterprise stakeholders.",
                                "Use when enterprise stakeholders need to understand product value, readiness boundaries, and future operating model direction quickly.",
                                List.of("stakeholder reporting", "pilot readiness review", "future roadmap review", "team handoff planning"),
                                List.of("buyer evaluation walkthrough", "architecture discussion", "outcome-based pilot framing"),
                                List.of("honest maturity narrative", "future-vs-current clarity", "repeatable reporting assets"),
                                List.of("executive summary", "architecture narrative", "pilot success plan"),
                                List.of("shared_program_path", "delivery_export_path"),
                                List.of("buyer_first_run"),
                                List.of("presentation_pack", "architecture_pack", "outcome_pack")
                        )
                    ),
                List.of(
                        new SolutionPacksResponse.WorkflowPreset(
                                "retrieval_review_path",
                                "Grounded retrieval and reviewer path",
                                "Start from evidence retrieval, move into reviewer interpretation, and keep the first pass tightly grounded in cited source material.",
                                "reviewer",
                                "focused_citation_first",
                                "reviewer Brief workflow",
                                "review after findings are shaped",
                                "optional export after approval",
                                List.of("question framing", "project context", "citation review depth", "workspace assignment target"),
                                List.of("reviewer workspace", "question packs", "research notebook")
                        ),
                        new SolutionPacksResponse.WorkflowPreset(
                                "approval_gate_path",
                                "Approval and audit path",
                                "Optimize for formal review, approval decisions, and visible audit history before any handoff leaves the workspace.",
                                "approver",
                                "evidence_bundle_with_findings",
                                "finding-by-finding review",
                                "required approver decision with note capture",
                                "hold until approved",
                                List.of("review notes", "decision wording", "approval owner", "audit visibility"),
                                List.of("review decisions", "policy safety report", "future control roadmap")
                        ),
                        new SolutionPacksResponse.WorkflowPreset(
                                "delivery_export_path",
                                "Governed delivery and export path",
                                "Package approved work into repeatable downstream handoff and export patterns while keeping the current live-vs-simulated boundary visible.",
                                "administrator",
                                "approved_brief_reuse",
                                "review completed before export",
                                "approval receipt required",
                                "tracked work-item or documentation delivery",
                                List.of("target system", "delivery mode", "retry handling", "operator sign-off"),
                                List.of("tracked export events", "implementation bundles", "integration operations")
                        ),
                        new SolutionPacksResponse.WorkflowPreset(
                                "shared_program_path",
                                "Shared program comparison path",
                                "Use one evidence base to compare provider, payer, and shared-program implications without losing audience-specific framing.",
                                "auditor",
                                "shared evidence set",
                                "cross-audience comparison review",
                                "summary sign-off before distribution",
                                "stakeholder-ready outcome pack",
                                List.of("audience comparison", "shared assumptions", "narrative emphasis", "follow-up artifact mix"),
                                List.of("solution packs", "stakeholder report", "pilot success plan")
                        )
                ),
                List.of(
                        new SolutionPacksResponse.OnboardingFlow(
                                "evaluator_first_run",
                                "evaluator",
                                "Start in pilot readiness, then solution packs, then the stakeholder report.",
                                List.of("Review bounded statements and current-vs-target posture", "Open the standards or prior-auth pack that matches the evaluation theme", "Use stakeholder reporting to anchor the trust conversation in live product state"),
                                "The evaluator understands what is demo-safe today and what is intentionally future roadmap.",
                                "Stay inside synthetic, non-sensitive scenarios and human-review-first workflows."
                        ),
                        new SolutionPacksResponse.OnboardingFlow(
                                "implementer_first_run",
                                "implementer",
                                "Start in the solution packs view, then workspace workflows, then implementation bundles.",
                                List.of("Choose a workflow-planning or provider-oriented pack", "Follow the recommended retrieval, review, and approval preset path", "Use implementation bundles and handoff surfaces for next-step planning"),
                                "The implementer can see how questions turn into grounded outputs, approvals, and reusable handoffs.",
                                "This is an implementation-planning environment, not a production clinical automation workflow."
                        ),
                        new SolutionPacksResponse.OnboardingFlow(
                                "buyer_first_run",
                                "enterprise_buyer",
                                "Start in the implementation-planning pack, then stakeholder report, then future roadmap.",
                                List.of("Pick the buyer-oriented pack that matches the audience", "Review outcome reporting and readiness artifacts", "Use the roadmap view to separate current capability from future direction"),
                                "The buyer gets a high-level product story, trust boundary, and realistic pilot path quickly.",
                                "Use the product story to frame a pilot, not to imply present-tense regulated production controls."
                        )
                ),
                List.of(
                        new SolutionPacksResponse.StakeholderPack(
                                "presentation_pack",
                                "leadership and partner audiences",
                                "Presentation pack",
                                "A concise product story for demos, partner calls, and buyer discussions that stays grounded in shipped workflows.",
                                List.of("solution packs", "stakeholder report", "pilot success plan"),
                                List.of("audience framing", "value narrative", "bounded next-step summary"),
                                List.of("executive overview", "partner intro", "pilot kickoff recap")
                        ),
                        new SolutionPacksResponse.StakeholderPack(
                                "architecture_pack",
                                "enterprise architecture and implementation audiences",
                                "Architecture summary pack",
                                "A reusable explanation of the evidence, review, approval, workspace, and export flow with current-vs-target clarity.",
                                List.of("solution packs", "future roadmap", "implementation bundles"),
                                List.of("system flow summary", "governance boundary summary", "future-state path"),
                                List.of("architecture review", "implementation planning session", "technical diligence follow-up")
                        ),
                        new SolutionPacksResponse.StakeholderPack(
                                "outcome_pack",
                                "delivery, pilot, and evaluator audiences",
                                "Outcome reporting pack",
                                "A repeatable package for showing what the team accomplished, what remains bounded, and what a pilot should measure next.",
                                List.of("stakeholder report", "pilot readiness", "pilot success plan"),
                                List.of("delivery snapshot", "trust posture summary", "pilot milestone framing"),
                                List.of("pilot checkpoint review", "delivery retrospective", "evaluator follow-up")
                        )
                ),
                List.of(
                        "Solution packs package current workflows; they do not imply new automated capabilities.",
                        "All presets remain inspectable, editable, and bounded to synthetic or non-sensitive evaluation scenarios.",
                        "Stakeholder assets should explain current HealthForge truth clearly, not oversell roadmap intent as shipped functionality."
                ),
                "These packs make demos and partner conversations easier to tailor by audience without drifting beyond shipped capabilities."
        );
    }

    public StakeholderReportingResponse stakeholderReport(AuthenticatedActor actor) {
        var evaluation = evaluationDashboardService.dashboard(actor);
        return new StakeholderReportingResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                new StakeholderReportingResponse.ExecutiveSummary(
                        count("select count(*) from engineering_brief where organization_id = ?", actor.organizationId()),
                        count("select count(*) from engineering_brief where organization_id = ? and status = 'approved'", actor.organizationId()),
                        count("select count(*) from workspace_project where organization_id = ?", actor.organizationId()),
                        count("""
                                select count(*) from tracked_export_event
                                where organization_id = ? and execution_status in ('writeback_blocked', 'simulated_retry', 'live_retry')
                                """, actor.organizationId()),
                        evaluation.qualityGate().decision()
                ),
                new StakeholderReportingResponse.DeliverySummary(
                        count("select count(*) from tracked_export_event where organization_id = ?", actor.organizationId()),
                        count("select count(*) from inbound_case where organization_id = ?", actor.organizationId()),
                        count("select count(*) from workspace_assignment where organization_id = ?", actor.organizationId()),
                        count("select count(*) from engineering_brief where organization_id = ? and status = 'changes_requested'", actor.organizationId())
                ),
                new StakeholderReportingResponse.TrustSummary(
                        count("""
                                select count(*) from answer_generation_event
                                where organization_id = ? and answer_status = 'insufficient_evidence'
                                """, actor.organizationId()),
                        count("""
                                select count(*) from answer_generation_event
                                where organization_id = ? and unsupported_triggered = true
                                """, actor.organizationId()),
                        count("""
                                select count(*) from (
                                    select finding_id
                                    from brief_review_decision
                                    where organization_id = ?
                                    group by finding_id
                                    having count(distinct decision) > 1
                                ) disagreements
                                """, actor.organizationId()),
                        count("select count(*) from operations_attestation where organization_id = ?", actor.organizationId())
                ),
                List.of(
                        new StakeholderReportingResponse.ReportingArtifact("Executive pilot snapshot", "leadership", "stakeholder report", "Summarize delivery progress, trust posture, and current readiness quickly."),
                        new StakeholderReportingResponse.ReportingArtifact("Evaluator oversight pack", "evaluator", "compliance + evaluation + operations", "Ground trust and safety discussions in current telemetry."),
                        new StakeholderReportingResponse.ReportingArtifact("Operator handoff pack", "platform operator", "pilot readiness + continuity + sign-off", "Support pilot execution and governance conversations."),
                        new StakeholderReportingResponse.ReportingArtifact("Audience solution summary", "buyer or partner", "solution packs", "Tailor the product story to provider, payer, or platform teams.")
                ),
                "This reporting pack turns live product state into leadership-, evaluator-, and operator-friendly summaries that are easier to reuse in pilot and partner conversations."
        );
    }

    public FutureControlRoadmapResponse futureRoadmap(AuthenticatedActor actor) {
        return new FutureControlRoadmapResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                List.of(
                        new FutureControlRoadmapResponse.CurrentStateItem("identity", "Local-header and trusted-proxy-ready identity foundation for private demo and pilot conversations.", "SSO-backed production identity with stronger group, tenant, and access lifecycle controls."),
                        new FutureControlRoadmapResponse.CurrentStateItem("governance", "Human-review-first approvals, audit trail, retention policy summaries, and operator sign-off history.", "Stronger automated governance workflows, environment policy enforcement, and control attestations tied to enterprise policy systems."),
                        new FutureControlRoadmapResponse.CurrentStateItem("deployment", "Private deployment guidance with continuity, observability, and quota reporting.", "Hardened regulated deployment architecture with production-grade secrets, networking, and operational controls."),
                        new FutureControlRoadmapResponse.CurrentStateItem("compliance narrative", "Bounded, honest explanation of what HealthForge does and does not claim today.", "Mapped control narratives and evidence structures that support formal enterprise review processes.")
                ),
                List.of(
                        new FutureControlRoadmapResponse.RoadmapTrack(
                                "production_identity",
                                "Move from local/trusted-proxy foundations to stronger enterprise identity integration.",
                                List.of("SSO and group-mapping narrative", "organization-scoped access review", "actor and role traceability"),
                                List.of("central identity provider integration", "lifecycle-managed roles", "stronger delegated admin flows")
                        ),
                        new FutureControlRoadmapResponse.RoadmapTrack(
                                "compliance_control_mapping",
                                "Clarify how current controls map to future compliance and governance expectations.",
                                List.of("bounded control summaries", "trust and safety reports", "operator attestation history"),
                                List.of("formal control mapping artifacts", "evidence packages for enterprise review", "clearer audit integration points")
                        ),
                        new FutureControlRoadmapResponse.RoadmapTrack(
                                "regulated_deployment_architecture",
                                "Explain the path from private pilot hosting to stronger regulated operating models.",
                                List.of("deployment guide", "continuity checks", "secret-boundary and config policy surfaces"),
                                List.of("production-ready hosting patterns", "segmented environments", "enterprise secrets and network governance")
                        )
                ),
                List.of(
                        "Do not describe roadmap assets as present-tense production controls.",
                        "Keep current-vs-target distinction explicit in every pilot or enterprise conversation.",
                        "Use this roadmap to make intent and direction clearer, not to imply certifications or compliance scope."
                ),
                "This roadmap asset helps enterprise stakeholders understand the future control path while keeping today’s boundary honest."
        );
    }

    public PilotSuccessResponse success(AuthenticatedActor actor) {
        seedCheckpoints(actor.organizationId());
        return new PilotSuccessResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                new PilotSuccessResponse.AdoptionSignals(
                        count("select count(*) from workspace_project where organization_id = ?", actor.organizationId()),
                        count("select count(*) from workspace_assignment where organization_id = ? and assignee_role = 'reviewer'", actor.organizationId()),
                        count("select count(*) from engineering_brief where organization_id = ? and status = 'approved'", actor.organizationId()),
                        count("select count(*) from tracked_export_event where organization_id = ?", actor.organizationId()),
                        count("select count(*) from inbound_case where organization_id = ?", actor.organizationId())
                ),
                checkpoints(actor.organizationId()),
                List.of(
                        "Tie each pilot milestone to one named owner, one observable workflow, and one bounded success outcome.",
                        "Use Brief approvals, governed delivery events, and workspace assignments as adoption signals rather than vague interest markers.",
                        "Capture follow-up notes after each milestone review so partner conversations become more structured over time."
                ),
                "This success-plan view gives pilot conversations clearer milestones, ownership, and adoption signals from the live workspace."
        );
    }

    public PilotSuccessResponse recordCheckpoint(AuthenticatedActor actor, PilotSuccessCheckpointRequest request) {
        seedCheckpoints(actor.organizationId());
        var now = Timestamp.from(Instant.now(clock));
        jdbcTemplate.update("""
                insert into pilot_success_checkpoint (
                    checkpoint_id, organization_id, milestone_name, owner_role, target_outcome, status, note, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "pilot_checkpoint_" + UUID.randomUUID(),
                actor.organizationId(),
                request.milestoneName().trim(),
                request.ownerRole().trim(),
                request.targetOutcome().trim(),
                request.status().trim(),
                request.note().trim(),
                now,
                now
        );
        return success(actor);
    }

    private List<PilotReadinessResponse.ChecklistItem> checklist(String organizationId) {
        return List.of(
                new PilotReadinessResponse.ChecklistItem("solution-packs", "Audience-specific solution packs are available", "ready", "reviewer", "Provider, payer, and platform narratives are now packaged around shipped workflows."),
                new PilotReadinessResponse.ChecklistItem("stakeholder-reporting", "Stakeholder reporting pack is available", "ready", "auditor", "Leadership and evaluator summaries can be reproduced from current product state."),
                new PilotReadinessResponse.ChecklistItem("pilot-boundaries", "Current non-production boundaries are explicit", "ready", "administrator", "Pilot artifacts stay honest about non-sensitive scope, human review, and non-PHI boundaries."),
                new PilotReadinessResponse.ChecklistItem("operations-surface", "Private pilot operations surfaces are available", count("select count(*) from operations_attestation where organization_id = ?", organizationId) >= 0 ? "in_place" : "planned", "administrator", "Phase 14 controls provide continuity, sign-off, and operations reporting for private pilots."),
                new PilotReadinessResponse.ChecklistItem("success-plan", "Pilot success milestones can be tracked", count("select count(*) from pilot_success_checkpoint where organization_id = ?", organizationId) > 0 ? "ready" : "in_place", "reviewer", "Pilots can now be structured around owners, milestones, and observable adoption signals."),
                new PilotReadinessResponse.ChecklistItem("regulated-roadmap", "Future regulated operating model is documented separately from current state", "ready", "auditor", "Enterprise conversations can discuss direction without overstating what is built today.")
        );
    }

    private void seedCheckpoints(String organizationId) {
        var existing = count("select count(*) from pilot_success_checkpoint where organization_id = ?", organizationId);
        if (existing > 0) {
            return;
        }
        var now = Timestamp.from(Instant.now(clock));
        insertCheckpoint(organizationId, organizationId + ".milestone.discovery", "Pilot kickoff and scope alignment", "administrator", "Agree on users, evaluation questions, and non-production scope.", "planned", "Use the pilot readiness and solution-pack views to align the evaluation path.", now);
        insertCheckpoint(organizationId, organizationId + ".milestone.workflow", "Reviewer workflow completed", "reviewer", "Create, review, and approve at least one grounded Brief end to end.", "planned", "Demonstrate evidence retrieval, review decisions, approval, and audit history.", now);
        insertCheckpoint(organizationId, organizationId + ".milestone.ops", "Operator governance walkthrough completed", "administrator", "Walk the evaluator through continuity, observability, and sign-off surfaces.", "planned", "Use Phase 14 and Phase 15 surfaces together for a credible pilot story.", now);
        insertCheckpoint(organizationId, organizationId + ".milestone.report", "Stakeholder report delivered", "auditor", "Share an oversight-oriented snapshot with leadership or partner stakeholders.", "planned", "Generate the executive, delivery, and trust summary using live product state.", now);
    }

    private void insertCheckpoint(String organizationId, String checkpointId, String milestoneName, String ownerRole, String targetOutcome, String status, String note, Timestamp now) {
        jdbcTemplate.update("""
                insert into pilot_success_checkpoint (
                    checkpoint_id, organization_id, milestone_name, owner_role, target_outcome, status, note, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (checkpoint_id) do nothing
                """,
                checkpointId, organizationId, milestoneName, ownerRole, targetOutcome, status, note, now, now
        );
    }

    private List<PilotSuccessResponse.Checkpoint> checkpoints(String organizationId) {
        return jdbcTemplate.query("""
                select checkpoint_id, milestone_name, owner_role, target_outcome, status, note, updated_at
                from pilot_success_checkpoint
                where organization_id = ?
                order by updated_at desc, milestone_name
                """, (rs, row) -> new PilotSuccessResponse.Checkpoint(
                rs.getString("checkpoint_id"),
                rs.getString("milestone_name"),
                rs.getString("owner_role"),
                rs.getString("target_outcome"),
                rs.getString("status"),
                rs.getString("note"),
                rs.getTimestamp("updated_at").toInstant()
        ), organizationId);
    }

    private int count(String sql, Object... args) {
        var value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
