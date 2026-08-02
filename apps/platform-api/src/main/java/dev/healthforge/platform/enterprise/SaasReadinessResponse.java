package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record SaasReadinessResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        String posture,
        List<PhaseReadiness> phases,
        List<String> launchGates,
        List<String> limitations,
        String summary
) {
    public record PhaseReadiness(
            String phase,
            String title,
            String status,
            String deliveredOutcome,
            List<String> evidence,
            List<String> nextActions
    ) {
    }
}
