package dev.healthforge.platform.tenant;

import java.time.Instant;
import java.util.List;

public record TenantAdministrationOverviewResponse(
        String requestedOrganizationId,
        Instant generatedAt,
        HostedProductPosture hostedProductPosture,
        List<CustomerTenantSummary> customerTenants,
        List<IsolationBoundary> isolationBoundaries,
        List<RoleDelegation> roleDelegations,
        List<ProvisioningRequestSummary> provisioningRequests,
        List<HostedPackagingArtifact> hostedPackagingArtifacts,
        List<String> deliveryGuardrails
) {
    public record HostedProductPosture(
            String productMode,
            String tenancySummary,
            String identitySummary,
            String setupSummary
    ) {}

    public record CustomerTenantSummary(
            String organizationId,
            String displayName,
            String status,
            String tenantTier,
            String deploymentModel,
            int userCount,
            int projectCount,
            int briefCount,
            Instant lastSeenAt
    ) {}

    public record IsolationBoundary(
            String boundaryId,
            String title,
            String summary,
            List<String> enforcedThrough
    ) {}

    public record RoleDelegation(
            String organizationId,
            String delegatedAdmin,
            List<String> assignedRoles,
            String delegationSummary
    ) {}

    public record ProvisioningRequestSummary(
            String provisioningRequestId,
            String tenantKey,
            String tenantName,
            String deploymentModel,
            String environmentShape,
            String status,
            String delegatedAdmin,
            Instant updatedAt
    ) {}

    public record HostedPackagingArtifact(
            String artifactId,
            String title,
            String audience,
            String summary,
            List<String> includedCapabilities
    ) {}
}
