package dev.healthforge.platform.evaluation;

import java.time.Instant;
import java.util.List;

public record PolicySafetyReportResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        String policyVersion,
        UnsupportedOutputSummary unsupportedOutputSummary,
        ApprovalPolicySummary approvalPolicySummary,
        IntegrationPolicySummary integrationPolicySummary,
        List<String> enabledControls,
        List<String> knownLimitations,
        String summary
) {
    public record UnsupportedOutputSummary(
            int totalAnswers,
            int insufficientEvidenceAnswers,
            int unsupportedTriggeredAnswers,
            double insufficientEvidenceRate,
            Instant latestObservedAt
    ) {
    }

    public record ApprovalPolicySummary(
            int approvedBriefs,
            int changesRequestedBriefs,
            int disagreementFindings,
            boolean approvalRequiredForExports,
            String interpretation
    ) {
    }

    public record IntegrationPolicySummary(
            int blockedTrackerWritebacks,
            int blockedCollaborationSends,
            int blockedDocumentationPublishes,
            int previewOnlyWebhookDeliveries,
            int governedDeliveries,
            String interpretation
    ) {
    }
}
