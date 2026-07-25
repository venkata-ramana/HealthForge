package dev.healthforge.platform.ingestion;

import jakarta.validation.constraints.NotBlank;

public record SourceLifecycleUpdateRequest(
        @NotBlank String status
) {
}
