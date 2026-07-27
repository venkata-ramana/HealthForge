package dev.healthforge.platform.crosswalk;

import dev.healthforge.platform.answer.GroundedAnswerResponse;

import java.time.Instant;
import java.util.List;

public record PriorAuthStandardsCrosswalkResponse(
        String crosswalkId,
        String status,
        Instant createdAt,
        Input input,
        String journeyType,
        String primaryPersona,
        String summary,
        List<RequirementCrosswalk> requirementCrosswalks,
        List<ArtifactSummary> artifactSummaries,
        List<String> reviewerWarnings,
        List<String> nextActions,
        List<GroundedAnswerResponse.EvidenceFinding> evidenceFindings,
        boolean requiresHumanReview,
        String reviewNotice
) {
    public record Input(
            String corpusId,
            String corpusVersion,
            String question,
            String projectContext,
            String scenarioHint
    ) {
    }

    public record RequirementCrosswalk(
            String requirementId,
            String policyStatement,
            String workflowStage,
            String policyFocus,
            List<String> fhirResources,
            List<String> operations,
            List<String> guides,
            List<ArtifactLink> artifacts,
            List<String> technicalImplications
    ) {
    }

    public record ArtifactLink(
            String artifactId,
            String title,
            String artifactType,
            String canonicalUrl,
            String reason
    ) {
    }

    public record ArtifactSummary(
            String artifactId,
            String title,
            String artifactType,
            String canonicalUrl,
            List<String> usedFor
    ) {
    }
}
