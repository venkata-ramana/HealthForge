package dev.healthforge.platform.copilot;

import jakarta.validation.constraints.NotBlank;

public record PriorAuthCopilotRequest(
        @NotBlank String corpusId,
        @NotBlank String corpusVersion,
        @NotBlank String question,
        String projectContext,
        String scenarioHint
) {
}
