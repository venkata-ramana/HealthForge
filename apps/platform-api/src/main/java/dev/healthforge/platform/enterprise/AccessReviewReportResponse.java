package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record AccessReviewReportResponse(
        String organizationId,
        String generatedBy,
        String actorRole,
        Instant generatedAt,
        AuditPolicySummary auditPolicy,
        AccessSummary accessSummary,
        List<AccessAssignment> accessAssignments,
        List<String> oversightNotes,
        String summary
) {
    public record AuditPolicySummary(
            String policyVersion,
            int trackedExportRetentionDays,
            String validationTelemetryRetention,
            String auditEvidenceRetention,
            String accessReviewCadence,
            String roleReviewExpectation,
            boolean approvalRequiredForExports
    ) {
    }

    public record AccessSummary(
            int totalUsers,
            int totalOrganizations,
            int totalMemberships,
            int totalRoleAssignments,
            int reviewerAssignments,
            int approverAssignments,
            int auditorAssignments,
            int administratorAssignments
    ) {
    }

    public record AccessAssignment(
            String actorUserId,
            String organizationId,
            String actorRole,
            String grantedBy,
            Instant grantedAt,
            Instant lastSeenAt,
            String accessRationale
    ) {
    }
}
