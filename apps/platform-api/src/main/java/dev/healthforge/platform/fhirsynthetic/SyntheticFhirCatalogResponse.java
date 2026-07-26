package dev.healthforge.platform.fhirsynthetic;

import java.util.List;

public record SyntheticFhirCatalogResponse(
        List<SyntheticScenario> scenarios
) {
    public record SyntheticScenario(
            String scenarioId,
            String title,
            String description,
            String profileUrl,
            String packageId,
            String packageVersion,
            String expectedStatus
    ) {
    }
}
