package dev.healthforge.platform.ingestion;

import jakarta.validation.constraints.NotBlank;

public record SourceWatchlistRequest(
        @NotBlank String manifestSourceId,
        @NotBlank String watchReason,
        @NotBlank String desiredCheckFrequency
) {
}
