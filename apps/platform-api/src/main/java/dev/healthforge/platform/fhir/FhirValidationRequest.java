package dev.healthforge.platform.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record FhirValidationRequest(
        @NotBlank String packageId,
        @NotBlank String packageVersion,
        @NotBlank String profileUrl,
        @NotBlank @Pattern(
                regexp = "synthetic|non_sensitive",
                message = "must be synthetic or non_sensitive"
        ) String dataClassification,
        @NotNull JsonNode resource
) {
}
