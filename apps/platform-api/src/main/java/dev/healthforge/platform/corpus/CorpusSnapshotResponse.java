package dev.healthforge.platform.corpus;

import java.time.Instant;
import java.util.List;

public record CorpusSnapshotResponse(
        String corpusId,
        String corpusVersion,
        Instant createdAt,
        String retrievalConfiguration,
        List<String> sourceVersionIds,
        boolean historicalReconstruction
) {}
