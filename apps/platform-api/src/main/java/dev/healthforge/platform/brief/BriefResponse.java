package dev.healthforge.platform.brief;

import java.time.Instant;
import java.util.List;

public record BriefResponse(
        String briefId,
        String status,
        Instant createdAt,
        Input input,
        List<Source> sources,
        List<Finding> findings,
        String summary,
        List<String> limitations,
        List<ReviewDecision> reviewDecisions,
        List<AuditEvent> auditEvents,
        boolean requiresHumanReview
) {
    public record Input(String question, String projectContext, String corpusId, String corpusVersion) {}
    public record Source(String sourceId, String sourceVersion, String sourceType, String title, String canonicalUrl) {}
    public record Finding(String findingId, String kind, String statement, String confidence, Citation citation) {}
    public record Citation(String sourceId, String sourceVersion, String locator, String support) {}
    public record ReviewDecision(String reviewId, String findingId, String decision, String reviewer, Instant decidedAt, String rationale, String correctedStatement) {}
    public record AuditEvent(String auditEventId, String actorId, String actorRole, String eventType, Instant occurredAt, String summary, String details) {}
}
