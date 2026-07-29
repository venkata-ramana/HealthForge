package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record FutureControlRoadmapResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        List<CurrentStateItem> currentState,
        List<RoadmapTrack> roadmapTracks,
        List<String> discussionGuardrails,
        String summary
) {
    public record CurrentStateItem(
            String area,
            String currentState,
            String targetState
    ) {
    }

    public record RoadmapTrack(
            String track,
            String focus,
            List<String> nearTermAssets,
            List<String> futureOutcomes
    ) {
    }
}
