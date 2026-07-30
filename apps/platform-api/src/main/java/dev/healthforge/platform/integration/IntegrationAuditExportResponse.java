package dev.healthforge.platform.integration;

import java.time.Instant;
import java.util.List;

public record IntegrationAuditExportResponse(
        String organizationId,
        Instant generatedAt,
        IntegrationStatusResponse.ReconciliationSummary reconciliationSummary,
        List<IntegrationStatusResponse.ConnectorSummary> connectors,
        List<IntegrationStatusResponse.ConnectorDrilldown> connectorDrilldowns,
        List<IntegrationStatusResponse.EnvironmentPolicySummary> environmentPolicies,
        List<IntegrationStatusResponse.DeliveryReceipt> recentReceipts,
        List<IntegrationStatusResponse.RetryQueueItem> retryQueue,
        List<IntegrationStatusResponse.RecoveryAction> recoveryActions,
        List<String> auditNotes
) {
}
