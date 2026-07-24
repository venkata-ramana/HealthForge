package dev.healthforge.platform.ingestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IngestionRequest(
        @NotBlank String manifestSourceId,
        @NotBlank String sourceVersion,
        @NotBlank @Pattern(regexp = "https://.+", message = "must be an HTTPS URL") String canonicalUrl,
        @NotBlank String expectedContentType,
        String requestedBy
) {
}
