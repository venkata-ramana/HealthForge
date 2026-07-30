package dev.healthforge.platform.corpus;

import java.time.Instant;
import java.util.List;

public record CorpusSnapshotDiffResponse(
        String corpusId,
        String corpusVersion,
        String comparedAgainstCorpusVersion,
        Instant generatedAt,
        Summary summary,
        List<AddedSource> addedSources,
        List<RemovedSource> removedSources,
        List<ChangedSource> changedSources
) {
    public record Summary(
            int addedCount,
            int removedCount,
            int changedCount,
            String summary
    ) {
    }

    public record AddedSource(
            String manifestSourceId,
            String sourceVersionId,
            String sourceVersion,
            String title
    ) {
    }

    public record RemovedSource(
            String manifestSourceId,
            String sourceVersionId,
            String sourceVersion,
            String title
    ) {
    }

    public record ChangedSource(
            String manifestSourceId,
            String previousSourceVersionId,
            String previousSourceVersion,
            String currentSourceVersionId,
            String currentSourceVersion,
            String title,
            String summary
    ) {
    }
}
