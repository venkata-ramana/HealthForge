package dev.healthforge.platform.ingestion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IngestionRequest(
        @NotBlank String manifestSourceId,
        @NotBlank String sourceVersion,
        @NotBlank @Pattern(regexp = "(https://.+|hf-local:///.+)", message = "must be an HTTPS URL or approved local source URI") String canonicalUrl,
        @NotBlank String expectedContentType,
        String requestedBy,
        @NotBlank String allowedUse,
        @NotBlank String termsReviewDecision,
        @NotBlank String termsReviewedBy
) {
}
