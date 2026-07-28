package dev.healthforge.platform.evaluation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EvaluationDashboardResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        QualityGate qualityGate,
        SourceHealth sourceHealth,
        EvidenceHealth evidenceHealth,
        ReviewQuality reviewQuality,
        WorkflowQuality workflowQuality,
        List<String> guidance,
        String summary
) {
    public record QualityGate(
            String gateId,
            LocalDate approvedOn,
            String baselineReport,
            String candidateReport,
            Instant candidateGeneratedAt,
            double retrievalRecall,
            double citationCoverageRate,
            double unsupportedAnswerPassRate,
            String decision,
            List<String> regressionSignals,
            List<FailureCase> highlightedFailures
    ) {
    }

    public record FailureCase(
            String caseId,
            String category,
            String severity,
            boolean retrievalHit,
            String answerStatus
    ) {
    }

    public record SourceHealth(
            int totalSources,
            int activeSources,
            int staleSourcesOlderThan30Days,
            int supersededSources,
            int snapshotCount,
            double averageSourceAgeDays,
            Instant latestSnapshotCreatedAt
    ) {
    }

    public record EvidenceHealth(
            int totalAnswers,
            int insufficientEvidenceAnswers,
            int unsupportedTriggeredAnswers,
            double averageRetrievalResults,
            int approvedBriefs,
            int evidenceBearingBriefs,
            double evidenceBearingBriefRate
    ) {
    }

    public record ReviewQuality(
            int totalReviewDecisions,
            int disagreementFindings,
            int sourceConsistencyAlerts,
            int correctedStatements,
            int approvals,
            int changesRequestedBriefs,
            double approvalRate,
            String advisoryNotice
    ) {
    }

    public record WorkflowQuality(
            int workflowEvents,
            int blockedIntegrationActions,
            int governedDeliveries,
            int previewOnlyDeliveries,
            int recentBriefApprovals,
            int recentChangeRequests,
            String summary
    ) {
    }
}
