package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record OperationsConfigurationResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        String deploymentTier,
        List<EnvironmentPolicyView> environments,
        List<ConfigBoundaryView> configBoundaries,
        List<SecretReferenceView> secretReferences,
        List<String> operatorAssumptions,
        String summary
) {
    public record EnvironmentPolicyView(
            String environmentName,
            String promotionGate,
            String secretBoundary,
            String changeWindow,
            String dataBoundary
    ) {
    }

    public record ConfigBoundaryView(
            String key,
            String classification,
            String source,
            String exposurePolicy,
            String rationale
    ) {
    }

    public record SecretReferenceView(
            String system,
            String reference,
            String rotationExpectation,
            String usageBoundary
    ) {
    }
}
