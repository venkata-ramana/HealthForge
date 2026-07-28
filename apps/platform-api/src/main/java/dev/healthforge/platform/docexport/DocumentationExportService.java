package dev.healthforge.platform.docexport;

import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.automation.WorkflowAutomationService;
import dev.healthforge.platform.brief.BriefService;
import dev.healthforge.platform.integration.GovernedConnectorGateway;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentationExportService {

    private final BriefService briefService;
    private final JdbcTemplate jdbcTemplate;
    private final WorkflowAutomationService automationService;
    private final GovernedConnectorGateway connectorGateway;
    private final Clock clock = Clock.systemUTC();

    public DocumentationExportService(
            BriefService briefService,
            JdbcTemplate jdbcTemplate,
            WorkflowAutomationService automationService,
            GovernedConnectorGateway connectorGateway
    ) {
        this.briefService = briefService;
        this.jdbcTemplate = jdbcTemplate;
        this.automationService = automationService;
        this.connectorGateway = connectorGateway;
    }

    public DocumentationExportResponse export(
            DocumentationExportRequest request,
            AuthenticatedActor actor
    ) {
        var target = normalized(request.targetSystem());
        if (!List.of("confluence", "sharepoint", "notion").contains(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "target_system must be confluence, sharepoint, or notion");
        }
        if (!request.approvalAcknowledgement()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Explicit approval acknowledgement is required before packaging documentation-system exports.");
        }
        var format = normalized(request.packageFormat());
        if (!List.of("markdown", "html").contains(format)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "package_format must be markdown or html");
        }

        var brief = briefService.get(request.briefId(), actor);
        var export = briefService.exportWorkItems(request.briefId(), actor);
        var approval = request.approvalId() == null || request.approvalId().isBlank()
                ? brief.approvals().stream().reduce((first, second) -> second).orElse(null)
                : brief.approvals().stream().filter(item -> item.approvalId().equals(request.approvalId())).findFirst().orElse(null);

        var createdAt = Instant.now(clock);
        var mode = request.publishRequested() ? "governed_publish" : "preview_only";
        var blocked = request.publishRequested() && (approval == null || request.targetLocator() == null || request.targetLocator().isBlank());
        var publishOperation = request.targetLocator() != null && request.targetLocator().contains("/") ? "update" : "create";
        var traceability = List.of(
                "Brief: " + brief.briefId(),
                "Approvals: " + brief.approvals().size(),
                "Accepted work items: " + export.workItems().size(),
                "Corpus snapshot: " + brief.input().corpusId() + " / " + brief.input().corpusVersion()
        );
        var packageBody = packageBody(format, brief, export, request.exportReason());
        var connectorExecution = blocked || connectorGateway == null
                ? null
                : connectorGateway.executeDocumentation(target, request.targetLocator(), request.publishRequested(), 0);
        var status = blocked ? "publish_blocked"
                : connectorExecution == null ? request.publishRequested() ? "published" : "preview_generated"
                : connectorExecution.status();
        var externalReference = request.publishRequested() && !blocked
                ? connectorExecution == null
                    ? target + "://" + request.targetLocator() + "/approved-brief/sim-1"
                    : connectorExecution.externalReference()
                : null;

        jdbcTemplate.update("""
                insert into documentation_export_event (
                    documentation_export_event_id, brief_id, organization_id, actor_id, actor_role,
                    target_system, export_mode, package_format, approval_id, target_locator,
                    delivery_status, external_reference, trace_summary, retention_until, occurred_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "documentation_export_" + UUID.randomUUID(),
                brief.briefId(),
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                target,
                mode,
                format,
                approval == null ? null : approval.approvalId(),
                request.targetLocator(),
                status,
                externalReference,
                String.join(" | ", traceability),
                Timestamp.from(createdAt.plus(30, ChronoUnit.DAYS)),
                Timestamp.from(createdAt)
        );

        automationService.emit(
                brief.briefId(),
                actor,
                "documentation",
                status,
                "Documentation export prepared for " + target + " in " + format + " format.",
                "private_demo",
                request.publishRequested() && !blocked,
                null
        );

        return new DocumentationExportResponse(
                "documentation_export_" + UUID.randomUUID(),
                brief.briefId(),
                target,
                mode,
                format,
                publishOperation,
                createdAt,
                status,
                externalReference,
                traceability,
                packageBody,
                connectorExecution == null || connectorExecution.simulated(),
                connectorExecution == null ? "documentation_receipt" : connectorExecution.receiptType(),
                blocked
                        ? "Publishing was blocked until a valid approval record and target_locator are supplied."
                        : "The documentation package preserves evidence traceability and keeps direct publishing governed."
        );
    }

    private String packageBody(
            String format,
            dev.healthforge.platform.brief.BriefResponse brief,
            dev.healthforge.platform.brief.BriefWorkItemExportResponse export,
            String exportReason
    ) {
        var lines = new ArrayList<String>();
        lines.add("# HealthForge approved artifact package");
        lines.add("");
        lines.add("- Brief ID: " + brief.briefId());
        lines.add("- Status: " + brief.status());
        lines.add("- Question: " + brief.input().question());
        lines.add("- Project context: " + brief.input().projectContext());
        lines.add("- Export reason: " + (exportReason == null || exportReason.isBlank() ? "Not supplied." : exportReason));
        lines.add("");
        lines.add("## Approval trace");
        brief.approvals().forEach(approval -> lines.add("- " + approval.approvalId() + " by " + approval.approver() + " at " + approval.approvedAt()));
        lines.add("");
        lines.add("## Work items");
        export.workItems().forEach(item -> {
            lines.add("- " + item.title());
            lines.add("  - Track: " + item.primaryTrack());
            lines.add("  - Stage: " + item.workflowStage());
            lines.add("  - Evidence count: " + item.evidence().size());
        });
        lines.add("");
        lines.add("## Guardrails");
        lines.add("- This package stays review-first and does not authorize uncontrolled external publication.");
        lines.add("- Evidence remains traceable back to cited source snapshots and approval history.");
        var markdown = String.join("\n", lines);
        return "html".equals(format)
                ? "<html><body><pre>" + markdown.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</pre></body></html>"
                : markdown;
    }

    private String normalized(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
