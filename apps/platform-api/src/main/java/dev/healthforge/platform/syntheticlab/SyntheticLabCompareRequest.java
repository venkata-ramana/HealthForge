package dev.healthforge.platform.syntheticlab;

import jakarta.validation.constraints.NotBlank;

public record SyntheticLabCompareRequest(
        @NotBlank String primaryTemplateId,
        @NotBlank String comparisonTemplateId
) {
}
