package dev.healthforge.platform.developer;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record DeveloperRepoGuidanceRequest(
        @NotBlank String briefId,
        @NotBlank String repositoryName,
        @NotBlank String workspaceRoot,
        List<String> repositoryInventory,
        List<String> changedFiles
) {
}
