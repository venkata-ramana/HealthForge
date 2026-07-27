package dev.healthforge.platform.bundlereview;

import jakarta.validation.constraints.NotBlank;

public record PriorAuthBundleReviewRequest(
        @NotBlank String corpusId,
        @NotBlank String corpusVersion,
        @NotBlank String question,
        String projectContext,
        String scenarioId
) {
}
