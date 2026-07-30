package dev.healthforge.platform.implementation;

import dev.healthforge.platform.architecture.ArchitectureReviewResponse;
import dev.healthforge.platform.brief.BriefWorkItemExportResponse;
import dev.healthforge.platform.codegen.StarterCodeGenerationResponse;

import java.time.Instant;
import java.util.List;

public record ImplementationBundleResponse(
        String briefId,
        String organizationId,
        String generatedBy,
        String actorRole,
        Instant generatedAt,
        HandoffSummary handoffSummary,
        List<ArchitecturePattern> architecturePatterns,
        TestPlan testPlan,
        ChangeImpact changeImpact,
        HandoffBundle handoffBundle,
        ReleaseBundle releaseBundle,
        List<StarterCodeGenerationResponse> starterArtifacts,
        List<String> deliveryGuardrails
) {
    public record HandoffSummary(
            String briefQuestion,
            String implementationStatus,
            int workItemCount,
            int implementationTrackCount,
            String summary
    ) {
    }

    public record ArchitecturePattern(
            String title,
            String workflow,
            String rationale,
            List<String> implementationNotes,
            List<String> tradeoffs
    ) {
    }

    public record TestPlan(
            List<AcceptanceCriterion> acceptanceCriteria,
            List<ValidationScenario> validationScenarios,
            List<ValidationScenario> negativeCases,
            List<TraceabilityLink> traceabilityLinks
    ) {
    }

    public record AcceptanceCriterion(
            String criterionId,
            String title,
            String expectedOutcome,
            String ownerFocus
    ) {
    }

    public record ValidationScenario(
            String scenarioId,
            String title,
            String scenarioType,
            String expectation
    ) {
    }

    public record TraceabilityLink(
            String findingId,
            String workItemId,
            String testFocus
    ) {
    }

    public record ChangeImpact(
            List<SourceChangeSignal> sourceChangeSignals,
            List<String> reReviewRecommendations,
            String maintenanceSummary
    ) {
    }

    public record SourceChangeSignal(
            String sourceId,
            String briefSourceVersion,
            String latestKnownVersion,
            String changeStatus,
            String actionHint
    ) {
    }

    public record HandoffBundle(
            BriefWorkItemExportResponse workItemExport,
            ArchitectureReviewResponse architectureReview,
            List<String> implementationSummary,
            List<String> deliveryArtifacts
    ) {
    }

    public record ReleaseBundle(
            String releaseStatus,
            List<ArtifactGroup> artifactGroups,
            List<DownstreamPackage> downstreamPackages,
            List<String> traceabilitySummary,
            String operatorHandoffSummary
    ) {
    }

    public record ArtifactGroup(
            String title,
            String audience,
            List<String> contents
    ) {
    }

    public record DownstreamPackage(
            String packageId,
            String targetAudience,
            String packageFormat,
            List<String> includedArtifacts,
            String handoffIntent
    ) {
    }
}
