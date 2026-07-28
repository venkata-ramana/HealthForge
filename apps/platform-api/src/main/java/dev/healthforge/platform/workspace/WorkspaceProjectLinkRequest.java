package dev.healthforge.platform.workspace;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceProjectLinkRequest(
        @NotBlank String briefId
) {
}
