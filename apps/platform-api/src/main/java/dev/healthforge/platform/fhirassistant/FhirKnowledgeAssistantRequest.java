package dev.healthforge.platform.fhirassistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FhirKnowledgeAssistantRequest(
        @NotBlank String query,
        @Pattern(regexp = "resource|profile|operation|implementation_guide", message = "artifact_type must be one of resource, profile, operation, or implementation_guide")
        String artifactType,
        String packageId,
        Integer limit
) {
}
