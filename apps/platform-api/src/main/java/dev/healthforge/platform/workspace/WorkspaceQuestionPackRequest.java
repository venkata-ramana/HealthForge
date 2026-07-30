package dev.healthforge.platform.workspace;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceQuestionPackRequest(
        String projectId,
        @NotBlank String name,
        @NotBlank String summary,
        @NotBlank String persona,
        @NotBlank String templateKind,
        @NotBlank String starterQuestion,
        @NotBlank String questionPrompts
) {
}
