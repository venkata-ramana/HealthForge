package dev.healthforge.platform.explainer;

import jakarta.validation.constraints.NotBlank;

public record RegulationExplainerRequest(
        @NotBlank String corpusId,
        @NotBlank String corpusVersion,
        @NotBlank String sourceId,
        @NotBlank String question,
        String projectContext
) {
}
