package dev.healthforge.platform.brief;

import java.time.Instant;
import java.util.List;

public record BriefWorkItemExportResponse(
        String briefId,
        String briefStatus,
        Instant createdAt,
        Instant exportedAt,
        String approvalStatus,
        String exportBoundary,
        List<WorkItem> workItems,
        List<ImplementationTrack> implementationTracks,
        List<BriefResponse.Approval> approvals,
        List<BriefResponse.AuditEvent> auditEvents
) {
    public record WorkItem(
            String workItemId,
            String title,
            String rationale,
            String affectedCapability,
            String primaryTrack,
            String workflowStage,
            List<String> dependencies,
            List<String> standardsTouchpoints,
            List<String> validationNotes,
            String humanReviewStatus,
            List<String> relatedFindingIds,
            List<Evidence> evidence
    ) {}

    public record ImplementationTrack(
            String trackId,
            String title,
            String actorFocus,
            String summary,
            List<String> dependencies,
            List<String> workflowStages,
            List<String> standardsTouchpoints,
            List<String> workItemIds
    ) {}

    public record Evidence(
            String sourceId,
            String sourceVersion,
            String title,
            String canonicalUrl,
            String locator,
            String support,
            String acceptedBy,
            Instant acceptedAt,
            String reviewRationale
    ) {}
}
