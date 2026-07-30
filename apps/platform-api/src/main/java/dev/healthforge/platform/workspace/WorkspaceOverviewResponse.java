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
        List<ResearchPackSummary> researchPacks
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
}
