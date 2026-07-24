package dev.healthforge.platform.retrieval;

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
            String locator
    ) {
    }
}
