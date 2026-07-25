package dev.healthforge.platform.copilot;

import dev.healthforge.platform.answer.GroundedAnswerResponse;

import java.time.Instant;
import java.util.List;

public record PriorAuthCopilotResponse(
        String copilotId,
        String status,
        Instant createdAt,
        Input input,
        String scenarioType,
        String workflowStage,
        List<String> personas,
        String summary,
        List<Touchpoint> standardsTouchpoints,
        List<String> reviewerWarnings,
        List<String> assumptions,
        List<String> nextActions,
        List<GroundedAnswerResponse.EvidenceFinding> findings,
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

    public record Touchpoint(
            String artifactId,
            String title,
            String artifactType,
            String canonicalUrl,
            String reason
    ) {
    }
}
