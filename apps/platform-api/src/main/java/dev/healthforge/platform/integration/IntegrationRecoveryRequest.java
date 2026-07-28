package dev.healthforge.platform.integration;

import jakarta.validation.constraints.NotBlank;

public record IntegrationRecoveryRequest(
        @NotBlank String sourceType,
        @NotBlank String sourceId,
        @NotBlank String connectorType,
        @NotBlank String requestedAction
) {
}
