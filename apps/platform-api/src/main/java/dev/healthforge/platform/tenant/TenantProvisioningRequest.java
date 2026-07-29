package dev.healthforge.platform.tenant;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record TenantProvisioningRequest(
        @NotBlank String tenantKey,
        @NotBlank String tenantName,
        @NotBlank String deploymentModel,
        @NotBlank String environmentShape,
        @NotBlank String delegatedAdmin,
        List<String> requestedCapabilities,
        @NotBlank String onboardingSummary
) {
}
