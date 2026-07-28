package dev.healthforge.platform.workspace;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceSavedViewRequest(
        String projectId,
        @NotBlank String viewType,
        @NotBlank String name,
        @NotBlank String queryText,
        @NotBlank String summary
) {
}
