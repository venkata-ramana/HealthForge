package dev.healthforge.platform.enterprise;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "healthforge.audit.policy")
public record AuditPolicyProperties(
        String policyVersion,
        int trackedExportRetentionDays,
        String validationTelemetryRetention,
        String auditEvidenceRetention,
        String accessReviewCadence,
        String roleReviewExpectation,
        boolean approvalRequiredForExports
) {
}
