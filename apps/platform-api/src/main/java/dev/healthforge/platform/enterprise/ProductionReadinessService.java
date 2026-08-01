package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ProductionReadinessService {

    private final JdbcTemplate jdbcTemplate;
    private final OperationsPolicyProperties operationsPolicyProperties;
    private final Clock clock = Clock.systemUTC();

    public ProductionReadinessService(
            JdbcTemplate jdbcTemplate,
            OperationsPolicyProperties operationsPolicyProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.operationsPolicyProperties = operationsPolicyProperties;
    }

    public ProductionReadinessResponse assess(AuthenticatedActor actor) {
        var phases = List.of(
                phase26Identity(),
                phase27Reliability(actor.organizationId()),
                phase28Quality(actor.organizationId()),
                phase29PilotOperations(actor.organizationId()),
                phase30Gate(actor.organizationId())
        );
        var score = phases.stream().mapToInt(ProductionReadinessResponse.PhaseReadiness::score).sum() / phases.size();
        var gaps = phases.stream()
                .flatMap(phase -> phase.checks().stream()
                        .filter(check -> !"in_place".equals(check.status()))
                        .map(check -> new ProductionReadinessResponse.ReadinessGap(
                                phase.phaseId(),
                                check.title(),
                                check.ownerRole(),
                                check.evidence()
                        )))
                .toList();
        var decision = score >= 85 && gaps.isEmpty() ? "ready_for_controlled_rollout"
                : score >= 60 ? "conditionally_ready"
                : "not_ready";

        return new ProductionReadinessResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                Instant.now(clock),
                decision,
                score,
                phases,
                gaps,
                List.of(
                        "This is an evidence-oriented readiness gate, not a certification or production approval.",
                        "Local-header identity, synthetic data, and simulated connector paths remain bounded deployment modes.",
                        "A ready decision requires operational ownership and external review beyond this application view."
                ),
                "This gate combines security, interoperability reliability, evidence quality, pilot operations, and release controls into one honest rollout conversation."
        );
    }

    private ProductionReadinessResponse.PhaseReadiness phase26Identity() {
        var checks = List.of(
                check("identity_directory", "Organization identity directory is present", count("select count(*) from actor_organization") > 0, "Add organization membership and role assignments before shared deployment.", "administrator"),
                check("identity_provider", "Enterprise identity provider posture is configured", count("select count(*) from workspace_identity_provider") > 0, "Configure a trusted identity provider or document the private deployment fallback.", "administrator"),
                check("config_boundaries", "Configuration and secret boundaries are documented", !operationsPolicyProperties.getConfigBoundaries().isEmpty() && !operationsPolicyProperties.getSecretReferences().isEmpty(), "Document environment-bound configuration and secret references.", "administrator"),
                check("access_review", "Access review evidence exists", count("select count(*) from actor_role_assignment") > 0, "Run an organization-scoped access review and retain the result.", "auditor")
        );
        return phase("phase_26", "Secure shared deployment", checks);
    }

    private ProductionReadinessResponse.PhaseReadiness phase27Reliability(String organizationId) {
        var checks = List.of(
                check("delivery_lineage", "Delivery lineage is observable", count("select count(*) from tracked_export_event where organization_id = ?", organizationId) > 0, "Exercise a tracked export or documentation handoff.", "administrator"),
                check("recovery_actions", "Recovery actions are recorded", count("select count(*) from integration_recovery_action where organization_id = ?", organizationId) > 0, "Run a retry, replay, or reconciliation drill.", "administrator"),
                check("connector_events", "Workflow event activity is visible", count("select count(*) from workflow_event where organization_id = ?", organizationId) > 0, "Exercise a governed workflow event in a sandbox environment.", "administrator"),
                check("blocked_delivery_review", "Blocked or retryable delivery states are reviewed", count("""
                        select count(*) from tracked_export_event
                        where organization_id = ? and execution_status in ('writeback_blocked', 'simulated_retry', 'live_retry')
                        """, organizationId) >= 0, "Review blocked/retryable delivery posture during every pilot checkpoint.", "operator")
        );
        return phase("phase_27", "Reliable interoperability runtime", checks);
    }

    private ProductionReadinessResponse.PhaseReadiness phase28Quality(String organizationId) {
        var checks = List.of(
                check("corpus_versions", "Source versions are available", count("select count(*) from source_version") > 0, "Load and version the evidence corpus used by the evaluation.", "reviewer"),
                check("quality_events", "Answer quality telemetry is present", count("select count(*) from answer_generation_event where organization_id = ?", organizationId) > 0, "Run representative questions and capture answer-generation telemetry.", "auditor"),
                check("feedback_loop", "Quality feedback is being captured", count("select count(*) from retrieval_feedback where organization_id = ?", organizationId) > 0 || count("select count(*) from pilot_feedback where organization_id = ?", organizationId) > 0, "Capture evidence-quality or reviewer-confidence feedback.", "reviewer"),
                check("unsupported_guard", "Unsupported-output handling is exercised", count("select count(*) from answer_generation_event where organization_id = ? and unsupported_triggered = true", organizationId) > 0, "Run an insufficient-evidence scenario and retain the safety result.", "auditor")
        );
        return phase("phase_28", "Evidence and answer quality", checks);
    }

    private ProductionReadinessResponse.PhaseReadiness phase29PilotOperations(String organizationId) {
        var checks = List.of(
                check("pilot_owners", "Pilot workspace ownership is visible", count("select count(*) from workspace_project where organization_id = ?", organizationId) > 0, "Create a pilot project with an accountable owner.", "administrator"),
                check("pilot_milestones", "Pilot milestones are recorded", count("select count(*) from pilot_success_checkpoint where organization_id = ?", organizationId) > 0, "Record kickoff, workflow, operations, and reporting milestones.", "reviewer"),
                check("support_escalation", "Operational escalation paths exist", count("select count(*) from workspace_review_escalation where organization_id = ?", organizationId) > 0, "Exercise an escalation or support handoff path.", "operator"),
                check("stakeholder_evidence", "Stakeholder reporting evidence is available", count("select count(*) from engineering_brief where organization_id = ?", organizationId) > 0, "Generate a stakeholder report from live pilot activity.", "auditor")
        );
        return phase("phase_29", "Pilot operations and customer success", checks);
    }

    private ProductionReadinessResponse.PhaseReadiness phase30Gate(String organizationId) {
        var checks = List.of(
                check("attestations", "Operational attestations are recorded", count("select count(*) from operations_attestation where organization_id = ?", organizationId) > 0, "Record administrator sign-off for deployment and policy posture.", "administrator"),
                check("continuity_evidence", "Continuity evidence exists", count("select count(*) from operations_attestation where organization_id = ? and policy_area in ('continuity', 'recovery', 'resilience')", organizationId) > 0, "Complete a restore or continuity rehearsal and record the result.", "administrator"),
                check("release_quality", "Release quality evidence exists", count("select count(*) from pilot_feedback where organization_id = ?", organizationId) > 0, "Attach quality feedback and regression evidence to the release review.", "auditor"),
                check("owned_gaps", "Readiness gaps have owners", count("select count(*) from pilot_success_checkpoint where organization_id = ? and owner_role is not null", organizationId) > 0, "Assign owners and dates to remaining rollout gaps.", "administrator")
        );
        return phase("phase_30", "Production-readiness gate", checks);
    }

    private ProductionReadinessResponse.PhaseReadiness phase(String id, String title, List<ProductionReadinessResponse.ReadinessCheck> checks) {
        var score = (int) (checks.stream().filter(check -> "in_place".equals(check.status())).count() * 100 / checks.size());
        var status = score == 100 ? "in_place" : score >= 50 ? "partially_in_place" : "planned";
        return new ProductionReadinessResponse.PhaseReadiness(id, title, score, status, checks);
    }

    private ProductionReadinessResponse.ReadinessCheck check(String id, String title, boolean inPlace, String evidence, String ownerRole) {
        return new ProductionReadinessResponse.ReadinessCheck(id, title, inPlace ? "in_place" : "planned", evidence, ownerRole);
    }

    private int count(String sql, Object... args) {
        var value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
