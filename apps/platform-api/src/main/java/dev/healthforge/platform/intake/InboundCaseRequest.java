package dev.healthforge.platform.intake;

import jakarta.validation.constraints.NotBlank;

public record InboundCaseRequest(
        @NotBlank String sourceSystem,
        @NotBlank String externalCaseId,
        @NotBlank String title,
        @NotBlank String summary,
        @NotBlank String requestedRole,
        String requestedAssignee,
        String sourceLocator,
        boolean createBrief,
        String corpusId,
        String corpusVersion,
        String briefQuestion,
        String projectContext
) {
}
