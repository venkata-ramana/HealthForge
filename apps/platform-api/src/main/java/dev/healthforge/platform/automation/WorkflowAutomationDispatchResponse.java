package dev.healthforge.platform.automation;

import java.time.Instant;
import java.util.List;

public record WorkflowAutomationDispatchResponse(
        String workflowEventId,
        String organizationId,
        String eventFamily,
        String eventName,
        String environmentScope,
        Instant occurredAt,
        List<Delivery> deliveries,
        String summary
) {
    public record Delivery(
            String deliveryId,
            String targetLabel,
            String deliveryMode,
            String deliveryStatus,
            int retryCount,
            String responseSummary,
            String externalReference,
            Instant attemptedAt
    ) {
    }
}
