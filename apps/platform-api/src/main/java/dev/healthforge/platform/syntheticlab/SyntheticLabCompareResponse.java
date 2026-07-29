package dev.healthforge.platform.syntheticlab;

import java.time.Instant;
import java.util.List;

public record SyntheticLabCompareResponse(
        Instant generatedAt,
        String primaryTemplateId,
        String comparisonTemplateId,
        String summary,
        List<Difference> differences,
        List<TimelineComparison> timelineComparisons,
        List<String> recommendedUseCases
) {
    public record Difference(
            String area,
            String primaryValue,
            String comparisonValue,
            String impact
    ) {
    }

    public record TimelineComparison(
            String stageId,
            String primaryTitle,
            String comparisonTitle,
            String note
    ) {
    }
}
