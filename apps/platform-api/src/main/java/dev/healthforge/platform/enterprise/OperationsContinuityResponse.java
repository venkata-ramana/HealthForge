package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record OperationsContinuityResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        ContinuityInventory continuityInventory,
        List<String> backupGuidance,
        List<String> restoreChecks,
        List<String> migrationChecks,
        List<String> recoveryRehearsals,
        String summary
) {
    public record ContinuityInventory(
            int totalBriefs,
            int totalAuditEvents,
            int totalTrackedExports,
            int totalValidationRuns,
            String latestFlywayVersion,
            Instant latestBriefCreatedAt
    ) {
    }
}
