package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record PilotReadinessResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        ReadinessSummary readinessSummary,
        List<ChecklistItem> checklist,
        List<ArtifactSummary> artifacts,
        List<String> boundedStatements,
        String summary
) {
    public record ReadinessSummary(
            String readinessTier,
            int completedChecks,
            int totalChecks,
            boolean pilotReadyForPrivateEvaluation
    ) {
    }

    public record ChecklistItem(
            String itemId,
            String title,
            String status,
            String ownerRole,
            String rationale
    ) {
    }

    public record ArtifactSummary(
            String title,
            String artifactType,
            String description,
            String intendedAudience
    ) {
    }
}
