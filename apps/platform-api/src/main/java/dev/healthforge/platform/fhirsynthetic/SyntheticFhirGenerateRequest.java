package dev.healthforge.platform.fhirsynthetic;

import jakarta.validation.constraints.NotBlank;

public record SyntheticFhirGenerateRequest(
        @NotBlank String scenarioId
) {
}
