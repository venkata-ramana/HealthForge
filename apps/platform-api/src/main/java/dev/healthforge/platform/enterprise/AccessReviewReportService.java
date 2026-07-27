package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class AccessReviewReportService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditPolicyProperties auditPolicyProperties;
    private final Clock clock = Clock.systemUTC();

    public AccessReviewReportService(
            JdbcTemplate jdbcTemplate,
            AuditPolicyProperties auditPolicyProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditPolicyProperties = auditPolicyProperties;
    }

    public AccessReviewReportResponse report(AuthenticatedActor actor) {
        var organizationId = actor.organizationId();
        var assignments = assignments(organizationId);
        var summary = summary(organizationId);

        return new AccessReviewReportResponse(
                organizationId,
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                new AccessReviewReportResponse.AuditPolicySummary(
                        auditPolicyProperties.policyVersion(),
                        auditPolicyProperties.trackedExportRetentionDays(),
                        auditPolicyProperties.validationTelemetryRetention(),
                        auditPolicyProperties.auditEvidenceRetention(),
                        auditPolicyProperties.accessReviewCadence(),
                        auditPolicyProperties.roleReviewExpectation(),
                        auditPolicyProperties.approvalRequiredForExports()
                ),
                summary,
                assignments,
                List.of(
                        "This report is organization scoped and excludes PHI by design.",
                        "Role assignments are inferred from the current durable identity directory and last-seen runtime activity.",
                        "Grant provenance currently reflects the actor that established or refreshed the observed role assignment in this private deployment mode.",
                        "Human review remains required before any access change is treated as approved governance action."
                ),
                "This report summarizes who currently has access in the organization, which roles they hold, when those roles were last observed, and which audit policy tier applies to this private deployment."
        );
    }

    private AccessReviewReportResponse.AccessSummary summary(String organizationId) {
        var totalUsers = count("""
                select count(distinct actor_user_id)
                from actor_organization_membership
                where organization_id = ?
                """, organizationId);
        var totalOrganizations = count("""
                select count(*)
                from actor_organization
                where organization_id = ?
                """, organizationId);
        var totalMemberships = count("""
                select count(*)
                from actor_organization_membership
                where organization_id = ?
                """, organizationId);
        var totalRoleAssignments = count("""
                select count(*)
                from actor_role_assignment
                where organization_id = ?
                """, organizationId);
        var reviewerAssignments = countRole(organizationId, "reviewer");
        var approverAssignments = countRole(organizationId, "approver");
        var auditorAssignments = countRole(organizationId, "auditor");
        var administratorAssignments = countRole(organizationId, "administrator");

        return new AccessReviewReportResponse.AccessSummary(
                totalUsers,
                totalOrganizations,
                totalMemberships,
                totalRoleAssignments,
                reviewerAssignments,
                approverAssignments,
                auditorAssignments,
                administratorAssignments
        );
    }

    private int countRole(String organizationId, String role) {
        return count("""
                select count(*)
                from actor_role_assignment
                where organization_id = ? and actor_role = ?
                """, organizationId, role);
    }

    private List<AccessReviewReportResponse.AccessAssignment> assignments(String organizationId) {
        return jdbcTemplate.query("""
                select actor_user_id, organization_id, actor_role, granted_by, granted_at, last_seen_at
                from actor_role_assignment
                where organization_id = ?
                order by actor_role, actor_user_id
                """, (rs, row) -> {
            var role = rs.getString("actor_role");
            return new AccessReviewReportResponse.AccessAssignment(
                    rs.getString("actor_user_id"),
                    rs.getString("organization_id"),
                    role,
                    rs.getString("granted_by"),
                    rs.getTimestamp("granted_at").toInstant(),
                    rs.getTimestamp("last_seen_at").toInstant(),
                    rationale(role)
            );
        }, organizationId);
    }

    private String rationale(String role) {
        return switch (role) {
            case "reviewer" -> "Reviewer access allows evidence review and finding decisions within the current organization.";
            case "approver" -> "Approver access allows reviewed Brief approval and approved work-item export flows.";
            case "auditor" -> "Auditor access allows oversight views such as audit exports, compliance telemetry, and enterprise posture inspection.";
            case "administrator" -> "Administrator access allows organization-scoped administration, identity inspection, and protected setup actions.";
            default -> "Access is recorded for oversight and should be reviewed by an administrator.";
        };
    }

    private int count(String sql, Object... args) {
        var value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
