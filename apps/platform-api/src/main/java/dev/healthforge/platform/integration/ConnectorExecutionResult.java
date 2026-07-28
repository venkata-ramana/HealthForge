package dev.healthforge.platform.integration;

public record ConnectorExecutionResult(
        String connectorType,
        String status,
        String resultSummary,
        String targetLocator,
        String externalReference,
        boolean simulated,
        String receiptType
) {
}
