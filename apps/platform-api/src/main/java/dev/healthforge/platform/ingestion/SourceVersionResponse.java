package dev.healthforge.platform.ingestion;

import java.time.Instant;

public record SourceVersionResponse(
        String sourceVersionId,
        String manifestSourceId,
        String sourceVersion,
        String sourceType,
        String title,
        String canonicalUrl,
        String artifactSha256,
        String contentType,
        Instant retrievedAt,
        String parserVersion,
        String chunkingVersion,
        String status,
        String allowedUse,
        String termsReviewDecision,
        String termsReviewedBy,
        Instant termsReviewedAt,
        String supersededBySourceVersionId,
        boolean eligibleForCurrentSnapshot
) {
}
