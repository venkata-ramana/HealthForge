package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record StakeholderReportingResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        ExecutiveSummary executiveSummary,
        DeliverySummary deliverySummary,
        TrustSummary trustSummary,
        List<ReportingArtifact> reportingArtifacts,
        String summary
) {
    public record ExecutiveSummary(
            int totalBriefs,
            int approvedBriefs,
            int activeProjects,
            int blockedDeliveries,
            String qualityGateDecision
    ) {
    }

    public record DeliverySummary(
            int trackedExports,
            int inboundCases,
            int assignments,
            int changesRequestedBriefs
    ) {
    }

    public record TrustSummary(
            int insufficientEvidenceAnswers,
            int unsupportedTriggeredAnswers,
            int reviewDisagreements,
            int attestations
    ) {
    }

    public record ReportingArtifact(
            String title,
            String audience,
            String sourceView,
            String useCase
    ) {
    }
}
