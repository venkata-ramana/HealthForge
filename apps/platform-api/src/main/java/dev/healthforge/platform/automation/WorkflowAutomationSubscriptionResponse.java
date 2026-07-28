package dev.healthforge.platform.automation;

import java.time.Instant;

public record WorkflowAutomationSubscriptionResponse(
        String subscriptionId,
        String organizationId,
        String eventFamily,
        String eventName,
        String environmentScope,
        String targetLabel,
        String deliveryMode,
        boolean enabled,
        Instant updatedAt
) {
}
