package dev.healthforge.platform.retrieval;

import java.time.Instant;
import java.util.List;

public record RetrievalResponse(
        String corpusId,
        String corpusVersion,
        String indexGenerationId,
        List<RetrievalResult> results
) {
    public record RetrievalResult(
            String passageId,
            String excerpt,
            int rank,
            double score,
            CiteableSource source
    ) {
    }

    public record CiteableSource(
            String sourceId,
            String sourceVersion,
            String sourceType,
            String title,
            String canonicalUrl,
            String locator,
            Instant retrievedAt,
            String lifecycleStatus,
            String freshnessStatus,
            long sourceAgeDays,
            String changeSummary
    ) {
        public CiteableSource(
                String sourceId,
                String sourceVersion,
                String sourceType,
                String title,
                String canonicalUrl,
                String locator
        ) {
            this(sourceId, sourceVersion, sourceType, title, canonicalUrl, locator, null, "unknown", "unknown", 0L, "No source-change summary is available.");
        }
    }
}
