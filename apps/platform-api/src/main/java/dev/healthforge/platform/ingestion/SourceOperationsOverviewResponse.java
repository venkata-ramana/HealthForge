package dev.healthforge.platform.ingestion;

import java.time.Instant;
import java.util.List;

public record SourceOperationsOverviewResponse(
        String organizationId,
        Instant generatedAt,
        Summary summary,
        List<WatchlistItem> watchlists,
        List<FreshnessAlert> freshnessAlerts,
        List<ReindexRecommendation> reindexRecommendations
) {
    public record Summary(
            int totalTrackedSources,
            int watchlistedSources,
            int staleSources,
            int supersededSources,
            Instant latestRetrievedAt,
            String summary
    ) {
    }

    public record WatchlistItem(
            String watchlistId,
            String manifestSourceId,
            String title,
            String canonicalUrl,
            String latestSourceVersionId,
            String latestSourceVersion,
            String freshnessStatus,
            long sourceAgeDays,
            String watchReason,
            String desiredCheckFrequency,
            String recommendedAction,
            Instant updatedAt
    ) {
    }

    public record FreshnessAlert(
            String manifestSourceId,
            String title,
            String latestSourceVersionId,
            String latestSourceVersion,
            String freshnessStatus,
            long sourceAgeDays,
            String changeSummary,
            String alertReason
    ) {
    }

    public record ReindexRecommendation(
            String manifestSourceId,
            String title,
            String latestSourceVersionId,
            String latestSourceVersion,
            String recommendationType,
            String rationale,
            String recommendedAction
    ) {
    }
}
