package dev.healthforge.platform.automation;

import jakarta.validation.constraints.NotBlank;

public record WorkflowAutomationDispatchRequest(
        String briefId,
        @NotBlank String eventFamily,
        @NotBlank String eventName,
        @NotBlank String payloadSummary,
        @NotBlank String environmentScope,
        boolean webhookRequested,
        String retryFromDeliveryId
) {
}
