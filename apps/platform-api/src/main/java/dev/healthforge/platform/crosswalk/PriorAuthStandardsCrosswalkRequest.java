package dev.healthforge.platform.crosswalk;

import jakarta.validation.constraints.NotBlank;

public record PriorAuthStandardsCrosswalkRequest(
        @NotBlank String corpusId,
        @NotBlank String corpusVersion,
        @NotBlank String question,
        String projectContext,
        String scenarioHint
) {
}
