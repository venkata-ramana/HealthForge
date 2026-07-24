package dev.healthforge.platform.ingestion;

import java.time.Instant;

public record IngestionJob(
        String ingestionId,
        String status,
        String manifestSourceId,
        Instant requestedAt,
        String sourceVersionId,
        String artifactSha256,
        String parserVersion,
        String chunkingVersion
) {
}
