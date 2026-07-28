package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record OperationsObservabilityResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        HealthSignals healthSignals,
        RetentionSignals retentionSignals,
        List<String> incidentRunbooks,
        List<String> operatorNarratives,
        String summary
) {
    public record HealthSignals(
            int briefsLast30Days,
            int answersLast30Days,
            int invalidValidationsLast30Days,
            int blockedDeliveries,
            int changesRequestedBriefs,
            Instant latestAuditEventAt
    ) {
    }

    public record RetentionSignals(
            int trackedExportRetentionDays,
            String validationTelemetryRetention,
            String auditEvidenceRetention,
            int attestationEvents,
            Instant latestAttestationAt
    ) {
    }
}
