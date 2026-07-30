package dev.healthforge.platform.workspace;

import jakarta.validation.constraints.NotBlank;

public record WorkspaceReviewEscalationRequest(
        String assignmentId,
        @NotBlank String briefId,
        @NotBlank String escalationReason,
        @NotBlank String urgency,
        @NotBlank String destinationQueue,
        @NotBlank String note
) {
}
