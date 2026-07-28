package dev.healthforge.platform.compliance;

import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.evaluation.EvaluationDashboardService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ComplianceDashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final EvaluationDashboardService evaluationDashboardService;
    private final Clock clock = Clock.systemUTC();

    public ComplianceDashboardService(
            JdbcTemplate jdbcTemplate,
            EvaluationDashboardService evaluationDashboardService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.evaluationDashboardService = evaluationDashboardService;
    }

    public ComplianceDashboardResponse dashboard(AuthenticatedActor actor) {
        var organizationId = actor.organizationId();
        var briefMetrics = briefMetrics(organizationId);
        var validationMetrics = validationMetrics(organizationId);
        var exportMetrics = exportMetrics(organizationId);
        var evaluationMetrics = evaluationMetrics(actor);
        var recentAuditEvents = recentAuditEvents(organizationId);

        return new ComplianceDashboardResponse(
                organizationId,
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                briefMetrics,
                validationMetrics,
                exportMetrics,
                evaluationMetrics,
                recentAuditEvents,
                List.of(
                        "Organization-scoped reads and writes are enforced for Briefs, approvals, audit events, validation telemetry, tracker-export telemetry, and evaluation telemetry.",
                        "Human review remains mandatory for regulatory interpretation, implementation guidance, and downstream exports.",
                        "Only synthetic or non-sensitive FHIR examples are supported in this phase.",
                        "Tracked export, collaboration notification, documentation export, webhook automation, and evaluation reporting flows are explicit, auditable, and organization scoped with retention metadata for downstream evidence governance."
                ),
                "This dashboard summarizes the current organization boundary, review activity, validation evidence, governed integration telemetry, and evaluation trust signals for enterprise oversight."
        );
    }

    private ComplianceDashboardResponse.BriefMetrics briefMetrics(String organizationId) {
        var counts = jdbcTemplate.query("""
                select status, count(*) as brief_count
                from engineering_brief
                where organization_id = ?
                group by status
                """, rs -> {
            int total = 0;
            int draft = 0;
            int inReview = 0;
            int changesRequested = 0;
            int approved = 0;
            while (rs.next()) {
                var count = rs.getInt("brief_count");
                total += count;
                switch (rs.getString("status")) {
                    case "draft" -> draft += count;
                    case "in_review" -> inReview += count;
                    case "changes_requested" -> changesRequested += count;
                    case "approved" -> approved += count;
                    default -> {
                    }
                }
            }
            return new ComplianceDashboardResponse.BriefMetrics(total, draft, inReview, changesRequested, approved);
        }, organizationId);
        return counts == null ? new ComplianceDashboardResponse.BriefMetrics(0, 0, 0, 0, 0) : counts;
    }

    private ComplianceDashboardResponse.ValidationMetrics validationMetrics(String organizationId) {
        var total = count("""
                select count(*) from fhir_validation_run where organization_id = ?
                """, organizationId);
        var valid = count("""
                select count(*) from fhir_validation_run where organization_id = ? and validation_status = 'valid'
                """, organizationId);
        var invalid = count("""
                select count(*) from fhir_validation_run where organization_id = ? and validation_status = 'invalid'
                """, organizationId);
        return new ComplianceDashboardResponse.ValidationMetrics(total, valid, invalid, total);
    }

    private ComplianceDashboardResponse.ExportMetrics exportMetrics(String organizationId) {
        var total = count("""
                select count(*) from tracked_export_event where organization_id = ?
                """, organizationId);
        var github = count("""
                select count(*) from tracked_export_event where organization_id = ? and target_system = 'github'
                """, organizationId);
        var jira = count("""
                select count(*) from tracked_export_event where organization_id = ? and target_system = 'jira'
                """, organizationId);
        var attempts = count("""
                select count(*) from tracked_export_event where organization_id = ? and export_mode = 'governed_writeback'
                """, organizationId);
        var successful = count("""
                select count(*) from tracked_export_event
                where organization_id = ? and execution_status in ('writeback_executed', 'writeback_retried')
                """, organizationId);
        var blocked = count("""
                select count(*) from tracked_export_event
                where organization_id = ? and execution_status = 'writeback_blocked'
                """, organizationId);
        var latestRetentionUntil = jdbcTemplate.query("""
                select max(retention_until) as latest_retention_until
                from tracked_export_event
                where organization_id = ?
                """, rs -> rs.next() ? timestamp(rs.getTimestamp("latest_retention_until")) : null, organizationId);
        return new ComplianceDashboardResponse.ExportMetrics(total, github, jira, attempts, successful, blocked, latestRetentionUntil);
    }

    private List<ComplianceDashboardResponse.AuditEventSummary> recentAuditEvents(String organizationId) {
        return jdbcTemplate.query("""
                select brief_id, event_type, actor_id, actor_role, occurred_at, summary
                from brief_audit_event
                where organization_id = ?
                order by occurred_at desc
                limit 8
                """, (rs, row) -> new ComplianceDashboardResponse.AuditEventSummary(
                rs.getString("brief_id"),
                rs.getString("event_type"),
                rs.getString("actor_id"),
                rs.getString("actor_role"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getString("summary")
        ), organizationId);
    }

    private ComplianceDashboardResponse.EvaluationMetrics evaluationMetrics(AuthenticatedActor actor) {
        var disagreementFindings = count("""
                select count(*) from (
                    select finding_id
                    from brief_review_decision
                    where organization_id = ?
                    group by finding_id
                    having count(distinct decision) > 1
                ) disagreement
                """, actor.organizationId());
        var evaluationDashboard = evaluationDashboardService.dashboard(actor);
        return new ComplianceDashboardResponse.EvaluationMetrics(
                count("select count(*) from answer_generation_event where organization_id = ?", actor.organizationId()),
                count("""
                        select count(*) from answer_generation_event
                        where organization_id = ? and answer_status = 'insufficient_evidence'
                        """, actor.organizationId()),
                count("""
                        select count(*) from answer_generation_event
                        where organization_id = ? and unsupported_triggered = true
                        """, actor.organizationId()),
                disagreementFindings,
                evaluationDashboard.qualityGate().decision()
        );
    }

    private int count(String sql, String organizationId) {
        var value = jdbcTemplate.queryForObject(sql, Integer.class, organizationId);
        return value == null ? 0 : value;
    }

    private Instant timestamp(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
