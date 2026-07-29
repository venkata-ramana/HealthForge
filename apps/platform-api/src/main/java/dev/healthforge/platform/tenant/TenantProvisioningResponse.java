package dev.healthforge.platform.tenant;

import java.time.Instant;
import java.util.List;

public record TenantProvisioningResponse(
        String provisioningRequestId,
        String organizationId,
        String tenantKey,
        String tenantName,
        String deploymentModel,
        String environmentShape,
        String status,
        String requestedBy,
        String delegatedAdmin,
        List<String> requestedCapabilities,
        List<String> setupChecklist,
        String onboardingSummary,
        Instant updatedAt
) {
}
