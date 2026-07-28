package dev.healthforge.platform.intelligence;

import java.time.Instant;

public record RetrievalFeedbackResponse(
        String feedbackId,
        String organizationId,
        String briefId,
        String findingId,
        String feedbackType,
        String sourceId,
        String actorId,
        String actorRole,
        String note,
        Instant createdAt
) {
}
