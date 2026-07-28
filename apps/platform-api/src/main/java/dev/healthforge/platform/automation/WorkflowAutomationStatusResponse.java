package dev.healthforge.platform.automation;

import java.time.Instant;
import java.util.List;

public record WorkflowAutomationStatusResponse(
        String organizationId,
        List<RecentEvent> recentEvents,
        List<RecentDelivery> recentDeliveries
) {
    public record RecentEvent(
            String workflowEventId,
            String briefId,
            String eventFamily,
            String eventName,
            String environmentScope,
            Instant occurredAt,
            String payloadSummary
    ) {
    }

    public record RecentDelivery(
            String deliveryId,
            String workflowEventId,
            String targetLabel,
            String deliveryMode,
            String deliveryStatus,
            int retryCount,
            Instant attemptedAt,
            String externalReference
    ) {
    }
}
