package dev.healthforge.platform.answer;

import java.time.Instant;
import java.util.List;

public record GroundedAnswerResponse(
        String answerId,
        String status,
        Instant createdAt,
        String question,
        String answer,
        List<EvidenceFinding> findings,
        List<String> limitations,
        String reviewNotice
) {
    public record EvidenceFinding(String findingId, String statement, Citation citation) {
    }

    public record Citation(
            String passageId,
            String sourceId,
            String sourceVersion,
            String sourceType,
            String title,
            String canonicalUrl,
            String locator,
            String support
    ) {
    }
}
