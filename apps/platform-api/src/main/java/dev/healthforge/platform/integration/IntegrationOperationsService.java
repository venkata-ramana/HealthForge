package dev.healthforge.platform.integration;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class IntegrationOperationsService {

    private final JdbcTemplate jdbcTemplate;
    private final IntegrationProperties properties;
    private final Clock clock = Clock.systemUTC();

    public IntegrationOperationsService(JdbcTemplate jdbcTemplate, IntegrationProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    public IntegrationStatusResponse status(AuthenticatedActor actor) {
        var connectors = properties.all().entrySet().stream().map(entry -> {
            var connectorType = entry.getKey();
            var config = entry.getValue();
            var successCount = countByStatus(connectorType, List.of("live_execution", "simulated_execution", "published", "notification_sent", "writeback_executed", "writeback_retried"));
            var blockedCount = countByStatus(connectorType, List.of("connector_disabled", "publish_blocked", "notification_blocked", "writeback_blocked"));
            var retryCount = countByStatus(connectorType, List.of("simulated_retry", "live_retry", "retried", "writeback_retried"));
            return new IntegrationStatusResponse.ConnectorSummary(
                    connectorType,
                    config.isEnabled(),
                    config.getExecutionMode(),
                    config.isAllowLiveCalls(),
                    config.getBaseUrl(),
                    config.getCredentialReference(),
                    successCount,
                    blockedCount,
                    retryCount,
                    config.isEnabled()
                            ? "Connector is environment-managed and generates operator-visible receipts."
                            : "Connector stays preview-only until an operator enables it for the environment."
            );
        }).toList();
        return new IntegrationStatusResponse(
                actor.organizationId(),
                Instant.now(clock),
                connectors,
                recentReceipts(actor.organizationId()),
                retryQueue(actor.organizationId()),
                recoveryActions(actor.organizationId())
        );
    }

    public IntegrationStatusResponse.RecoveryAction recover(IntegrationRecoveryRequest request, AuthenticatedActor actor) {
        var previousStatus = currentStatus(request.sourceType(), request.sourceId(), actor.organizationId());
        var createdAt = Instant.now(clock);
        var actionId = "recovery_" + java.util.UUID.randomUUID();
        var outcome = previousStatus != null && previousStatus.contains("blocked") ? "retry_requested" : "inspected";
        var summary = "Operator requested " + request.requestedAction() + " for " + request.sourceType() + " " + request.sourceId() + ".";
        jdbcTemplate.update("""
                insert into integration_recovery_action (
                    recovery_action_id, organization_id, source_type, source_id, connector_type,
                    previous_status, requested_action, outcome_status, summary, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                actionId, actor.organizationId(), request.sourceType(), request.sourceId(), request.connectorType(),
                previousStatus, request.requestedAction(), outcome, summary, Timestamp.from(createdAt)
        );
        return new IntegrationStatusResponse.RecoveryAction(
                actionId, request.sourceType(), request.sourceId(), request.connectorType(),
                previousStatus, request.requestedAction(), outcome, summary, createdAt
        );
    }

    private int countByStatus(String connectorType, List<String> statuses) {
        var trackerStatuses = jdbcTemplate.queryForList("""
                select execution_status from tracked_export_event where target_system = ?
                """, String.class, connectorType);
        var documentationStatuses = jdbcTemplate.queryForList("""
                select delivery_status from documentation_export_event where target_system = ?
                """, String.class, connectorType);
        return (int) java.util.stream.Stream.concat(trackerStatuses.stream(), documentationStatuses.stream())
                .filter(status -> status != null && statuses.contains(status))
                .count();
    }

    private List<IntegrationStatusResponse.DeliveryReceipt> recentReceipts(String organizationId) {
        var receipts = new ArrayList<IntegrationStatusResponse.DeliveryReceipt>();
        receipts.addAll(jdbcTemplate.query("""
                select tracked_export_event_id as id, target_system, brief_id, execution_status, target_locator, external_reference, occurred_at
                from tracked_export_event where organization_id = ? order by occurred_at desc limit 10
                """, (rs, row) -> new IntegrationStatusResponse.DeliveryReceipt(
                rs.getString("id"), "tracker_receipt", rs.getString("target_system"), rs.getString("brief_id"),
                rs.getString("execution_status"), rs.getString("target_locator"), rs.getString("external_reference"),
                rs.getTimestamp("occurred_at").toInstant()
        ), organizationId));
        receipts.addAll(jdbcTemplate.query("""
                select documentation_export_event_id as id, target_system, brief_id, delivery_status, target_locator, external_reference, occurred_at
                from documentation_export_event where organization_id = ? order by occurred_at desc limit 10
                """, (rs, row) -> new IntegrationStatusResponse.DeliveryReceipt(
                rs.getString("id"), "documentation_receipt", rs.getString("target_system"), rs.getString("brief_id"),
                rs.getString("delivery_status"), rs.getString("target_locator"), rs.getString("external_reference"),
                rs.getTimestamp("occurred_at").toInstant()
        ), organizationId));
        receipts.sort((left, right) -> right.occurredAt().compareTo(left.occurredAt()));
        return receipts.stream().limit(16).toList();
    }

    private List<IntegrationStatusResponse.RetryQueueItem> retryQueue(String organizationId) {
        var items = new ArrayList<IntegrationStatusResponse.RetryQueueItem>();
        items.addAll(jdbcTemplate.query("""
                select tracked_export_event_id, target_system, execution_status, target_locator
                from tracked_export_event
                where organization_id = ? and execution_status in ('writeback_blocked', 'simulated_retry', 'live_retry')
                order by occurred_at desc
                """, (rs, row) -> new IntegrationStatusResponse.RetryQueueItem(
                "tracked_export_event", rs.getString("tracked_export_event_id"), rs.getString("target_system"),
                rs.getString("execution_status"), rs.getString("target_locator"),
                "Retry the governed export after confirming approval and connector policy."
        ), organizationId));
        items.addAll(jdbcTemplate.query("""
                select documentation_export_event_id, target_system, delivery_status, target_locator
                from documentation_export_event
                where organization_id = ? and delivery_status in ('publish_blocked')
                order by occurred_at desc
                """, (rs, row) -> new IntegrationStatusResponse.RetryQueueItem(
                "documentation_export_event", rs.getString("documentation_export_event_id"), rs.getString("target_system"),
                rs.getString("delivery_status"), rs.getString("target_locator"),
                "Retry publishing after confirming approval, publish target, and connector readiness."
        ), organizationId));
        return items;
    }

    private List<IntegrationStatusResponse.RecoveryAction> recoveryActions(String organizationId) {
        return jdbcTemplate.query("""
                select recovery_action_id, source_type, source_id, connector_type, previous_status,
                       requested_action, outcome_status, summary, created_at
                from integration_recovery_action
                where organization_id = ?
                order by created_at desc
                limit 12
                """, (rs, row) -> new IntegrationStatusResponse.RecoveryAction(
                rs.getString("recovery_action_id"),
                rs.getString("source_type"),
                rs.getString("source_id"),
                rs.getString("connector_type"),
                rs.getString("previous_status"),
                rs.getString("requested_action"),
                rs.getString("outcome_status"),
                rs.getString("summary"),
                rs.getTimestamp("created_at").toInstant()
        ), organizationId);
    }

    private String currentStatus(String sourceType, String sourceId, String organizationId) {
        if ("tracked_export_event".equals(sourceType)) {
            var rows = jdbcTemplate.queryForList("""
                    select execution_status from tracked_export_event where tracked_export_event_id = ? and organization_id = ?
                    """, String.class, sourceId, organizationId);
            return rows.isEmpty() ? null : rows.getFirst();
        }
        if ("documentation_export_event".equals(sourceType)) {
            var rows = jdbcTemplate.queryForList("""
                    select delivery_status from documentation_export_event where documentation_export_event_id = ? and organization_id = ?
                    """, String.class, sourceId, organizationId);
            return rows.isEmpty() ? null : rows.getFirst();
        }
        return null;
    }
}
