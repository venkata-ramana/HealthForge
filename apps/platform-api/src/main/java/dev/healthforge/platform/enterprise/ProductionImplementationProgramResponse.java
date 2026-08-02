package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record ProductionImplementationProgramResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        String posture,
        List<Phase> phases,
        List<String> implementationGates,
        List<String> limitations,
        String summary
) {
    public record Phase(
            String phase,
            String title,
            String status,
            String implementedContract,
            List<String> evidence,
            List<String> nextActions
    ) {
    }
}
