package dev.healthforge.platform.developer;

import java.time.Instant;
import java.util.List;

public record DeveloperRepoGuidanceResponse(
        String briefId,
        String repositoryName,
        String workspaceRoot,
        Instant generatedAt,
        String summary,
        RepoContext repoContext,
        List<ImplementationFocus> implementationFocus,
        List<FileSuggestion> fileSuggestions,
        List<AutomationStep> automationSteps,
        List<String> traceabilityNotes,
        List<String> deliveryGuardrails
) {
    public record RepoContext(
            int inventoryCount,
            int changedFileCount,
            List<String> detectedTechnologySignals,
            List<String> changedFiles
    ) {
    }

    public record ImplementationFocus(
            String workItemId,
            String title,
            String workflowStage,
            String affectedCapability,
            String rationale,
            List<String> dependencies,
            List<String> standardsTouchpoints,
            List<String> validationNotes
    ) {
    }

    public record FileSuggestion(
            String path,
            String matchReason,
            String recommendation,
            List<String> relatedWorkItemIds
    ) {
    }

    public record AutomationStep(
            String stepId,
            String title,
            String commandHint,
            String expectedOutcome
    ) {
    }
}
