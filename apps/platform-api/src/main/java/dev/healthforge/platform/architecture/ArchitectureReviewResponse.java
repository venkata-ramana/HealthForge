package dev.healthforge.platform.architecture;

import dev.healthforge.platform.answer.GroundedAnswerResponse;

import java.time.Instant;
import java.util.List;

public record ArchitectureReviewResponse(
        String reviewId,
        String status,
        Instant createdAt,
        Input input,
        String summary,
        List<ComponentRecommendation> components,
        List<IntegrationRecommendation> integrations,
        List<String> standardsTouchpoints,
        List<Assumption> assumptions,
        List<Risk> risks,
        List<ReviewCheckpoint> reviewCheckpoints,
        List<GroundedAnswerResponse.EvidenceFinding> evidenceFindings,
        boolean humanReviewRequired,
        String reviewNotice
) {
    public record Input(String corpusId, String corpusVersion, String question, String projectContext) {}

    public record ComponentRecommendation(String name, String role, String rationale) {}

    public record IntegrationRecommendation(String system, String interaction, String rationale) {}

    public record Assumption(String statement, String validationNeeded) {}

    public record Risk(String name, String whyItMatters, String mitigationFocus) {}

    public record ReviewCheckpoint(String checkpoint, String reason) {}
}
