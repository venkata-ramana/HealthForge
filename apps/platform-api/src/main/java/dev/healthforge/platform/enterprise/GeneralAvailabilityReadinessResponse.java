package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record GeneralAvailabilityReadinessResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        String posture,
        List<Phase> phases,
        List<String> gaGates,
        List<String> limitations,
        String summary
) {
    public record Phase(
            String phase,
            String title,
            String status,
            String outcome,
            List<String> currentEvidence,
            List<String> remainingActions
    ) {
    }
}
