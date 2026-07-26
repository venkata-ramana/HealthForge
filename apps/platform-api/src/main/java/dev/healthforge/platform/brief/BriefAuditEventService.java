package dev.healthforge.platform.brief;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BriefAuditEventService {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public BriefAuditEventService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(String briefId, AuthenticatedActor actor, String eventType, String summary, String details) {
        jdbcTemplate.update("""
                insert into brief_audit_event (audit_event_id, brief_id, organization_id, actor_id, actor_role, event_type, occurred_at, summary, details)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "audit_" + UUID.randomUUID(),
                briefId,
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                eventType,
                Timestamp.from(Instant.now(clock)),
                summary,
                details
        );
    }

    public List<BriefResponse.AuditEvent> listForBrief(String briefId, String organizationId) {
        return jdbcTemplate.query("""
                select audit_event_id, actor_id, actor_role, event_type, occurred_at, summary, details
                from brief_audit_event where brief_id = ? and organization_id = ? order by occurred_at
                """, (rs, row) -> new BriefResponse.AuditEvent(
                rs.getString("audit_event_id"),
                rs.getString("actor_id"),
                rs.getString("actor_role"),
                rs.getString("event_type"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getString("summary"),
                rs.getString("details")
        ), briefId, organizationId);
    }
}
