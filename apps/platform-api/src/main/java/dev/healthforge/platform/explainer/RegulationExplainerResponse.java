package dev.healthforge.platform.explainer;

import java.time.Instant;
import java.util.List;

public record RegulationExplainerResponse(
        String explainerId,
        String status,
        Instant createdAt,
        Input input,
        Source source,
        String plainEnglishSummary,
        List<String> technicalImplications,
        List<String> assumptions,
        List<String> unresolvedQuestions,
        List<Finding> findings,
        List<String> followOnWorkflows,
        boolean requiresHumanReview,
        String reviewNotice
) {
    public record Input(
            String corpusId,
            String corpusVersion,
            String sourceId,
            String question,
            String projectContext
    ) {
    }

    public record Source(
            String sourceId,
            String sourceVersion,
            String sourceType,
            String title,
            String canonicalUrl
    ) {
    }

    public record Finding(
            String findingId,
            String statement,
            Citation citation
    ) {
    }

    public record Citation(
            String passageId,
            String sourceId,
            String sourceVersion,
            String sourceType,
            String title,
            String canonicalUrl,
            String locator,
            String support
    ) {
    }
}
