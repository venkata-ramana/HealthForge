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
                                "provider",
                                "Turn public prior-authorization and interoperability guidance into grounded implementation planning artifacts for provider workflow teams.",
                                List.of("reviewer Brief workflow", "architecture planning", "prior-auth workflow and bundle review", "team workspace handoff"),
                                List.of("CMS prior auth modernization", "documentation + status exchange planning", "review-to-approval workflow"),
                                List.of("grounded evidence", "human review", "approval and audit trail"),
                                List.of("approved Brief", "work-item export preview", "stakeholder report summary")
                        ),
                        new SolutionPacksResponse.SolutionPack(
                                "payer",
                                "Use grounded policy, standards, and workflow artifacts to align utilization-management, policy, and interoperability planning conversations.",
                                List.of("policy-to-standards crosswalk", "prior-auth copilot", "evaluation and safety reporting", "governed delivery"),
                                List.of("PAS/CRD/DTR planning", "shared implementation track comparison", "quality gate review"),
                                List.of("traceable boundaries", "unsupported-output handling", "operator-visible controls"),
                                List.of("crosswalk output", "quality gate snapshot", "pilot readiness checklist")
                        ),
                        new SolutionPacksResponse.SolutionPack(
                                "platform_interoperability",
                                "Give interoperability leaders a controlled workspace for evidence retrieval, standards testing, governance reporting, and pilot operations planning.",
                                List.of("FHIR validation workspace", "standards catalog", "admin console", "private deployment operations"),
                                List.of("enterprise evaluator walkthrough", "synthetic standards validation", "future control roadmap"),
                                List.of("identity posture", "retention and sign-off", "evaluation baseline"),
                                List.of("operator report", "solution narrative", "future-state architecture path")
                        )
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
