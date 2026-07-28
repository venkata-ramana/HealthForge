package dev.healthforge.platform.collaboration;

import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.brief.BriefResponse;
import dev.healthforge.platform.brief.BriefService;
import dev.healthforge.platform.automation.WorkflowAutomationService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CollaborationNotificationService {

    private final BriefService briefService;
    private final JdbcTemplate jdbcTemplate;
    private final WorkflowAutomationService automationService;
    private final Clock clock = Clock.systemUTC();

    public CollaborationNotificationService(
            BriefService briefService,
            JdbcTemplate jdbcTemplate,
            WorkflowAutomationService automationService
    ) {
        this.briefService = briefService;
        this.jdbcTemplate = jdbcTemplate;
        this.automationService = automationService;
    }

    public CollaborationNotificationResponse notify(
            CollaborationNotificationRequest request,
            AuthenticatedActor actor
    ) {
        var target = normalized(request.targetSystem());
        if (!List.of("slack", "teams").contains(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "target_system must be slack or teams");
        }
        if (!request.approvalAcknowledgement()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Explicit approval acknowledgement is required before generating collaboration notifications.");
        }
        var type = normalized(request.notificationType());
        if (!List.of("review_ready", "approval_needed", "workflow_handoff").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "notification_type must be review_ready, approval_needed, or workflow_handoff");
        }

        var brief = briefService.get(request.briefId(), actor);
        var approval = request.approvalId() == null || request.approvalId().isBlank()
                ? null
                : brief.approvals().stream().filter(item -> item.approvalId().equals(request.approvalId())).findFirst().orElse(null);

        var occurredAt = Instant.now(clock);
        var deliveryMode = request.sendRequested() ? "governed_send" : "preview_only";
        var handoffRole = request.handoffRole() == null || request.handoffRole().isBlank()
                ? inferredHandoff(type, brief.status())
                : normalized(request.handoffRole());
        var blocked = request.sendRequested() && approval == null;
        var status = blocked ? "notification_blocked" : request.sendRequested() ? "notification_sent" : "preview_generated";
        var safeBullets = safeBullets(type, handoffRole, brief);
        var messageSummary = type.replace('_', ' ') + " notification for Brief " + brief.briefId()
                + " (" + brief.status() + ") targeting " + target + ".";
        var externalReference = request.sendRequested() && !blocked
                ? target + "://" + (request.targetLocator() == null || request.targetLocator().isBlank() ? "default-queue" : request.targetLocator())
                + "/" + type + "/sim-1"
                : null;

        jdbcTemplate.update("""
                insert into collaboration_notification_event (
                    collaboration_notification_event_id, brief_id, organization_id, actor_id, actor_role,
                    target_system, delivery_mode, notification_type, handoff_role, target_locator,
                    message_summary, approval_id, delivery_status, external_reference, retention_until, occurred_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "collaboration_notification_" + UUID.randomUUID(),
                brief.briefId(),
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                target,
                deliveryMode,
                type,
                handoffRole,
                request.targetLocator(),
                messageSummary,
                approval == null ? null : approval.approvalId(),
                status,
                externalReference,
                Timestamp.from(occurredAt.plus(30, ChronoUnit.DAYS)),
                Timestamp.from(occurredAt)
        );

        automationService.emit(
                brief.briefId(),
                actor,
                "collaboration",
                status,
                messageSummary,
                "private_demo",
                request.sendRequested() && !blocked,
                null
        );

        return new CollaborationNotificationResponse(
                "collaboration_notification_" + UUID.randomUUID(),
                brief.briefId(),
                target,
                type,
                deliveryMode,
                occurredAt,
                handoffRole,
                status,
                messageSummary,
                safeBullets,
                externalReference,
                blocked
                        ? "Governed collaboration delivery was blocked because no approval_id matched this Brief."
                        : "The message stays PHI-safe, bounded to queue-ready metadata, and traceable to the underlying Brief."
        );
    }

    private List<String> safeBullets(String type, String handoffRole, BriefResponse brief) {
        return List.of(
                "Question: " + brief.input().question(),
                "Status: " + brief.status(),
                "Queue owner: " + handoffRole,
                "Summary: " + switch (type) {
                    case "approval_needed" -> "Accepted findings are ready for final approver review.";
                    case "workflow_handoff" -> "Approved work items are ready for delivery planning handoff.";
                    default -> "The Brief is ready for reviewer attention.";
                }
        );
    }

    private String inferredHandoff(String type, String briefStatus) {
        return switch (type) {
            case "approval_needed" -> "approver";
            case "workflow_handoff" -> "administrator";
            default -> "reviewer";
        };
    }

    private String normalized(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
