package dev.healthforge.platform.fhirsynthetic;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record SyntheticFhirGenerateResponse(
        String scenarioId,
        String title,
        String description,
        String expectedValidationStatus,
        String dataClassification,
        ValidationRecommendation validationRecommendation,
        JsonNode validationRequest,
        JsonNode resource,
        List<String> notes
) {
    public record ValidationRecommendation(
            String packageId,
            String packageVersion,
            String profileUrl,
            String validateEndpoint
    ) {
    }
}
