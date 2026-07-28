package dev.healthforge.platform.workspace;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceProjectRequest(
        @NotBlank String name,
        @NotBlank String kind,
        @NotBlank String description,
        String tags
) {
}
