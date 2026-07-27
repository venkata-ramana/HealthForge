package dev.healthforge.platform.journey;

import jakarta.validation.constraints.NotBlank;

public record PriorAuthJourneyRequest(
        @NotBlank String corpusId,
        @NotBlank String corpusVersion,
        @NotBlank String question,
        String projectContext,
        String scenarioHint
) {
}
