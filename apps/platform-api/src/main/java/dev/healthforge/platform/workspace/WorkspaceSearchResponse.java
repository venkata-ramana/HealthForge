package dev.healthforge.platform.workspace;

import java.time.Instant;
import java.util.List;

public record WorkspaceSearchResponse(
        String organizationId,
        Instant generatedAt,
        String query,
        String facet,
        int totalHits,
        List<SearchHitSummary> hits
) {
    public record SearchHitSummary(
            String hitType,
            String refId,
            String title,
            String excerpt,
            String topic,
            String status
    ) {}
}
