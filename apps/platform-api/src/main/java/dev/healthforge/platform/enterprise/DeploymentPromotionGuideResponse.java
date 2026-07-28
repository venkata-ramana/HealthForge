package dev.healthforge.platform.enterprise;

import java.util.List;

public record DeploymentPromotionGuideResponse(
        String organizationId,
        String deploymentMode,
        List<EnvironmentStage> environments,
        List<String> promotionSteps,
        List<String> rollbackSteps,
        List<String> operatorGuardrails
) {
    public record EnvironmentStage(
            String name,
            String purpose,
            List<String> configurationSurfaces,
            List<String> operatorChecks
    ) {
    }
}
