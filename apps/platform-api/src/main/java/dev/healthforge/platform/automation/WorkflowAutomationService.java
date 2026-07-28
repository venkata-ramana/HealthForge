package dev.healthforge.platform.automation;

import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.brief.BriefAuditEventService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WorkflowAutomationService {

    private final JdbcTemplate jdbcTemplate;
    private final BriefAuditEventService auditEventService;
    private final Clock clock = Clock.systemUTC();

    public WorkflowAutomationService(
            JdbcTemplate jdbcTemplate,
            BriefAuditEventService auditEventService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditEventService = auditEventService;
    }

    public WorkflowAutomationSubscriptionResponse configureSubscription(
            WorkflowAutomationSubscriptionRequest request,
            AuthenticatedActor actor
    ) {
        var subscriptionId = existingSubscriptionId(
                actor.organizationId(),
                request.eventFamily(),
                request.eventName(),
                request.environmentScope(),
                request.targetLabel()
        );
        var now = Instant.now(clock);
        if (subscriptionId == null) {
            subscriptionId = "workflow_subscription_" + UUID.randomUUID();
            jdbcTemplate.update("""
                    insert into workflow_event_subscription (
                        workflow_event_subscription_id, organization_id, event_family, event_name,
                        environment_scope, target_label, delivery_mode, enabled, created_at, updated_at
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    subscriptionId,
                    actor.organizationId(),
                    normalized(request.eventFamily()),
                    normalized(request.eventName()),
                    normalized(request.environmentScope()),
                    request.targetLabel().trim(),
                    normalized(request.deliveryMode()),
                    request.enabled(),
                    Timestamp.from(now),
                    Timestamp.from(now)
            );
        } else {
            jdbcTemplate.update("""
                    update workflow_event_subscription
                    set delivery_mode = ?, enabled = ?, updated_at = ?
                    where workflow_event_subscription_id = ? and organization_id = ?
                    """,
                    normalized(request.deliveryMode()),
                    request.enabled(),
                    Timestamp.from(now),
                    subscriptionId,
                    actor.organizationId()
            );
        }
        return new WorkflowAutomationSubscriptionResponse(
                subscriptionId,
                actor.organizationId(),
                normalized(request.eventFamily()),
                normalized(request.eventName()),
                normalized(request.environmentScope()),
                request.targetLabel().trim(),
                normalized(request.deliveryMode()),
                request.enabled(),
                now
        );
    }

    public WorkflowAutomationDispatchResponse dispatch(
            WorkflowAutomationDispatchRequest request,
            AuthenticatedActor actor
    ) {
        return emit(
                request.briefId(),
                actor,
                request.eventFamily(),
                request.eventName(),
                request.payloadSummary(),
                request.environmentScope(),
                request.webhookRequested(),
                request.retryFromDeliveryId()
        );
    }

    public WorkflowAutomationDispatchResponse emit(
            String briefId,
            AuthenticatedActor actor,
            String eventFamily,
            String eventName,
            String payloadSummary,
            String environmentScope,
            boolean webhookRequested,
            String retryFromDeliveryId
    ) {
        var occurredAt = Instant.now(clock);
        var workflowEventId = "workflow_event_" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into workflow_event (
                    workflow_event_id, organization_id, brief_id, actor_id, actor_role,
                    event_family, event_name, payload_summary, environment_scope, occurred_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                workflowEventId,
                actor.organizationId(),
                briefId,
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                normalized(eventFamily),
                normalized(eventName),
                payloadSummary,
                normalized(environmentScope),
                Timestamp.from(occurredAt)
        );

        var subscriptions = subscriptions(actor.organizationId(), eventFamily, eventName, environmentScope);
        var deliveries = subscriptions.stream()
                .map(subscription -> createDelivery(workflowEventId, actor.organizationId(), subscription, webhookRequested, retryFromDeliveryId, occurredAt))
                .toList();

        if (briefId != null && !briefId.isBlank()) {
            auditEventService.record(
                    briefId,
                    actor,
                    "workflow_event_emitted",
                    "Emitted governed workflow event '" + normalized(eventFamily) + "." + normalized(eventName) + "'.",
                    "deliveries=" + deliveries.size() + ", environment_scope=" + normalized(environmentScope)
            );
        }

        return new WorkflowAutomationDispatchResponse(
                workflowEventId,
                actor.organizationId(),
                normalized(eventFamily),
                normalized(eventName),
                normalized(environmentScope),
                occurredAt,
                deliveries,
                deliveries.isEmpty()
                        ? "No enabled webhook subscriptions matched this event. The event was still recorded for audit and operator review."
                        : "Recorded the workflow event and produced " + deliveries.size() + " governed webhook delivery records."
        );
    }

    public WorkflowAutomationStatusResponse status(AuthenticatedActor actor) {
        var recentEvents = jdbcTemplate.query("""
                select workflow_event_id, brief_id, event_family, event_name, environment_scope, occurred_at, payload_summary
                from workflow_event
                where organization_id = ?
                order by occurred_at desc
                limit 12
                """, (rs, row) -> new WorkflowAutomationStatusResponse.RecentEvent(
                rs.getString("workflow_event_id"),
                rs.getString("brief_id"),
                rs.getString("event_family"),
                rs.getString("event_name"),
                rs.getString("environment_scope"),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getString("payload_summary")
        ), actor.organizationId());

        var recentDeliveries = jdbcTemplate.query("""
                select outbound_webhook_delivery_id, workflow_event_id, target_label, delivery_mode,
                       delivery_status, retry_count, last_attempt_at, external_reference
                from outbound_webhook_delivery
                where organization_id = ?
                order by last_attempt_at desc
                limit 20
                """, (rs, row) -> new WorkflowAutomationStatusResponse.RecentDelivery(
                rs.getString("outbound_webhook_delivery_id"),
                rs.getString("workflow_event_id"),
                rs.getString("target_label"),
                rs.getString("delivery_mode"),
                rs.getString("delivery_status"),
                rs.getInt("retry_count"),
                rs.getTimestamp("last_attempt_at").toInstant(),
                rs.getString("external_reference")
        ), actor.organizationId());

        return new WorkflowAutomationStatusResponse(actor.organizationId(), recentEvents, recentDeliveries);
    }

    private WorkflowAutomationDispatchResponse.Delivery createDelivery(
            String workflowEventId,
            String organizationId,
            Subscription subscription,
            boolean webhookRequested,
            String retryFromDeliveryId,
            Instant occurredAt
    ) {
        var prior = retryFromDeliveryId == null || retryFromDeliveryId.isBlank()
                ? null
                : priorDelivery(organizationId, retryFromDeliveryId);
        var retryCount = prior == null ? 0 : prior.retryCount() + 1;
        var deliveryId = "webhook_delivery_" + UUID.randomUUID();
        var status = !webhookRequested
                ? "preview_generated"
                : retryCount == 0 ? "delivered" : "retried";
        var responseSummary = !webhookRequested
                ? "Webhook delivery was previewed only. Operators can enable explicit send mode when ready."
                : "Webhook delivery executed through the local enterprise-safe stub and remains ready for a real connector."
                        + (retryCount == 0 ? "" : " Retry count " + retryCount + " is preserved.");
        var externalReference = webhookRequested
                ? "webhook://" + subscription.targetLabel().replaceAll("[^a-zA-Z0-9._-]+", "-")
                + "/" + subscription.eventFamily() + "." + subscription.eventName()
                + "/" + (retryCount == 0 ? "sim-1" : "retry-" + retryCount)
                : null;

        jdbcTemplate.update("""
                insert into outbound_webhook_delivery (
                    outbound_webhook_delivery_id, workflow_event_id, organization_id, target_label,
                    delivery_mode, delivery_status, retry_count, response_summary, external_reference, last_attempt_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                deliveryId,
                workflowEventId,
                organizationId,
                subscription.targetLabel(),
                webhookRequested ? subscription.deliveryMode() : "preview_only",
                status,
                retryCount,
                responseSummary,
                externalReference,
                Timestamp.from(occurredAt)
        );

        return new WorkflowAutomationDispatchResponse.Delivery(
                deliveryId,
                subscription.targetLabel(),
                webhookRequested ? subscription.deliveryMode() : "preview_only",
                status,
                retryCount,
                responseSummary,
                externalReference,
                occurredAt
        );
    }

    private List<Subscription> subscriptions(
            String organizationId,
            String eventFamily,
            String eventName,
            String environmentScope
    ) {
        return jdbcTemplate.query("""
                select event_family, event_name, environment_scope, target_label, delivery_mode
                from workflow_event_subscription
                where organization_id = ?
                  and event_family = ?
                  and event_name = ?
                  and environment_scope = ?
                  and enabled = true
                order by target_label
                """, (rs, row) -> new Subscription(
                rs.getString("event_family"),
                rs.getString("event_name"),
                rs.getString("environment_scope"),
                rs.getString("target_label"),
                rs.getString("delivery_mode")
        ), organizationId, normalized(eventFamily), normalized(eventName), normalized(environmentScope));
    }

    private DeliveryRetry priorDelivery(String organizationId, String retryFromDeliveryId) {
        var rows = jdbcTemplate.query("""
                select outbound_webhook_delivery_id, retry_count
                from outbound_webhook_delivery
                where organization_id = ? and outbound_webhook_delivery_id = ?
                """, (rs, row) -> new DeliveryRetry(rs.getString("outbound_webhook_delivery_id"), rs.getInt("retry_count")),
                organizationId, retryFromDeliveryId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private String existingSubscriptionId(
            String organizationId,
            String eventFamily,
            String eventName,
            String environmentScope,
            String targetLabel
    ) {
        var ids = jdbcTemplate.query("""
                select workflow_event_subscription_id
                from workflow_event_subscription
                where organization_id = ?
                  and event_family = ?
                  and event_name = ?
                  and environment_scope = ?
                  and target_label = ?
                """, (rs, row) -> rs.getString("workflow_event_subscription_id"),
                organizationId,
                normalized(eventFamily),
                normalized(eventName),
                normalized(environmentScope),
                targetLabel.trim());
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private String normalized(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    record Subscription(
            String eventFamily,
            String eventName,
            String environmentScope,
            String targetLabel,
            String deliveryMode
    ) {
    }

    private record DeliveryRetry(String deliveryId, int retryCount) {
    }
}
