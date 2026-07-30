package dev.healthforge.platform.intake;

import java.time.Instant;
import java.util.List;

public record InboundCaseResponse(
        String caseId,
        String organizationId,
        String sourceSystem,
        String externalCaseId,
        String title,
        String summary,
        String intakeStatus,
        String requestedRole,
        String requestedAssignee,
        String linkedBriefId,
        String sourceLocator,
        Instant createdAt,
        List<String> lineage,
        WorkflowLineage workflowLineage
) {
    public record WorkflowLineage(
            String linkedBriefStatus,
            int approvalCount,
            int trackedExportCount,
            int documentationExportCount,
            String latestDeliveryStatus,
            List<String> downstreamReferences
    ) {}
}
