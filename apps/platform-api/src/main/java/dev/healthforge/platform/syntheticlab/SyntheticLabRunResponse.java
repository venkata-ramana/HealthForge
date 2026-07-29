package dev.healthforge.platform.syntheticlab;

import dev.healthforge.platform.bundlereview.PriorAuthBundleReviewResponse;
import dev.healthforge.platform.journey.PriorAuthJourneyResponse;

import java.time.Instant;
import java.util.List;

public record SyntheticLabRunResponse(
        String runId,
        String templateId,
        String title,
        Instant generatedAt,
        String summary,
        PriorAuthBundleReviewResponse bundleReview,
        PriorAuthJourneyResponse journey,
        List<Assertion> assertions,
        List<String> expectedOutcomes,
        List<TimelineEvent> timeline,
        ReplayMetadata replayMetadata
) {
    public record Assertion(
            String assertionId,
            String title,
            String status,
            String detail
    ) {
    }

    public record TimelineEvent(
            String stageId,
            String title,
            String ownerActor,
            String expectedOutput
    ) {
    }

    public record ReplayMetadata(
            String bundleType,
            List<String> resourceTypes,
            String comparisonHint
    ) {
    }
}
