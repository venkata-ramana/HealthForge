package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record ControlledRolloutResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        String decision,
        int overallScore,
        List<PhaseSummary> phases,
        List<EvidenceItem> evidence,
        List<Gap> gaps,
        List<String> boundedStatements,
        String summary
) {
    public record PhaseSummary(
            String phaseId,
            String title,
            int score,
            String status,
            List<Check> checks
    ) {
    }

    public record Check(
            String checkId,
            String title,
            String status,
            String evidence,
            String ownerRole,
            String nextAction
    ) {
    }

    public record EvidenceItem(
            String evidenceId,
            String phaseId,
            String checkId,
            String status,
            String ownerRole,
            String evidenceSummary,
            String nextAction,
            String actorId,
            Instant updatedAt
    ) {
    }

    public record Gap(
            String phaseId,
            String checkId,
            String title,
            String ownerRole,
            String nextAction
    ) {
    }
}
