package dev.healthforge.platform.syntheticlab;

import jakarta.validation.constraints.NotBlank;

public record SyntheticLabRunRequest(
        @NotBlank String templateId
) {
}
