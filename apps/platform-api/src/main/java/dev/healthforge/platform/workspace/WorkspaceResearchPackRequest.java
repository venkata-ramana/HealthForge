package dev.healthforge.platform.workspace;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceResearchPackRequest(
        String projectId,
        @NotBlank String name,
        @NotBlank String summary,
        @NotBlank String recurringQuestions,
        String nextReviewDate
) {
}
