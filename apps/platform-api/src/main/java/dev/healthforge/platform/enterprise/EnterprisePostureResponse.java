package dev.healthforge.platform.enterprise;

import java.util.List;

public record EnterprisePostureResponse(
        String organizationId,
        String actorId,
        String actorRole,
        String deploymentMode,
        String identityMode,
        List<String> supportedRoles,
        List<String> activeControls,
        RetentionPolicy retentionPolicy,
        AuditPolicy auditPolicy,
        List<String> currentBoundaries,
        List<String> nextEnterpriseMoves
) {
    public record RetentionPolicy(
            int trackedExportRetentionDays,
            String validationTelemetryRetention,
            String auditEvidenceRetention
    ) {
    }

    public record AuditPolicy(
            String policyVersion,
            String accessReviewCadence,
            String roleReviewExpectation,
            boolean approvalRequiredForExports
    ) {
    }
}
