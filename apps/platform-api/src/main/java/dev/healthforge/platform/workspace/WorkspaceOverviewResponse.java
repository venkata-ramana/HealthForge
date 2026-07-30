package dev.healthforge.platform.workspace;

import java.time.Instant;
import java.util.List;

public record WorkspaceOverviewResponse(
        String organizationId,
        Instant generatedAt,
        AuthFoundation authFoundation,
        List<ProjectSummary> projects,
        List<QueueSummary> queues,
        List<AssignmentSummary> assignments,
        List<WorkflowConfigurationSummary> workflowConfigurations,
        List<SavedViewSummary> savedViews,
        List<EvidenceCollectionSummary> evidenceCollections,
        List<ResearchPackSummary> researchPacks,
        List<QuestionPackSummary> questionPacks,
        List<ScenarioTemplateSummary> scenarioTemplates,
        List<PersonaPresetSummary> personaPresets,
        List<PrecedentComparisonSummary> precedentComparisons,
        List<DecisionPatternSummary> decisionPatterns,
        List<ThemeClusterSummary> themeClusters,
        List<ResearchNotebookSummary> researchNotebooks,
        List<TopicSummary> topicBrowser,
        ReviewerOperationsSummary reviewerOperations
) {
    public record AuthFoundation(
            String activeMode,
            String modeSummary,
            List<String> supportedModes,
            List<IdentityProviderSummary> identityProviders,
            List<GroupRoleMappingSummary> groupRoleMappings
    ) {}

    public record IdentityProviderSummary(
            String providerId,
            String providerType,
            String displayName,
            String status,
            String fallbackMode
    ) {}

    public record GroupRoleMappingSummary(
            String mappingId,
            String providerId,
            String groupName,
            String actorRole,
            String scopeSummary
    ) {}

    public record ProjectSummary(
            String projectId,
            String name,
            String kind,
            String description,
            String ownerActorId,
            List<String> tags,
            List<String> briefIds,
            int briefCount,
            Instant updatedAt
    ) {}

    public record QueueSummary(
            String queueName,
            int totalAssignments,
            int draftBriefs,
            int inReviewBriefs,
            int changesRequestedBriefs,
            int approvedBriefs
    ) {}

    public record AssignmentSummary(
            String assignmentId,
            String briefId,
            String briefQuestion,
            String briefStatus,
            String assigneeActorId,
            String assigneeRole,
            String queueName,
            String status,
            String handoffSummary,
            Instant updatedAt
    ) {}

    public record WorkflowConfigurationSummary(
            String configId,
            String configType,
            String name,
            String versionLabel,
            String status,
            String summary,
            String promptProfile,
            String retrievalProfile,
            String workflowProfile,
            Instant updatedAt
    ) {}

    public record SavedViewSummary(
            String viewId,
            String projectId,
            String projectName,
            String viewType,
            String name,
            String queryText,
            String summary,
            Instant updatedAt
    ) {}

    public record EvidenceCollectionSummary(
            String collectionId,
            String projectId,
            String projectName,
            String name,
            String summary,
            int sourceCount,
            Instant updatedAt
    ) {}

    public record ResearchPackSummary(
            String researchPackId,
            String projectId,
            String projectName,
            String name,
            String summary,
            int questionCount,
            List<String> recurringQuestions,
            Instant nextReviewAt,
            Instant updatedAt
    ) {}

    public record QuestionPackSummary(
            String questionPackId,
            String projectId,
            String projectName,
            String name,
            String summary,
            String persona,
            String templateKind,
            String starterQuestion,
            List<String> questionPrompts,
            Instant updatedAt
    ) {}

    public record ScenarioTemplateSummary(
            String templateId,
            String title,
            String persona,
            String workflowStage,
            String summary,
            List<String> startingPoints
    ) {}

    public record PersonaPresetSummary(
            String presetId,
            String persona,
            String recommendedRole,
            String startingView,
            String summary,
            List<String> focusAreas
    ) {}

    public record PrecedentComparisonSummary(
            String comparisonId,
            String primaryBriefId,
            String primaryQuestion,
            String relatedBriefId,
            String relatedQuestion,
            String overlapTheme,
            List<String> sharedSignals,
            String advisorySummary
    ) {}

    public record DecisionPatternSummary(
            String patternId,
            String title,
            String patternType,
            String summary,
            List<String> signals
    ) {}

    public record ThemeClusterSummary(
            String clusterId,
            String theme,
            int briefCount,
            int approvedCount,
            List<String> representativeQuestions,
            String summary
    ) {}

    public record ResearchNotebookSummary(
            String notebookId,
            String projectId,
            String projectName,
            String briefId,
            String briefQuestion,
            String title,
            String summary,
            List<String> keyTakeaways,
            String evidenceBundleName,
            String handoffSummary,
            String continuityNote,
            Instant updatedAt
    ) {}

    public record TopicSummary(
            String topicId,
            String theme,
            int relatedBriefs,
            int relatedFindings,
            int approvalCount,
            String summary
    ) {}

    public record ReviewerOperationsSummary(
            int totalAssignments,
            int dueSoonAssignments,
            int staleAssignments,
            int escalatedAssignments,
            List<SlaCueSummary> slaCues,
            List<EscalationSummary> escalations
    ) {}

    public record SlaCueSummary(
            String assignmentId,
            String briefId,
            String briefQuestion,
            String queueName,
            String assigneeActorId,
            long ageDays,
            String urgency,
            String recommendation
    ) {}

    public record EscalationSummary(
            String escalationId,
            String assignmentId,
            String briefId,
            String briefQuestion,
            String escalationReason,
            String urgency,
            String destinationQueue,
            String status,
            String note,
            Instant updatedAt
    ) {}
}
