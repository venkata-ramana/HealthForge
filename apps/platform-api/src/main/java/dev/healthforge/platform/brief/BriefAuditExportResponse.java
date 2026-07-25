package dev.healthforge.platform.brief;

import java.time.Instant;
import java.util.List;

public record BriefAuditExportResponse(
        String briefId,
        String status,
        Instant createdAt,
        String corpusId,
        String corpusVersion,
        List<BriefResponse.ReviewDecision> reviewDecisions,
        List<BriefResponse.Approval> approvals,
        List<BriefResponse.AuditEvent> auditEvents
) {
}
