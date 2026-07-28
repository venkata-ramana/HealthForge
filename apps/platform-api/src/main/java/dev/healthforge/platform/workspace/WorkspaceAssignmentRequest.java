package dev.healthforge.platform.workspace;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceAssignmentRequest(
        @NotBlank String briefId,
        @NotBlank String assigneeActorId,
        @NotBlank String assigneeRole,
        @NotBlank String queueName,
        @NotBlank String handoffSummary
) {
}
