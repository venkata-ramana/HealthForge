package dev.healthforge.platform.integration;

import jakarta.validation.constraints.NotBlank;

public record IntegrationGovernanceCheckRequest(
        @NotBlank String connectorType,
        @NotBlank String actionType,
        String briefId,
        String approvalId,
        String targetLocator,
        String requestedExecutionMode
) {
}
