package dev.healthforge.platform.enterprise;

import jakarta.validation.constraints.NotBlank;

public record ControlledRolloutEvidenceRequest(
        @NotBlank String phaseId,
        @NotBlank String checkId,
        @NotBlank String status,
        @NotBlank String ownerRole,
        @NotBlank String evidenceSummary,
        @NotBlank String nextAction
) {
}
