package dev.healthforge.platform.intake;

import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.brief.BriefRequest;
import dev.healthforge.platform.brief.BriefService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InboundCaseService {

    private final JdbcTemplate jdbcTemplate;
    private final BriefService briefService;
    private final Clock clock = Clock.systemUTC();

    public InboundCaseService(JdbcTemplate jdbcTemplate, BriefService briefService) {
        this.jdbcTemplate = jdbcTemplate;
        this.briefService = briefService;
    }

    public InboundCaseResponse intake(InboundCaseRequest request, AuthenticatedActor actor) {
        var createdAt = Instant.now(clock);
        var linkedBriefId = createBriefIfRequested(request, actor);
        var caseId = "case_" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into inbound_case (
                    inbound_case_id, organization_id, source_system, external_case_id, title, summary,
                    intake_status, requested_role, requested_assignee, linked_brief_id, source_locator, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                caseId, actor.organizationId(), normalized(request.sourceSystem()), request.externalCaseId(),
                request.title(), request.summary(), linkedBriefId == null ? "received" : "brief_created",
                normalized(request.requestedRole()), blankToNull(request.requestedAssignee()), linkedBriefId,
                blankToNull(request.sourceLocator()), Timestamp.from(createdAt), Timestamp.from(createdAt)
        );
        return new InboundCaseResponse(
                caseId,
                actor.organizationId(),
                normalized(request.sourceSystem()),
                request.externalCaseId(),
                request.title(),
                request.summary(),
                linkedBriefId == null ? "received" : "brief_created",
                normalized(request.requestedRole()),
                blankToNull(request.requestedAssignee()),
                linkedBriefId,
                blankToNull(request.sourceLocator()),
                createdAt,
                List.of(
                        "Source system: " + normalized(request.sourceSystem()),
                        "External case: " + request.externalCaseId(),
                        linkedBriefId == null ? "No brief created automatically." : "Linked Brief: " + linkedBriefId
                ),
                workflowLineage(actor.organizationId(), linkedBriefId)
        );
    }

    public List<InboundCaseResponse> list(AuthenticatedActor actor) {
        return jdbcTemplate.query("""
                select inbound_case_id, organization_id, source_system, external_case_id, title, summary, intake_status,
                       requested_role, requested_assignee, linked_brief_id, source_locator, created_at
                from inbound_case
                where organization_id = ?
                order by created_at desc
                """, (rs, row) -> new InboundCaseResponse(
                rs.getString("inbound_case_id"),
                rs.getString("organization_id"),
                rs.getString("source_system"),
                rs.getString("external_case_id"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("intake_status"),
                rs.getString("requested_role"),
                rs.getString("requested_assignee"),
                rs.getString("linked_brief_id"),
                rs.getString("source_locator"),
                rs.getTimestamp("created_at").toInstant(),
                List.of(
                        "Source system: " + rs.getString("source_system"),
                        "External case: " + rs.getString("external_case_id"),
                        rs.getString("linked_brief_id") == null ? "No linked Brief." : "Linked Brief: " + rs.getString("linked_brief_id")
                ),
                workflowLineage(actor.organizationId(), rs.getString("linked_brief_id"))
        ), actor.organizationId());
    }

    private String createBriefIfRequested(InboundCaseRequest request, AuthenticatedActor actor) {
        if (!request.createBrief()) {
            return null;
        }
        var brief = briefService.create(new BriefRequest(
                request.corpusId() == null || request.corpusId().isBlank() ? "mvp-regulatory-corpus" : request.corpusId(),
                request.corpusVersion() == null || request.corpusVersion().isBlank() ? "2026-07-24-expanded-web-core-v4" : request.corpusVersion(),
                request.briefQuestion() == null || request.briefQuestion().isBlank() ? request.title() : request.briefQuestion(),
                request.projectContext() == null || request.projectContext().isBlank() ? request.summary() : request.projectContext(),
                null
        ), actor);
        return brief.briefId();
    }

    private String normalized(String value) {
        return value.trim().toLowerCase().replace(' ', '_');
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private InboundCaseResponse.WorkflowLineage workflowLineage(String organizationId, String linkedBriefId) {
        if (linkedBriefId == null || linkedBriefId.isBlank()) {
            return new InboundCaseResponse.WorkflowLineage(
                    null,
                    0,
                    0,
                    0,
                    "no_linked_brief",
                    List.of()
            );
        }
        var briefRows = jdbcTemplate.queryForList("""
                select status from engineering_brief where organization_id = ? and brief_id = ?
                """, organizationId, linkedBriefId);
        var briefStatus = briefRows.isEmpty() ? null : String.valueOf(briefRows.getFirst().get("status"));
        var approvalCount = count("""
                select count(*) from brief_approval where organization_id = ? and brief_id = ?
                """, organizationId, linkedBriefId);
        var trackedExportCount = count("""
                select count(*) from tracked_export_event where organization_id = ? and brief_id = ?
                """, organizationId, linkedBriefId);
        var documentationExportCount = count("""
                select count(*) from documentation_export_event where organization_id = ? and brief_id = ?
                """, organizationId, linkedBriefId);
        var downstreamReferences = jdbcTemplate.query("""
                select external_reference
                from (
                    select external_reference, occurred_at
                    from tracked_export_event
                    where organization_id = ? and brief_id = ? and external_reference is not null
                    union all
                    select external_reference, occurred_at
                    from documentation_export_event
                    where organization_id = ? and brief_id = ? and external_reference is not null
                ) refs
                order by occurred_at desc
                limit 4
                """, (rs, row) -> rs.getString("external_reference"), organizationId, linkedBriefId, organizationId, linkedBriefId);
        var latestStatuses = jdbcTemplate.query("""
                select status_value
                from (
                    select execution_status as status_value, occurred_at
                    from tracked_export_event
                    where organization_id = ? and brief_id = ?
                    union all
                    select delivery_status as status_value, occurred_at
                    from documentation_export_event
                    where organization_id = ? and brief_id = ?
                ) statuses
                order by occurred_at desc
                limit 1
                """, (rs, row) -> rs.getString("status_value"), organizationId, linkedBriefId, organizationId, linkedBriefId);
        return new InboundCaseResponse.WorkflowLineage(
                briefStatus,
                approvalCount,
                trackedExportCount,
                documentationExportCount,
                latestStatuses.isEmpty() ? "no_downstream_activity" : latestStatuses.getFirst(),
                downstreamReferences
        );
    }

    private int count(String sql, String organizationId, String briefId) {
        var value = jdbcTemplate.queryForObject(sql, Integer.class, organizationId, briefId);
        return value == null ? 0 : value;
    }
}
