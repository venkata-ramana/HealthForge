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
        String reviewNotice,
        Diagnostics diagnostics
) {
    public GroundedAnswerResponse(
            String answerId,
            String status,
            Instant createdAt,
            String question,
            String answer,
            List<EvidenceFinding> findings,
            List<String> limitations,
            String reviewNotice
    ) {
        this(answerId, status, createdAt, question, answer, findings, limitations, reviewNotice, null);
    }

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
            String support,
            String freshnessStatus,
            long sourceAgeDays,
            String changeSummary
    ) {
        public Citation(
                String passageId,
                String sourceId,
                String sourceVersion,
                String sourceType,
                String title,
                String canonicalUrl,
                String locator,
                String support
        ) {
            this(passageId, sourceId, sourceVersion, sourceType, title, canonicalUrl, locator, support, "unknown", 0L, "No source-change summary is available.");
        }
    }

    public record Diagnostics(
            String sufficiency,
            int retrievalResultCount,
            List<String> reasons,
            List<String> queryRefinements,
            List<String> contextHints,
            String nextBestAction
    ) {
    }
}
