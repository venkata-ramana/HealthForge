package dev.healthforge.platform.integration;

import java.time.Instant;
import java.util.List;

public record IntegrationStatusResponse(
        String organizationId,
        Instant generatedAt,
        List<ConnectorSummary> connectors,
        List<DeliveryReceipt> recentReceipts,
        List<RetryQueueItem> retryQueue,
        List<RecoveryAction> recoveryActions
) {
    public record ConnectorSummary(
            String connectorType,
            boolean enabled,
            String executionMode,
            boolean liveCapable,
            String baseUrl,
            String credentialReference,
            int successCount,
            int blockedCount,
            int retryCount,
            String operatorSummary
    ) {}

    public record DeliveryReceipt(
            String receiptId,
            String receiptType,
            String connectorType,
            String sourceId,
            String status,
            String targetLocator,
            String externalReference,
            Instant occurredAt
    ) {}

    public record RetryQueueItem(
            String sourceType,
            String sourceId,
            String connectorType,
            String currentStatus,
            String targetLocator,
            String retryHint
    ) {}

    public record RecoveryAction(
            String recoveryActionId,
            String sourceType,
            String sourceId,
            String connectorType,
            String previousStatus,
            String requestedAction,
            String outcomeStatus,
            String summary,
            Instant createdAt
    ) {}
}
