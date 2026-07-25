package dev.healthforge.platform.codegen;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record StarterCodeGenerationRequest(
        @NotBlank String briefId,
        @NotBlank String workItemId,
        @NotBlank @Pattern(regexp = "spring_boot_endpoint_stub|spring_service_stub") String artifactType
) {
}
