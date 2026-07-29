package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record SolutionPacksResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        List<SolutionPack> packs,
        String summary
) {
    public record SolutionPack(
            String audience,
            String positioning,
            List<String> workflows,
            List<String> demoAngles,
            List<String> trustAngles,
            List<String> deliveryArtifacts
    ) {
    }
}
