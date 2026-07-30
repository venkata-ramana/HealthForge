package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record SolutionPacksResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        List<SolutionPack> packs,
        List<WorkflowPreset> workflowPresets,
        List<OnboardingFlow> onboardingFlows,
        List<StakeholderPack> stakeholderPacks,
        List<String> boundedStatements,
        String summary
) {
    public record SolutionPack(
            String packId,
            String audience,
            String domain,
            String positioning,
            String whenToUse,
            List<String> workflows,
            List<String> demoAngles,
            List<String> trustAngles,
            List<String> deliveryArtifacts,
            List<String> presetIds,
            List<String> onboardingFlowIds,
            List<String> stakeholderPackIds
    ) {
    }

    public record WorkflowPreset(
            String presetId,
            String name,
            String summary,
            String recommendedRole,
            String retrievalProfile,
            String reviewPath,
            String approvalPath,
            String exportPath,
            List<String> editableControls,
            List<String> recommendedTemplates
    ) {
    }

    public record OnboardingFlow(
            String flowId,
            String audience,
            String entryPoint,
            List<String> firstRunPath,
            String successOutcome,
            String boundaryReminder
    ) {
    }

    public record StakeholderPack(
            String packId,
            String audience,
            String title,
            String summary,
            List<String> sourceViews,
            List<String> narrativeAssets,
            List<String> presentationMoments
    ) {
    }
}
