package dev.healthforge.platform.trackerexport;

import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.automation.WorkflowAutomationService;
import dev.healthforge.platform.brief.BriefAuditEventService;
import dev.healthforge.platform.brief.BriefService;
import dev.healthforge.platform.brief.BriefWorkItemExportResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class TrackedWorkItemExportService {

    private final BriefService briefService;
    private final BriefAuditEventService auditEventService;
    private final JdbcTemplate jdbcTemplate;
    private final WorkflowAutomationService automationService;
    private final Clock clock = Clock.systemUTC();

    public TrackedWorkItemExportService(
            BriefService briefService,
            BriefAuditEventService auditEventService
    ) {
        this(briefService, auditEventService, null, null);
    }

    @Autowired
    public TrackedWorkItemExportService(
            BriefService briefService,
            BriefAuditEventService auditEventService,
            JdbcTemplate jdbcTemplate
    ) {
        this(briefService, auditEventService, jdbcTemplate, null);
    }

    public TrackedWorkItemExportService(
            BriefService briefService,
            BriefAuditEventService auditEventService,
            JdbcTemplate jdbcTemplate,
            WorkflowAutomationService automationService
    ) {
        this.briefService = briefService;
        this.auditEventService = auditEventService;
        this.jdbcTemplate = jdbcTemplate;
        this.automationService = automationService;
    }

    public TrackedWorkItemExportResponse preview(TrackedWorkItemExportRequest request, AuthenticatedActor actor) {
        var target = request.targetSystem().trim().toLowerCase(Locale.ROOT);
        if (!List.of("github", "jira").contains(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "target_system must be github or jira");
        }
        if (!request.approvalAcknowledgement()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Explicit approval acknowledgement is required before generating tracker-ready export payloads.");
        }

        var export = briefService.exportWorkItems(request.briefId(), actor);
        var workItemIds = request.workItemIds() == null ? List.<String>of() : request.workItemIds();
        var selectedItems = export.workItems().stream()
                .filter(item -> workItemIds.isEmpty() || workItemIds.contains(item.workItemId()))
                .toList();
        if (selectedItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No approved work items matched the requested export.");
        }

        var occurredAt = Instant.now(clock);
        var retentionUntil = occurredAt.plus(30, ChronoUnit.DAYS);
        var executionBundle = execution(target, request, actor, occurredAt);
        var mode = request.writebackRequested() ? "governed_writeback" : "preview_only";
        var approvalGate = executionBundle.approvalGate();
        var execution = executionBundle.writebackExecution();

        if (jdbcTemplate != null) {
            jdbcTemplate.update("""
                    insert into tracked_export_event (
                        tracked_export_event_id, brief_id, organization_id, actor_id, actor_role, target_system,
                        export_mode, work_item_count, export_reason, retention_until, occurred_at,
                        writeback_approval_id, approval_actor_id, approval_actor_role, approval_recorded_at,
                        target_locator, execution_status, execution_result, external_reference,
                        retry_count, retried_from_event_id, executed_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    "tracked_export_event_" + UUID.randomUUID(),
                    request.briefId(),
                    actor.organizationId(),
                    actor.actorId(),
                    actor.role().name().toLowerCase(Locale.ROOT),
                    target,
                    mode,
                    selectedItems.size(),
                    request.exportReason(),
                    Timestamp.from(retentionUntil),
                    Timestamp.from(occurredAt),
                    approvalGate == null ? null : approvalGate.approvalId(),
                    approvalGate == null ? null : approvalGate.approvedBy(),
                    approvalGate == null ? null : approvalGate.approvedRole(),
                    approvalGate == null || approvalGate.approvedAt() == null ? null : Timestamp.from(approvalGate.approvedAt()),
                    execution.targetLocator(),
                    execution.executionStatus(),
                    execution.executionResult(),
                    execution.externalReference(),
                    execution.retryCount(),
                    execution.retriedFromExportId(),
                    execution.executedAt() == null ? null : Timestamp.from(execution.executedAt())
            );
        }

        var eventType = switch (execution.executionStatus()) {
            case "writeback_executed", "writeback_retried" -> "tracker_writeback_executed";
            case "writeback_blocked" -> "tracker_writeback_blocked";
            default -> "tracker_export_preview_generated";
        };
        auditEventService.record(request.briefId(), actor, eventType,
                auditSummary(target, selectedItems.size(), execution.executionStatus()),
                "target=" + target
                        + ", work_items=" + selectedItems.size()
                        + ", mode=" + mode
                        + ", status=" + execution.executionStatus()
                        + ", retention_until=" + retentionUntil
                        + (execution.externalReference() == null ? "" : ", external_reference=" + execution.externalReference()));
        if (automationService != null) {
            automationService.emit(
                    request.briefId(),
                    actor,
                    "export",
                    execution.executionStatus(),
                    "Tracked export for " + target + " completed with status " + execution.executionStatus() + ".",
                    "private_demo",
                    request.writebackRequested() && !"writeback_blocked".equals(execution.executionStatus()),
                    null
            );
        }

        return new TrackedWorkItemExportResponse(
                "tracked_export_" + UUID.randomUUID(),
                request.briefId(),
                target,
                mode,
                occurredAt,
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                selectedItems.stream().map(item -> toPreviewItem(item, target)).toList(),
                List.of(
                        "Provider credentials, access tokens, and permission models remain outside source control.",
                        "Human approval, audit review, and downstream backlog curation remain required before creating or retrying external tickets.",
                        "Writeback actions in this phase are explicit, auditable, and disableable per environment."
                ),
                request.writebackRequested(),
                approvalGate,
                execution,
                request.writebackRequested()
                        ? "Governed GitHub and Jira writeback requires explicit approval records, actor traceability, and operator-visible execution status."
                        : "Tracker-ready previews remain available without executing writeback. Human review remains mandatory before external delivery."
        );
    }

    private ExecutionBundle execution(
            String target,
            TrackedWorkItemExportRequest request,
            AuthenticatedActor actor,
            Instant occurredAt
    ) {
        if (!request.writebackRequested()) {
            return new ExecutionBundle(
                    null,
                    new TrackedWorkItemExportResponse.WritebackExecution(
                            "preview_generated",
                            "Preview payload generated without external writeback.",
                            request.targetLocator(),
                            null,
                            0,
                            null,
                            null
                    )
            );
        }

        var approvalGate = loadApprovalGate(request.briefId(), actor.organizationId(), request.writebackApprovalId());
        if (approvalGate == null) {
            return new ExecutionBundle(
                    null,
                    new TrackedWorkItemExportResponse.WritebackExecution(
                            "writeback_blocked",
                            "A valid Brief approval record is required before governed writeback can execute.",
                            request.targetLocator(),
                            null,
                            0,
                            request.retryFromExportId(),
                            occurredAt
                    )
            );
        }

        if (request.targetLocator() == null || request.targetLocator().isBlank()) {
            return new ExecutionBundle(
                    approvalGate,
                    new TrackedWorkItemExportResponse.WritebackExecution(
                            "writeback_blocked",
                            "target_locator is required for governed GitHub or Jira writeback.",
                            null,
                            null,
                            0,
                            request.retryFromExportId(),
                            occurredAt
                    )
            );
        }

        var retryMetadata = retryMetadata(request.briefId(), actor.organizationId(), request.retryFromExportId());
        if (request.retryFromExportId() != null && !request.retryFromExportId().isBlank() && retryMetadata == null) {
            return new ExecutionBundle(
                    approvalGate,
                    new TrackedWorkItemExportResponse.WritebackExecution(
                            "writeback_blocked",
                            "retry_from_export_id did not match a prior tracked export event for this Brief and organization.",
                            request.targetLocator(),
                            null,
                            0,
                            request.retryFromExportId(),
                            occurredAt
                    )
            );
        }

        var retryCount = retryMetadata == null ? 0 : retryMetadata.retryCount() + 1;
        return new ExecutionBundle(
                approvalGate,
                new TrackedWorkItemExportResponse.WritebackExecution(
                        retryCount == 0 ? "writeback_executed" : "writeback_retried",
                        "Governed writeback executed through the local integration stub with explicit approval traceability. Production connector credentials remain environment-governed.",
                        request.targetLocator(),
                        externalReference(target, request.targetLocator(), retryCount),
                        retryCount,
                        retryMetadata == null ? null : retryMetadata.eventId(),
                        occurredAt
                )
        );
    }

    private TrackedWorkItemExportResponse.ApprovalGate loadApprovalGate(
            String briefId,
            String organizationId,
            String approvalId
    ) {
        if (jdbcTemplate == null || approvalId == null || approvalId.isBlank()) {
            return null;
        }
        var rows = jdbcTemplate.queryForList("""
                select approval_id, approver, approver_role, approved_at
                from brief_approval
                where approval_id = ? and brief_id = ? and organization_id = ?
                """, approvalId, briefId, organizationId);
        if (rows.isEmpty()) {
            return null;
        }
        var row = rows.getFirst();
        return new TrackedWorkItemExportResponse.ApprovalGate(
                string(row, "approval_id"),
                string(row, "approver"),
                string(row, "approver_role"),
                timestamp(row.get("approved_at"))
        );
    }

    private RetryMetadata retryMetadata(
            String briefId,
            String organizationId,
            String retryFromExportId
    ) {
        if (jdbcTemplate == null || retryFromExportId == null || retryFromExportId.isBlank()) {
            return null;
        }
        var rows = jdbcTemplate.queryForList("""
                select tracked_export_event_id, retry_count
                from tracked_export_event
                where tracked_export_event_id = ? and brief_id = ? and organization_id = ?
                """, retryFromExportId, briefId, organizationId);
        if (rows.isEmpty()) {
            return null;
        }
        var row = rows.getFirst();
        var retryCount = row.get("retry_count") instanceof Number number ? number.intValue() : 0;
        return new RetryMetadata(string(row, "tracked_export_event_id"), retryCount);
    }

    private String auditSummary(String target, int itemCount, String executionStatus) {
        return switch (executionStatus) {
            case "writeback_executed" -> "Executed governed " + target + " writeback for approved work items.";
            case "writeback_retried" -> "Retried governed " + target + " writeback for approved work items.";
            case "writeback_blocked" -> "Blocked governed " + target + " writeback until approval or target requirements are satisfied.";
            default -> "Generated " + target + " tracker-ready preview payloads from approved work items.";
        };
    }

    private String externalReference(String target, String targetLocator, int retryCount) {
        var suffix = retryCount == 0 ? "sim-1" : "retry-" + retryCount;
        return switch (target) {
            case "github" -> "https://github.com/" + targetLocator + "/issues/" + suffix;
            case "jira" -> targetLocator + "-" + (retryCount == 0 ? "SIM-1" : "RETRY-" + retryCount);
            default -> target + ":" + targetLocator + ":" + suffix;
        };
    }

    private String string(Map<String, Object> row, String key) {
        var value = row.get(key);
        return value == null ? null : value.toString();
    }

    private Instant timestamp(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        return null;
    }

    private TrackedWorkItemExportResponse.PreviewItem toPreviewItem(
            BriefWorkItemExportResponse.WorkItem item,
            String target
    ) {
        var body = previewBody(item);
        var labels = labels(item);
        var github = new TrackedWorkItemExportResponse.GitHubShape(item.title(), body, labels);
        var jira = new TrackedWorkItemExportResponse.JiraShape(item.title(), body, "Task", labels);
        return new TrackedWorkItemExportResponse.PreviewItem(
                item.workItemId(),
                item.title(),
                target,
                labels,
                body,
                jira,
                github
        );
    }

    private String previewBody(BriefWorkItemExportResponse.WorkItem item) {
        var lines = new ArrayList<String>();
        lines.add("HealthForge approved work-item preview");
        lines.add("");
        lines.add("Title: " + item.title());
        lines.add("Capability: " + item.affectedCapability());
        lines.add("Primary track: " + item.primaryTrack());
        lines.add("Workflow stage: " + item.workflowStage());
        lines.add("Human review status: " + item.humanReviewStatus());
        lines.add("");
        lines.add("Summary:");
        lines.add(item.rationale());
        lines.add("");
        lines.add("Dependencies:");
        item.dependencies().forEach(dependency -> lines.add("- " + dependency));
        lines.add("");
        lines.add("Standards touchpoints:");
        item.standardsTouchpoints().forEach(touchpoint -> lines.add("- " + touchpoint));
        lines.add("");
        lines.add("Reviewer-visible warnings:");
        item.validationNotes().forEach(note -> lines.add("- " + note));
        lines.add("");
        lines.add("Evidence:");
        item.evidence().forEach(evidence -> lines.add("- " + evidence.title() + " [" + evidence.sourceVersion() + "] " + evidence.locator()));
        lines.add("");
        lines.add("Non-goal: this payload does not bypass human approval, audit review, or environment controls.");
        return String.join("\n", lines);
    }

    private List<String> labels(BriefWorkItemExportResponse.WorkItem item) {
        var labels = new LinkedHashSet<String>();
        labels.add("healthforge");
        labels.add("reviewed-brief");
        labels.add(item.primaryTrack());
        labels.add(item.workflowStage().replace('_', '-'));
        labels.add(item.affectedCapability().replace('_', '-'));
        item.standardsTouchpoints().stream()
                .limit(2)
                .map(touchpoint -> touchpoint.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-"))
                .forEach(labels::add);
        return labels.stream().toList();
    }

    private record RetryMetadata(String eventId, int retryCount) {
    }

    private record ExecutionBundle(
            TrackedWorkItemExportResponse.ApprovalGate approvalGate,
            TrackedWorkItemExportResponse.WritebackExecution writebackExecution
    ) {
    }
}
