package dev.healthforge.platform.trackerexport;

import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.brief.BriefAuditEventService;
import dev.healthforge.platform.brief.BriefService;
import dev.healthforge.platform.brief.BriefWorkItemExportResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TrackedWorkItemExportService {

    private final BriefService briefService;
    private final BriefAuditEventService auditEventService;
    private final Clock clock = Clock.systemUTC();

    public TrackedWorkItemExportService(
            BriefService briefService,
            BriefAuditEventService auditEventService
    ) {
        this.briefService = briefService;
        this.auditEventService = auditEventService;
    }

    public TrackedWorkItemExportResponse preview(TrackedWorkItemExportRequest request, AuthenticatedActor actor) {
        var target = request.targetSystem().trim().toLowerCase(Locale.ROOT);
        if (!List.of("github", "jira").contains(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "target_system must be github or jira");
        }
        if (!request.approvalAcknowledgement()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Explicit approval acknowledgement is required before generating tracker-ready export previews.");
        }
        if (request.writebackRequested()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Direct writeback is not enabled in this phase. Preview mode only.");
        }

        var export = briefService.exportWorkItems(request.briefId());
        var workItemIds = request.workItemIds() == null ? List.<String>of() : request.workItemIds();
        var selectedItems = export.workItems().stream()
                .filter(item -> workItemIds.isEmpty() || workItemIds.contains(item.workItemId()))
                .toList();
        if (selectedItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No approved work items matched the requested export preview.");
        }

        auditEventService.record(request.briefId(), actor, "tracker_export_preview_generated",
                "Generated " + target + " tracker-ready preview payloads from approved work items.",
                "target=" + target + ", work_items=" + selectedItems.size() + ", writeback=false");

        return new TrackedWorkItemExportResponse(
                "tracked_export_" + UUID.randomUUID(),
                request.briefId(),
                target,
                "preview_only",
                Instant.now(clock),
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                selectedItems.stream().map(item -> toPreviewItem(item, target)).toList(),
                List.of(
                        "This phase does not perform direct GitHub or Jira writeback.",
                        "Provider credentials, access tokens, and permission models remain outside source control.",
                        "Human approval, audit review, and downstream backlog curation remain required before creating external tickets."
                ),
                false,
                "This integration layer produces tracked preview payloads only. Human review and explicit approval remain mandatory before any external tracker action."
        );
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
        lines.add("Human review status: " + item.humanReviewStatus());
        lines.add("");
        lines.add("Summary:");
        lines.add(item.rationale());
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
        lines.add("Non-goal: this preview does not create an external ticket automatically.");
        return String.join("\n", lines);
    }

    private List<String> labels(BriefWorkItemExportResponse.WorkItem item) {
        var labels = new LinkedHashSet<String>();
        labels.add("healthforge");
        labels.add("reviewed-brief");
        labels.add(item.affectedCapability().replace('_', '-'));
        item.standardsTouchpoints().stream()
                .limit(2)
                .map(touchpoint -> touchpoint.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-"))
                .forEach(labels::add);
        return labels.stream().toList();
    }
}
