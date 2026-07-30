package dev.healthforge.platform.workspace;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceResearchNotebookRequest(
        String projectId,
        String briefId,
        @NotBlank String title,
        @NotBlank String summary,
        @NotBlank String keyTakeaways,
        @NotBlank String evidenceBundleName,
        @NotBlank String handoffSummary,
        @NotBlank String continuityNote
) {
}
