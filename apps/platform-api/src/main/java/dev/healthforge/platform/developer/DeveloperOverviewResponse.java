package dev.healthforge.platform.developer;

import dev.healthforge.platform.brief.BriefSummary;

import java.time.Instant;
import java.util.List;

public record DeveloperOverviewResponse(
        String organizationId,
        Instant generatedAt,
        List<BriefSummary> approvedBriefs,
        List<WorkspaceSurface> workspaceSurfaces,
        List<AutomationRecipe> automationRecipes,
        List<String> deliveryGuardrails
) {
    public record WorkspaceSurface(
            String surfaceId,
            String title,
            String workflowType,
            String summary,
            List<String> supportedActions
    ) {
    }

    public record AutomationRecipe(
            String recipeId,
            String title,
            String command,
            String summary,
            List<String> expectedOutputs
    ) {
    }
}
