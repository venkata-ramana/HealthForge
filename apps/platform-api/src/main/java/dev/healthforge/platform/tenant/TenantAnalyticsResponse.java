package dev.healthforge.platform.tenant;

import java.time.Instant;
import java.util.List;

public record TenantAnalyticsResponse(
        String requestedOrganizationId,
        Instant generatedAt,
        UsageSummary usageSummary,
        List<TenantUsage> tenantUsage,
        List<ProductPackagingView> productPackaging,
        List<String> stakeholderNarratives
) {
    public record UsageSummary(
            int totalTenants,
            int activeTenants,
            int privateDeploymentTenants,
            int hostedEvaluationTenants
    ) {}

    public record TenantUsage(
            String organizationId,
            String displayName,
            int users,
            int briefsLast30Days,
            int projects,
            int approvalsLast30Days,
            String engagementSignal,
            String packagingFit
    ) {}

    public record ProductPackagingView(
            String packageId,
            String title,
            String deliveryModel,
            String summary,
            List<String> targetCapabilities
    ) {}
}
