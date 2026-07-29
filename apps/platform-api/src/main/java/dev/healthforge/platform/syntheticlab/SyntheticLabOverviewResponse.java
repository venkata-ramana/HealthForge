package dev.healthforge.platform.syntheticlab;

import java.time.Instant;
import java.util.List;

public record SyntheticLabOverviewResponse(
        Instant generatedAt,
        String summary,
        CoverageSummary coverageSummary,
        List<TemplateSummary> templates,
        List<SupportMatrixEntry> supportMatrix,
        List<ValidationGap> validationGaps,
        List<String> recommendedNextScenarios
) {
    public record CoverageSummary(
            int totalTemplates,
            int validTemplates,
            int negativeTemplates,
            List<String> supportedJourneys
    ) {
    }

    public record TemplateSummary(
            String templateId,
            String title,
            String description,
            String journeyType,
            String syntheticScenarioId,
            String expectedValidationStatus,
            List<String> primaryActors,
            List<String> coverageTags
    ) {
    }

    public record SupportMatrixEntry(
            String workflowArea,
            String coverageStatus,
            List<String> templateIds,
            String notes
    ) {
    }

    public record ValidationGap(
            String area,
            String severity,
            String gap,
            String suggestedNextTemplate
    ) {
    }
}
