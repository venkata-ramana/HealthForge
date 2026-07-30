package dev.healthforge.platform.integration;

import java.time.Instant;
import java.util.List;

public record IntegrationGovernanceCheckResponse(
        String organizationId,
        Instant generatedAt,
        String connectorType,
        String actionType,
        String requestedExecutionMode,
        String finalDecision,
        boolean liveExecutionAllowed,
        ApprovalGateSummary approvalGate,
        EnvironmentPolicySummary environmentPolicy,
        List<String> operatorActions,
        List<String> safeguards
) {
    public record ApprovalGateSummary(
            String status,
            String approvalId,
            String approvedBy,
            String approvedRole,
            Instant approvedAt,
            String summary
    ) {}

    public record EnvironmentPolicySummary(
            boolean connectorEnabled,
            String configuredExecutionMode,
            boolean allowLiveCalls,
            String targetLocator,
            String postureSummary
    ) {}
}
