package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record ProductionReadinessResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        String decision,
        int overallScore,
        List<PhaseReadiness> phases,
        List<ReadinessGap> gaps,
        List<String> boundedStatements,
        String summary
) {
    public record PhaseReadiness(
            String phaseId,
            String title,
            int score,
            String status,
            List<ReadinessCheck> checks
    ) {
    }

    public record ReadinessCheck(
            String checkId,
            String title,
            String status,
            String evidence,
            String ownerRole
    ) {
    }

    public record ReadinessGap(
            String phaseId,
            String title,
            String ownerRole,
            String nextAction
    ) {
    }
}
