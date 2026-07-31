package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record PilotAnalyticsResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        FunnelSummary funnel,
        OutcomeSummary outcomes,
        List<RoleActivity> roleActivity,
        StakeholderSummary stakeholderSummary,
        FeedbackSummary feedback,
        ExpansionReadiness expansionReadiness,
        List<String> boundedStatements,
        String summary
) {
    public record FunnelSummary(
            int questionStarts,
            int briefCreated,
            int briefsReviewed,
            int briefsApproved,
            int implementationHandoffs,
            int questionToBriefRate,
            int briefToApprovalRate,
            int approvalToHandoffRate,
            List<FunnelStage> stages
    ) {
    }

    public record FunnelStage(
            String stage,
            int count,
            int dropOffFromPrevious,
            String interpretation
    ) {
    }

    public record OutcomeSummary(
            String primaryOutcome,
            String outcomeBoundary,
            List<OutcomeSignal> signals
    ) {
    }

    public record OutcomeSignal(
            String name,
            int value,
            String unit,
            String explanation
    ) {
    }

    public record RoleActivity(
            String role,
            int auditEvents,
            int feedbackRecords,
            String activitySignal
    ) {
    }

    public record StakeholderSummary(
            String headline,
            List<StakeholderMetric> metrics,
            List<String> valueEvidence,
            List<String> sponsorQuestions
    ) {
    }

    public record StakeholderMetric(
            String name,
            String value,
            String audience,
            String meaning
    ) {
    }

    public record FeedbackSummary(
            int totalRecords,
            List<FeedbackTypeSummary> byType,
            List<FeedbackRecord> recent
    ) {
    }

    public record FeedbackTypeSummary(
            String feedbackType,
            int records,
            double averageRating
    ) {
    }

    public record FeedbackRecord(
            String feedbackId,
            String feedbackType,
            int rating,
            String actorRole,
            String briefId,
            String findingId,
            String note,
            Instant createdAt
    ) {
    }

    public record ExpansionReadiness(
            String currentStage,
            int score,
            String nextStage,
            List<ReadinessCheck> checks,
            List<String> gaps
    ) {
    }

    public record ReadinessCheck(
            String checkId,
            String title,
            String status,
            String evidence
    ) {
    }
}
