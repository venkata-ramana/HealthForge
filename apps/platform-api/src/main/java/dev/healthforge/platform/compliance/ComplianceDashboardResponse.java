package dev.healthforge.platform.compliance;

import java.time.Instant;
import java.util.List;

public record ComplianceDashboardResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        BriefMetrics briefMetrics,
        ValidationMetrics validationMetrics,
        ExportMetrics exportMetrics,
        List<AuditEventSummary> recentAuditEvents,
        List<String> controls,
        String summary
) {
    public record BriefMetrics(
            int totalBriefs,
            int draftBriefs,
            int inReviewBriefs,
            int changesRequestedBriefs,
            int approvedBriefs
    ) {
    }

    public record ValidationMetrics(
            int totalRuns,
            int validRuns,
            int invalidRuns,
            int humanReviewRequiredRuns
    ) {
    }

    public record ExportMetrics(
            int totalTrackedExports,
            int githubTrackedExports,
            int jiraTrackedExports,
            int writebackAttempts,
            int successfulWritebacks,
            int blockedWritebacks,
            Instant latestRetentionUntil
    ) {
    }

    public record AuditEventSummary(
            String briefId,
            String eventType,
            String actorId,
            String actorRole,
            Instant occurredAt,
            String summary
    ) {
    }
}
