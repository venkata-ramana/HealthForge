package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record EnterpriseLaunchProgramResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        String programStatus,
        List<Phase> phases,
        List<String> goNoGoGates,
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
