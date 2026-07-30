package dev.healthforge.platform.integration;

import java.time.Instant;
import java.util.List;

public record IntegrationStatusResponse(
        String organizationId,
        Instant generatedAt,
        List<ConnectorSummary> connectors,
        List<DeliveryReceipt> recentReceipts,
        List<RetryQueueItem> retryQueue,
        List<RecoveryAction> recoveryActions,
        ReconciliationSummary reconciliationSummary,
        List<ConnectorDrilldown> connectorDrilldowns,
        List<EnvironmentPolicySummary> environmentPolicies
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

    public record ReconciliationSummary(
            int totalReceipts,
            int successfulReceipts,
            int blockedReceipts,
            int retryingReceipts,
            int simulatedReceipts,
            int liveReceipts,
            String operatorSummary
    ) {}

    public record ConnectorDrilldown(
            String connectorType,
            String deliveryMode,
            boolean liveCapable,
            boolean enabled,
            String approvalGate,
            String policySummary,
            List<String> recentStatuses,
            List<String> recommendedActions
    ) {}

    public record EnvironmentPolicySummary(
            String connectorType,
            String executionMode,
            boolean liveCallsAllowed,
            String environmentPosture,
            List<String> operatorChecks
    ) {}
}
