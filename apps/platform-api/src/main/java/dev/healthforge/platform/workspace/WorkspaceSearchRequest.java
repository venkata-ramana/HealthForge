package dev.healthforge.platform.workspace;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceSearchRequest(
        @NotBlank String query,
        String facet
) {
}
