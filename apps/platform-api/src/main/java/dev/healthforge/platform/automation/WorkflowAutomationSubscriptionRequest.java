package dev.healthforge.platform.automation;

import jakarta.validation.constraints.NotBlank;

public record WorkflowAutomationSubscriptionRequest(
        @NotBlank String eventFamily,
        @NotBlank String eventName,
        @NotBlank String environmentScope,
        @NotBlank String targetLabel,
        @NotBlank String deliveryMode,
        boolean enabled
) {
}
