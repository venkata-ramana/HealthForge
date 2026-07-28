package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record OperationsAttestationResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        List<String> expectedAttestations,
        List<AttestationEvent> recentAttestations,
        String summary
) {
    public record AttestationEvent(
            String attestationId,
            String policyArea,
            String environmentName,
            String attestationType,
            String actorId,
            String actorRole,
            String changeSummary,
            List<String> controlIds,
            String acknowledgment,
            Instant createdAt
    ) {
    }
}
