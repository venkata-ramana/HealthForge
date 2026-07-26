package dev.healthforge.platform.compliance;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ComplianceDashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public ComplianceDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ComplianceDashboardResponse dashboard(AuthenticatedActor actor) {
        var organizationId = actor.organizationId();
        var briefMetrics = briefMetrics(organizationId);
        var validationMetrics = validationMetrics(organizationId);
        var exportMetrics = exportMetrics(organizationId);
        var recentAuditEvents = recentAuditEvents(organizationId);

        return new ComplianceDashboardResponse(
                organizationId,
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                briefMetrics,
                validationMetrics,
                exportMetrics,
                recentAuditEvents,
                List.of(
                        "Organization-scoped reads and writes are enforced for Briefs, approvals, audit events, validation telemetry, and tracker-export telemetry.",
                        "Human review remains mandatory for regulatory interpretation, implementation guidance, and downstream exports.",
                        "Only synthetic or non-sensitive FHIR examples are supported in this phase.",
                        "Tracked export previews remain preview-only with explicit retention metadata for downstream evidence governance."
                ),
                "This dashboard summarizes the current organization boundary, review activity, validation evidence, and export telemetry for enterprise oversight."
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
        var latestRetentionUntil = jdbcTemplate.query("""
                select max(retention_until) as latest_retention_until
                from tracked_export_event
                where organization_id = ?
                """, rs -> rs.next() ? timestamp(rs.getTimestamp("latest_retention_until")) : null, organizationId);
        return new ComplianceDashboardResponse.ExportMetrics(total, github, jira, latestRetentionUntil);
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

    private int count(String sql, String organizationId) {
        var value = jdbcTemplate.queryForObject(sql, Integer.class, organizationId);
        return value == null ? 0 : value;
    }

    private Instant timestamp(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
