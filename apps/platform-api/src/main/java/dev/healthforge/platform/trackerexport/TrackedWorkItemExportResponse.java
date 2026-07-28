package dev.healthforge.platform.trackerexport;

import java.time.Instant;
import java.util.List;

public record TrackedWorkItemExportResponse(
        String exportId,
        String briefId,
        String targetSystem,
        String mode,
        Instant createdAt,
        String requestedBy,
        String requestedRole,
        List<PreviewItem> items,
        List<String> nonGoals,
        boolean writebackEnabled,
        ApprovalGate approvalGate,
        WritebackExecution writebackExecution,
        String reviewNotice
) {
    public record PreviewItem(
            String workItemId,
            String title,
            String providerType,
            List<String> labels,
            String body,
            JiraShape jira,
            GitHubShape github
    ) {
    }

    public record GitHubShape(
            String title,
            String body,
            List<String> labels
    ) {
    }

    public record JiraShape(
            String summary,
            String description,
            String issueType,
            List<String> labels
    ) {
    }

    public record ApprovalGate(
            String approvalId,
            String approvedBy,
            String approvedRole,
            Instant approvedAt
    ) {
    }

    public record WritebackExecution(
            String executionStatus,
            String executionResult,
            String targetLocator,
            String externalReference,
            int retryCount,
            String retriedFromExportId,
            Instant executedAt,
            boolean simulated,
            String receiptType
    ) {
    }
}
