package dev.healthforge.platform.enterprise;

import jakarta.validation.constraints.NotBlank;

public record PilotSuccessCheckpointRequest(
        @NotBlank String milestoneName,
        @NotBlank String ownerRole,
        @NotBlank String targetOutcome,
        @NotBlank String status,
        @NotBlank String note
) {
}
