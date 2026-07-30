package dev.healthforge.platform.integration;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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
        var receipts = recentReceipts(actor.organizationId());
        var retries = retryQueue(actor.organizationId());
        var recoveries = recoveryActions(actor.organizationId());
        return new IntegrationStatusResponse(
                actor.organizationId(),
                Instant.now(clock),
                connectors,
                receipts,
                retries,
                recoveries,
                reconciliationSummary(receipts),
                connectorDrilldowns(connectors, actor.organizationId()),
                environmentPolicies()
        );
    }

    public IntegrationAuditExportResponse auditExport(AuthenticatedActor actor) {
        var status = status(actor);
        return new IntegrationAuditExportResponse(
                actor.organizationId(),
                Instant.now(clock),
                status.reconciliationSummary(),
                status.connectors(),
                status.connectorDrilldowns(),
                status.environmentPolicies(),
                status.recentReceipts(),
                status.retryQueue(),
                status.recoveryActions(),
                List.of(
                        "This export is an operator-facing audit packet for governed delivery visibility.",
                        "Receipts and recovery actions remain bounded to local platform records and environment-managed connectors.",
                        "Live-capable execution still depends on explicit approval, connector enablement, and environment policy."
                )
        );
    }

    public IntegrationGovernanceCheckResponse governanceCheck(IntegrationGovernanceCheckRequest request, AuthenticatedActor actor) {
        var connectorType = request.connectorType().trim().toLowerCase();
        var connector = properties.connector(connectorType);
        if (connector == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported connector type.");
        }
        var actionType = request.actionType().trim().toLowerCase();
        var approvalGate = approvalGate(actor.organizationId(), request.briefId(), request.approvalId());
        var validApproval = approvalGate != null && "approval_found".equals(approvalGate.status());
        var targetLocator = request.targetLocator() == null ? null : request.targetLocator().trim();
        var requestedMode = request.requestedExecutionMode() == null || request.requestedExecutionMode().isBlank()
                ? connector.getExecutionMode()
                : request.requestedExecutionMode().trim().toLowerCase();
        var liveAllowed = connector.isEnabled()
                && connector.isAllowLiveCalls()
                && "live".equals(normalizedMode(connector))
                && validApproval
                && targetLocator != null
                && !targetLocator.isBlank();
        var finalDecision = liveAllowed
                ? "live_execution_permitted"
                : connector.isEnabled()
                    ? "governed_preview_only"
                    : "connector_disabled";
        return new IntegrationGovernanceCheckResponse(
                actor.organizationId(),
                Instant.now(clock),
                connectorType,
                actionType,
                requestedMode,
                finalDecision,
                liveAllowed,
                approvalGate == null
                        ? new IntegrationGovernanceCheckResponse.ApprovalGateSummary(
                                "approval_missing",
                                request.approvalId(),
                                null,
                                null,
                                null,
                                "A valid approval record is required before governed live-capable execution can proceed."
                        )
                        : approvalGate,
                new IntegrationGovernanceCheckResponse.EnvironmentPolicySummary(
                        connector.isEnabled(),
                        normalizedMode(connector),
                        connector.isAllowLiveCalls(),
                        targetLocator,
                        connector.isEnabled()
                                ? connector.isAllowLiveCalls()
                                    ? "Connector is enabled with explicit execution posture controlled by environment policy."
                                    : "Connector is enabled but live calls are disabled by environment policy."
                                : "Connector is disabled for this environment and remains preview-only."
                ),
                operatorActions(connectorType, liveAllowed, validApproval, targetLocator),
                List.of(
                        "Approval traceability stays mandatory for governed live-capable actions.",
                        "Environment mode determines whether execution remains simulated or can use a live adapter path.",
                        "Operators should review receipts and retry posture after any governed delivery action."
                )
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

    private IntegrationStatusResponse.ReconciliationSummary reconciliationSummary(List<IntegrationStatusResponse.DeliveryReceipt> receipts) {
        var total = receipts.size();
        var successful = (int) receipts.stream().filter(item -> List.of("published", "writeback_executed", "writeback_retried", "live_execution", "live_retry", "simulated_execution", "simulated_retry").contains(item.status())).count();
        var blocked = (int) receipts.stream().filter(item -> item.status().contains("blocked") || item.status().contains("disabled")).count();
        var retrying = (int) receipts.stream().filter(item -> item.status().contains("retry")).count();
        var simulated = (int) receipts.stream().filter(item -> item.status().contains("simulated")).count();
        var live = (int) receipts.stream().filter(item -> item.status().contains("live")).count();
        return new IntegrationStatusResponse.ReconciliationSummary(
                total,
                successful,
                blocked,
                retrying,
                simulated,
                live,
                blocked > 0
                        ? "Blocked governed deliveries need operator review before downstream status is considered healthy."
                        : "Recent delivery receipts show a bounded governed-delivery story with visible execution outcomes."
        );
    }

    private List<IntegrationStatusResponse.ConnectorDrilldown> connectorDrilldowns(List<IntegrationStatusResponse.ConnectorSummary> connectors, String organizationId) {
        return connectors.stream().map(connector -> new IntegrationStatusResponse.ConnectorDrilldown(
                connector.connectorType(),
                connector.executionMode(),
                connector.liveCapable(),
                connector.enabled(),
                connector.liveCapable()
                        ? "Approved, target-qualified actions may use the live adapter path."
                        : "Connector remains simulated or disabled until environment policy allows live execution.",
                connector.operatorSummary(),
                recentStatusesForConnector(connector.connectorType(), organizationId),
                connector.enabled()
                        ? List.of(
                                "Inspect latest receipts before retrying blocked work.",
                                "Confirm approval trace exists for governed writeback or publishing actions."
                        )
                        : List.of("Leave connector preview-only until an operator enables it for the target environment.")
        )).toList();
    }

    private List<IntegrationStatusResponse.EnvironmentPolicySummary> environmentPolicies() {
        return properties.all().entrySet().stream().map(entry -> {
            var connectorType = entry.getKey();
            var connector = entry.getValue();
            return new IntegrationStatusResponse.EnvironmentPolicySummary(
                    connectorType,
                    normalizedMode(connector),
                    connector.isAllowLiveCalls(),
                    connector.isEnabled()
                            ? connector.isAllowLiveCalls()
                                ? "governed_live_capable"
                                : "enabled_but_simulated"
                            : "disabled_preview_only",
                    connector.isEnabled()
                            ? List.of(
                                    "Confirm target locator, approval gate, and receipt visibility before governed delivery.",
                                    "Rehearse retry or rollback posture before treating connector execution as operationally ready."
                            )
                            : List.of(
                                    "Connector stays preview-only in this environment.",
                                    "Operators can still use payload previews and audit exports without enabling execution."
                            )
            );
        }).toList();
    }

    private List<String> recentStatusesForConnector(String connectorType, String organizationId) {
        var statuses = new ArrayList<String>();
        statuses.addAll(jdbcTemplate.queryForList("""
                select execution_status from tracked_export_event
                where organization_id = ? and target_system = ?
                order by occurred_at desc
                limit 3
                """, String.class, organizationId, connectorType));
        statuses.addAll(jdbcTemplate.queryForList("""
                select delivery_status from documentation_export_event
                where organization_id = ? and target_system = ?
                order by occurred_at desc
                limit 3
                """, String.class, organizationId, connectorType));
        return statuses.isEmpty() ? List.of("no_recent_receipts") : statuses.stream().limit(4).toList();
    }

    private IntegrationGovernanceCheckResponse.ApprovalGateSummary approvalGate(String organizationId, String briefId, String approvalId) {
        if (approvalId == null || approvalId.isBlank() || briefId == null || briefId.isBlank()) {
            return null;
        }
        var rows = jdbcTemplate.queryForList("""
                select approval_id, approver, approver_role, approved_at
                from brief_approval
                where organization_id = ? and brief_id = ? and approval_id = ?
                """, organizationId, briefId, approvalId);
        if (rows.isEmpty()) {
            return new IntegrationGovernanceCheckResponse.ApprovalGateSummary(
                    "approval_not_found",
                    approvalId,
                    null,
                    null,
                    null,
                    "The requested approval id was not found for this Brief in the current organization."
            );
        }
        var row = rows.getFirst();
        return new IntegrationGovernanceCheckResponse.ApprovalGateSummary(
                String.valueOf("approval_found"),
                String.valueOf(row.get("approval_id")),
                String.valueOf(row.get("approver")),
                String.valueOf(row.get("approver_role")),
                row.get("approved_at") instanceof Timestamp timestamp ? timestamp.toInstant() : null,
                "A valid approval record exists for governed execution checks."
        );
    }

    private List<String> operatorActions(String connectorType, boolean liveAllowed, boolean validApproval, String targetLocator) {
        var actions = new ArrayList<String>();
        if (!validApproval) {
            actions.add("Record or supply a valid approval id before attempting governed live-capable delivery.");
        }
        if (targetLocator == null || targetLocator.isBlank()) {
            actions.add("Supply a target locator so downstream receipts can be reconciled to a concrete destination.");
        }
        if (liveAllowed) {
            actions.add("Proceed with governed execution and then inspect receipts plus retry posture.");
        } else {
            actions.add("Use preview or simulated execution until environment posture and approvals align.");
        }
        actions.add("Review connector " + connectorType + " audit export before promoting changes in delivery posture.");
        return actions;
    }

    private String normalizedMode(IntegrationProperties.Connector connector) {
        return connector.getExecutionMode() == null ? "simulated" : connector.getExecutionMode().trim().toLowerCase();
    }
}
