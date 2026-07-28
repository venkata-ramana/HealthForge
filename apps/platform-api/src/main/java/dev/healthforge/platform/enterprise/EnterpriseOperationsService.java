package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class EnterpriseOperationsService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditPolicyProperties auditPolicyProperties;
    private final OperationsPolicyProperties operationsPolicyProperties;
    private final Clock clock = Clock.systemUTC();

    public EnterpriseOperationsService(
            JdbcTemplate jdbcTemplate,
            AuditPolicyProperties auditPolicyProperties,
            OperationsPolicyProperties operationsPolicyProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditPolicyProperties = auditPolicyProperties;
        this.operationsPolicyProperties = operationsPolicyProperties;
    }

    public OperationsConfigurationResponse configuration(AuthenticatedActor actor) {
        return new OperationsConfigurationResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                operationsPolicyProperties.getDeploymentTier(),
                operationsPolicyProperties.getEnvironments().stream()
                        .map(environment -> new OperationsConfigurationResponse.EnvironmentPolicyView(
                                environment.getEnvironmentName(),
                                environment.getPromotionGate(),
                                environment.getSecretBoundary(),
                                environment.getChangeWindow(),
                                environment.getDataBoundary()
                        ))
                        .toList(),
                operationsPolicyProperties.getConfigBoundaries().stream()
                        .map(boundary -> new OperationsConfigurationResponse.ConfigBoundaryView(
                                boundary.getKey(),
                                boundary.getClassification(),
                                boundary.getSource(),
                                boundary.getExposurePolicy(),
                                boundary.getRationale()
                        ))
                        .toList(),
                operationsPolicyProperties.getSecretReferences().stream()
                        .map(reference -> new OperationsConfigurationResponse.SecretReferenceView(
                                reference.getSystem(),
                                reference.getReference(),
                                reference.getRotationExpectation(),
                                reference.getUsageBoundary()
                        ))
                        .toList(),
                List.of(
                        "Configuration is explained through classified summaries and secret references rather than raw values.",
                        "Environment promotion is expected to change only environment-scoped configuration, not application behavior embedded in source control.",
                        "Credential references remain private-deployment concerns and are never stored as readable secrets inside the product."
                ),
                "This view explains which runtime assumptions matter, which settings are environment-bound, and where secret-backed dependencies sit without exposing sensitive values."
        );
    }

    public OperationsObservabilityResponse observability(AuthenticatedActor actor) {
        var organizationId = actor.organizationId();
        return new OperationsObservabilityResponse(
                organizationId,
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                new OperationsObservabilityResponse.HealthSignals(
                        count("""
                                select count(*) from engineering_brief
                                where organization_id = ? and created_at >= ?
                                """, organizationId, timestamp(daysAgo(30))),
                        count("""
                                select count(*) from answer_generation_event
                                where organization_id = ? and created_at >= ?
                                """, organizationId, timestamp(daysAgo(30))),
                        count("""
                                select count(*) from fhir_validation_run
                                where organization_id = ? and validation_status = 'invalid' and created_at >= ?
                                """, organizationId, timestamp(daysAgo(30))),
                        count("""
                                select count(*) from tracked_export_event
                                where organization_id = ? and execution_status in ('writeback_blocked', 'simulated_retry', 'live_retry')
                                """, organizationId),
                        count("""
                                select count(*) from engineering_brief
                                where organization_id = ? and status = 'changes_requested'
                                """, organizationId),
                        instant("""
                                select max(occurred_at) from brief_audit_event where organization_id = ?
                                """, organizationId)
                ),
                new OperationsObservabilityResponse.RetentionSignals(
                        auditPolicyProperties.trackedExportRetentionDays(),
                        auditPolicyProperties.validationTelemetryRetention(),
                        auditPolicyProperties.auditEvidenceRetention(),
                        count("""
                                select count(*) from operations_attestation where organization_id = ?
                                """, organizationId),
                        instant("""
                                select max(created_at) from operations_attestation where organization_id = ?
                                """, organizationId)
                ),
                operationsPolicyProperties.getObservabilityRunbooks(),
                buildObservabilityNarratives(),
                "This operator view summarizes workflow pressure, incident-oriented signals, and retention posture without requiring direct database or infrastructure inspection."
        );
    }

    public OperationsContinuityResponse continuity(AuthenticatedActor actor) {
        var organizationId = actor.organizationId();
        return new OperationsContinuityResponse(
                organizationId,
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                new OperationsContinuityResponse.ContinuityInventory(
                        count("select count(*) from engineering_brief where organization_id = ?", organizationId),
                        count("select count(*) from brief_audit_event where organization_id = ?", organizationId),
                        count("select count(*) from tracked_export_event where organization_id = ?", organizationId),
                        count("select count(*) from fhir_validation_run where organization_id = ?", organizationId),
                        string("select max(version) from flyway_schema_history where success = true"),
                        instant("select max(created_at) from engineering_brief where organization_id = ?", organizationId)
                ),
                List.of(
                        "Back up the Postgres volume, environment-specific configuration, and deployment manifests together so auditability and runtime policy stay aligned.",
                        "Keep the current image tag, migration version, and connector mode settings with each backup set.",
                        "Export synthetic demo fixtures separately from operator evidence so rehearsal data can be refreshed without touching governance history."
                ),
                List.of(
                        "Restore into an isolated environment first and confirm the latest flyway version, organization counts, and approval history are present.",
                        "Verify at least one Brief, one approval, one audit event, and one tracked export can still be read after restore.",
                        "Confirm secret references resolve through the target environment before enabling governed connectors."
                ),
                operationsPolicyProperties.getContinuityChecks(),
                List.of(
                        "Rehearse rollback with connectors in preview-only mode before rehearsing live-capable delivery paths.",
                        "Document how to pause outbound delivery, replay blocked work, and preserve audit evidence for a post-incident review.",
                        "Treat migration validation as both a data check and an operator explainability check for enterprise demos."
                ),
                "This continuity view turns backup, restore, migration, and recovery expectations into a repeatable checklist rather than tribal knowledge."
        );
    }

    public OperationsUsageResponse usage(AuthenticatedActor actor) {
        var organizationId = actor.organizationId();
        var briefsLast30Days = count("""
                select count(*) from engineering_brief
                where organization_id = ? and created_at >= ?
                """, organizationId, timestamp(daysAgo(30)));
        var answersLast30Days = count("""
                select count(*) from answer_generation_event
                where organization_id = ? and created_at >= ?
                """, organizationId, timestamp(daysAgo(30)));
        var validationsLast30Days = count("""
                select count(*) from fhir_validation_run
                where organization_id = ? and created_at >= ?
                """, organizationId, timestamp(daysAgo(30)));
        var trackedExportsLast30Days = count("""
                select count(*) from tracked_export_event
                where organization_id = ? and occurred_at >= ?
                """, organizationId, timestamp(daysAgo(30)));
        var inboundCasesLast30Days = count("""
                select count(*) from inbound_case
                where organization_id = ? and created_at >= ?
                """, organizationId, timestamp(daysAgo(30)));

        return new OperationsUsageResponse(
                organizationId,
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                new OperationsUsageResponse.UsageSummary(
                        count("select count(*) from actor_organization"),
                        briefsLast30Days,
                        answersLast30Days,
                        validationsLast30Days,
                        trackedExportsLast30Days,
                        inboundCasesLast30Days
                ),
                operationsPolicyProperties.getQuotas().stream()
                        .map(quota -> new OperationsUsageResponse.QuotaStatus(
                                quota.getMetric(),
                                observed(quota.getMetric(), briefsLast30Days, answersLast30Days, validationsLast30Days, trackedExportsLast30Days, inboundCasesLast30Days),
                                quota.getSoftLimit(),
                                quota.getWindow(),
                                quotaStatus(observed(quota.getMetric(), briefsLast30Days, answersLast30Days, validationsLast30Days, trackedExportsLast30Days, inboundCasesLast30Days), quota.getSoftLimit()),
                                quota.getRationale()
                        ))
                        .toList(),
                List.of(
                        new OperationsUsageResponse.CostSignal(
                                "Workflow activity concentration",
                                briefsLast30Days + " briefs / " + answersLast30Days + " answers in the last 30 days",
                                "This is a lightweight proxy for operator attention and AI-assisted workflow demand by organization."
                        ),
                        new OperationsUsageResponse.CostSignal(
                                "Governed delivery pressure",
                                trackedExportsLast30Days + " tracked exports in the last 30 days",
                                "Delivery volume is a practical cost-control signal because it predicts downstream coordination, retries, and governance overhead."
                        ),
                        new OperationsUsageResponse.CostSignal(
                                "Validation load",
                                validationsLast30Days + " validation runs in the last 30 days",
                                "Validation volume helps explain synthetic standards-testing demand and future package/runtime expansion pressure."
                        )
                ),
                List.of(
                        "Usage remains organization scoped and demo safe; it does not expose PHI, document contents, or connector secrets.",
                        "Soft quotas are advisory guardrails for private deployments rather than hard billing enforcement in this phase.",
                        "These signals create a clearer enterprise story for future commercial packaging and tenant-aware operations."
                ),
                "This view helps operators explain current workload, soft limits, and cost-related signals using organization-scoped activity rather than infrastructure-only estimates."
        );
    }

    public OperationsAttestationResponse attestations(AuthenticatedActor actor) {
        return new OperationsAttestationResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                operationsPolicyProperties.getExpectedAttestations(),
                recentAttestations(actor.organizationId()),
                "This sign-off history shows how important operational changes, policy acknowledgments, and environment-specific controls were explicitly recorded by administrators."
        );
    }

    public OperationsAttestationResponse recordAttestation(AuthenticatedActor actor, OperationsAttestationRequest request) {
        var createdAt = Instant.now(clock);
        jdbcTemplate.update("""
                        insert into operations_attestation (
                            attestation_id, organization_id, actor_id, actor_role, policy_area, environment_name,
                            attestation_type, change_summary, control_ids, acknowledgment, created_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                "operations_attestation_" + UUID.randomUUID(),
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                request.policyArea().trim(),
                request.environmentName().trim(),
                request.attestationType().trim(),
                request.changeSummary().trim(),
                String.join("|", request.controlIds() == null ? List.of() : request.controlIds().stream().map(String::trim).filter(value -> !value.isBlank()).toList()),
                request.acknowledgment().trim(),
                timestamp(createdAt)
        );
        return attestations(actor);
    }

    private List<String> buildObservabilityNarratives() {
        return List.of(
                "Operators can separate workflow pressure from connector incidents by reviewing blocked deliveries alongside answer and brief activity.",
                "Retention posture stays visible next to incident runbooks so cleanup and recovery are easier to rehearse together.",
                "The product now tells a more credible private-deployment operations story without claiming production-grade infrastructure automation."
        );
    }

    private List<OperationsAttestationResponse.AttestationEvent> recentAttestations(String organizationId) {
        return jdbcTemplate.query("""
                select attestation_id, policy_area, environment_name, attestation_type, actor_id, actor_role,
                       change_summary, control_ids, acknowledgment, created_at
                from operations_attestation
                where organization_id = ?
                order by created_at desc
                limit 12
                """, (rs, row) -> new OperationsAttestationResponse.AttestationEvent(
                rs.getString("attestation_id"),
                rs.getString("policy_area"),
                rs.getString("environment_name"),
                rs.getString("attestation_type"),
                rs.getString("actor_id"),
                rs.getString("actor_role"),
                rs.getString("change_summary"),
                splitControlIds(rs.getString("control_ids")),
                rs.getString("acknowledgment"),
                rs.getTimestamp("created_at").toInstant()
        ), organizationId);
    }

    private List<String> splitControlIds(String controlIds) {
        if (controlIds == null || controlIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(controlIds.split("\\|"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private int observed(
            String metric,
            int briefsLast30Days,
            int answersLast30Days,
            int validationsLast30Days,
            int trackedExportsLast30Days,
            int inboundCasesLast30Days
    ) {
        return switch (metric) {
            case "briefs_last_30_days" -> briefsLast30Days;
            case "answers_last_30_days" -> answersLast30Days;
            case "validations_last_30_days" -> validationsLast30Days;
            case "tracked_exports_last_30_days" -> trackedExportsLast30Days;
            case "inbound_cases_last_30_days" -> inboundCasesLast30Days;
            default -> 0;
        };
    }

    private String quotaStatus(int observed, int softLimit) {
        if (softLimit <= 0) {
            return "informational";
        }
        if (observed >= softLimit) {
            return "watch";
        }
        if (observed >= Math.round(softLimit * 0.75)) {
            return "nearing_limit";
        }
        return "healthy";
    }

    private Instant daysAgo(int days) {
        return Instant.now(clock).minus(days, ChronoUnit.DAYS);
    }

    private int count(String sql, Object... args) {
        var value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private String string(String sql, Object... args) {
        return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getString(1) : null, args);
    }

    private Instant instant(String sql, Object... args) {
        return jdbcTemplate.query(sql, rs -> rs.next() ? toInstant(rs.getTimestamp(1)) : null, args);
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private Instant toInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
