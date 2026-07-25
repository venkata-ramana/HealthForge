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
        List<BriefResponse.Approval> approvals,
        List<BriefResponse.AuditEvent> auditEvents
) {
    public record WorkItem(
            String workItemId,
            String title,
            String rationale,
            String affectedCapability,
            List<String> standardsTouchpoints,
            List<String> validationNotes,
            String humanReviewStatus,
            List<String> relatedFindingIds,
            List<Evidence> evidence
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
