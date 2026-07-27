package dev.healthforge.platform.bundlereview;

import dev.healthforge.platform.answer.GroundedAnswerResponse;
import dev.healthforge.platform.fhir.FhirValidationResponse;
import dev.healthforge.platform.journey.PriorAuthJourneyResponse;

import java.time.Instant;
import java.util.List;

public record PriorAuthBundleReviewResponse(
        String reviewId,
        String status,
        Instant createdAt,
        Input input,
        SyntheticScenario scenario,
        String summary,
        BundleInventory bundleInventory,
        WorkflowContext workflowContext,
        List<ScenarioFinding> scenarioFindings,
        FhirValidationResponse validation,
        List<GroundedAnswerResponse.EvidenceFinding> evidenceFindings,
        List<String> reviewerWarnings,
        List<String> nextActions,
        boolean requiresHumanReview,
        String reviewNotice
) {
    public record Input(
            String corpusId,
            String corpusVersion,
            String question,
            String projectContext
    ) {
    }

    public record SyntheticScenario(
            String scenarioId,
            String title,
            String description,
            String expectedStatus
    ) {
    }

    public record BundleInventory(
            String bundleId,
            String bundleType,
            int entryCount,
            List<String> resourceTypes,
            List<String> referencesObserved
    ) {
    }

    public record WorkflowContext(
            String journeyType,
            String primaryPersona,
            List<PriorAuthJourneyResponse.WorkflowStage> workflowStages,
            List<PriorAuthJourneyResponse.StateTransition> stateTransitions,
            List<PriorAuthJourneyResponse.Responsibility> responsibilities,
            List<PriorAuthJourneyResponse.StandardsTouchpoint> standardsTouchpoints
    ) {
    }

    public record ScenarioFinding(
            String findingType,
            String severity,
            String title,
            String detail,
            List<String> evidenceLinks
    ) {
    }
}
