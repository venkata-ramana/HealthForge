package dev.healthforge.platform.intelligence;

import java.time.Instant;
import java.util.List;

public record IntelligenceOverviewResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        List<RetrievalImprovement> retrievalImprovements,
        List<EvidenceGap> evidenceGaps,
        List<SimilarityCluster> similarityClusters,
        List<PersonaRecommendation> personaRecommendations,
        List<WorkflowTuningRecommendation> workflowTuningRecommendations,
        List<String> guardrails,
        String summary
) {
    public record RetrievalImprovement(
            String title,
            String priority,
            String rationale,
            List<String> evidence
    ) {}

    public record EvidenceGap(
            String gapId,
            String severity,
            String summary,
            List<String> suggestedSources,
            List<String> evidence
    ) {}

    public record SimilarityCluster(
            String clusterId,
            String theme,
            int artifactCount,
            List<String> briefIds,
            List<String> notes
    ) {}

    public record PersonaRecommendation(
            String recommendationId,
            String persona,
            String workflowStage,
            String nextAction,
            String explanation
    ) {}

    public record WorkflowTuningRecommendation(
            String recommendationId,
            String priority,
            String title,
            String summary,
            List<String> supportingSignals
    ) {}
}
