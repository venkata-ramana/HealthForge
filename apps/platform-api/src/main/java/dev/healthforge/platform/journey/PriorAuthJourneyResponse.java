package dev.healthforge.platform.journey;

import dev.healthforge.platform.answer.GroundedAnswerResponse;

import java.time.Instant;
import java.util.List;

public record PriorAuthJourneyResponse(
        String journeyId,
        String status,
        Instant createdAt,
        Input input,
        String journeyType,
        String primaryPersona,
        String summary,
        List<WorkflowStage> workflowStages,
        List<StateTransition> stateTransitions,
        List<Responsibility> responsibilities,
        List<StandardsTouchpoint> standardsTouchpoints,
        List<String> reviewerWarnings,
        List<String> nextActions,
        List<GroundedAnswerResponse.EvidenceFinding> findings,
        boolean requiresHumanReview,
        String reviewNotice
) {
    public record Input(
            String corpusId,
            String corpusVersion,
            String question,
            String projectContext,
            String scenarioHint
    ) {
    }

    public record WorkflowStage(
            String stageId,
            String title,
            String objective,
            List<String> candidateStandards,
            List<String> expectedOutputs
    ) {
    }

    public record StateTransition(
            String fromStage,
            String toStage,
            String trigger,
            String transitionNote
    ) {
    }

    public record Responsibility(
            String actor,
            String systemRole,
            String responsibility,
            String stageId
    ) {
    }

    public record StandardsTouchpoint(
            String stageId,
            String artifactId,
            String title,
            String artifactType,
            String canonicalUrl,
            String reason
    ) {
    }
}
