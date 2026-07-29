package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record PilotSuccessResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        AdoptionSignals adoptionSignals,
        List<Checkpoint> checkpoints,
        List<String> followUpPatterns,
        String summary
) {
    public record AdoptionSignals(
            int activeProjects,
            int reviewerAssignments,
            int approvedBriefs,
            int trackedExports,
            int inboundCases
    ) {
    }

    public record Checkpoint(
            String checkpointId,
            String milestoneName,
            String ownerRole,
            String targetOutcome,
            String status,
            String note,
            Instant updatedAt
    ) {
    }
}
