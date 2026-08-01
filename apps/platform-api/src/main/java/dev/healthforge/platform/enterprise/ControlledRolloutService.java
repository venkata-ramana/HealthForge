package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ControlledRolloutService {

    private static final Set<String> STATUSES = Set.of("planned", "partial", "in_place", "blocked");
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public ControlledRolloutService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ControlledRolloutResponse assess(AuthenticatedActor actor) {
        var definitions = definitions(actor.organizationId());
        var evidence = evidence(actor.organizationId());
        var phases = definitions.stream()
                .map(definition -> buildPhase(definition, evidence))
                .toList();
        var overallScore = phases.stream().mapToInt(ControlledRolloutResponse.PhaseSummary::score).sum() / phases.size();
        var gaps = phases.stream()
                .flatMap(phase -> phase.checks().stream()
                        .filter(check -> !"in_place".equals(check.status()))
                        .map(check -> new ControlledRolloutResponse.Gap(
                                phase.phaseId(),
                                check.checkId(),
                                check.title(),
                                check.ownerRole(),
                                check.nextAction()
                        )))
                .toList();
        var decision = overallScore >= 85 && gaps.isEmpty() ? "ready_for_controlled_rollout"
                : overallScore >= 60 ? "conditionally_ready"
                : "not_ready";
        return new ControlledRolloutResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                Instant.now(clock),
                decision,
                overallScore,
                phases,
                evidence.values().stream().toList(),
                gaps,
                List.of(
                        "This registry records rollout evidence; it does not replace external security, compliance, performance, or clinical review.",
                        "Evidence is organization-scoped and should remain synthetic or non-sensitive unless a separately approved deployment boundary exists.",
                        "A ready decision means the recorded checks are in place for this operating context, not that every external dependency is production-certified."
                ),
                "This view turns Phases 31–35 into an executable evidence registry for identity, connectors, quality, pilot operations, and controlled rollout."
        );
    }

    public ControlledRolloutResponse recordEvidence(AuthenticatedActor actor, ControlledRolloutEvidenceRequest request) {
        var definition = definitions(actor.organizationId()).stream()
                .flatMap(phase -> phase.checks().stream().map(check -> new CheckLookup(phase.phaseId(), check.checkId())))
                .filter(item -> item.phaseId().equals(request.phaseId()) && item.checkId().equals(request.checkId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown phaseId/checkId combination."));
        var status = request.status().trim().toLowerCase(Locale.ROOT);
        if (!STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be planned, partial, in_place, or blocked");
        }
        var now = Timestamp.from(Instant.now(clock));
        jdbcTemplate.update("""
                insert into controlled_rollout_evidence (
                    evidence_id, organization_id, phase_id, check_id, status, owner_role,
                    evidence_summary, next_action, actor_id, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (organization_id, phase_id, check_id) do update set
                    status = excluded.status,
                    owner_role = excluded.owner_role,
                    evidence_summary = excluded.evidence_summary,
                    next_action = excluded.next_action,
                    actor_id = excluded.actor_id,
                    updated_at = excluded.updated_at
                """,
                "rollout_evidence_" + UUID.randomUUID(),
                actor.organizationId(),
                definition.phaseId(),
                definition.checkId(),
                status,
                request.ownerRole().trim(),
                request.evidenceSummary().trim(),
                request.nextAction().trim(),
                actor.actorId(),
                now,
                now
        );
        return assess(actor);
    }

    private List<PhaseDefinition> definitions(String organizationId) {
        var identityProvider = count("select count(*) from workspace_identity_provider where organization_id = ?", organizationId) > 0;
        var secretBoundaries = count("select count(*) from operations_attestation where organization_id = ?", organizationId) > 0;
        var deliveries = count("select count(*) from tracked_export_event where organization_id = ?", organizationId) > 0;
        var recovery = count("select count(*) from integration_recovery_action where organization_id = ?", organizationId) > 0;
        var sources = count("select count(*) from source_version") > 0;
        var qualityFeedback = count("select count(*) from retrieval_feedback where organization_id = ?", organizationId) > 0
                || count("select count(*) from pilot_feedback where organization_id = ?", organizationId) > 0;
        var project = count("select count(*) from workspace_project where organization_id = ?", organizationId) > 0;
        var milestones = count("select count(*) from pilot_success_checkpoint where organization_id = ?", organizationId) > 0;

        return List.of(
                phase("phase_31", "Enterprise identity and tenant security", List.of(
                        check("identity_provider", "Identity provider posture", identityProvider, "Configure or evidence a trusted identity provider for shared deployment.", "administrator"),
                        check("tenant_isolation", "Tenant isolation checks", false, "Run organization-isolation tests against shared data and access paths.", "administrator"),
                        check("access_lifecycle", "Access lifecycle evidence", false, "Record role assignment, removal, and access-review evidence.", "auditor"),
                        check("secret_boundaries", "Secret and configuration boundaries", secretBoundaries, "Record an administrator attestation for environment and secret boundaries.", "administrator")
                )),
                phase("phase_32", "Reliable connector execution", List.of(
                        check("connector_contracts", "Connector contract checks", false, "Run contract checks for each supported connector mode.", "administrator"),
                        check("idempotency", "Idempotency and duplicate protection", false, "Evidence duplicate-delivery protection for each outbound path.", "administrator"),
                        check("recovery_drill", "Retry and recovery drill", recovery, "Run and record a retry, replay, or reconciliation drill.", "operator"),
                        check("delivery_reconciliation", "Delivery reconciliation", deliveries, "Reconcile at least one delivery from request through receipt.", "operator")
                )),
                phase("phase_33", "Quality engineering and corpus lifecycle", List.of(
                        check("corpus_freshness", "Corpus freshness evidence", sources, "Review source version freshness and supersession posture.", "reviewer"),
                        check("benchmark_regression", "Benchmark and regression run", false, "Run the benchmark suite and retain the regression result.", "auditor"),
                        check("answer_thresholds", "Answer-quality thresholds", false, "Define and review quality thresholds for grounded and unsupported outputs.", "auditor"),
                        check("feedback_release_loop", "Feedback connected to release review", qualityFeedback, "Attach evidence-quality or reviewer feedback to a release decision.", "reviewer")
                )),
                phase("phase_34", "Pilot operations and customer success", List.of(
                        check("pilot_owner", "Pilot owner and workspace", project, "Create a pilot workspace with an accountable owner.", "administrator"),
                        check("support_escalation", "Support and escalation path", false, "Exercise an escalation or support handoff and record the outcome.", "operator"),
                        check("sponsor_review", "Sponsor review cadence", milestones, "Record a sponsor checkpoint and outcome review.", "auditor"),
                        check("pilot_closeout", "Repeatable pilot closeout", false, "Complete a pilot closeout with outcomes, gaps, and next-step ownership.", "reviewer")
                )),
                phase("phase_35", "Controlled rollout and scale", List.of(
                        check("capacity_evidence", "Performance and capacity evidence", false, "Run representative capacity tests and retain the results.", "operator"),
                        check("restore_rehearsal", "Backup and restore rehearsal", false, "Complete an isolated restore rehearsal and record the result.", "administrator"),
                        check("security_review", "Security and dependency review", secretBoundaries, "Attach security and dependency review evidence to the release.", "auditor"),
                        check("release_rollback", "Release approval and rollback posture", false, "Record an approved rollback or canary plan.", "administrator")
                ))
        );
    }

    private PhaseDefinition phase(String phaseId, String title, List<CheckDefinition> checks) {
        return new PhaseDefinition(phaseId, title, checks);
    }

    private CheckDefinition check(String checkId, String title, boolean derivedInPlace, String nextAction, String ownerRole) {
        return new CheckDefinition(checkId, title, derivedInPlace ? "in_place" : "planned", derivedInPlace ? "Derived from current organization-scoped platform evidence." : "No explicit evidence has been recorded yet.", ownerRole, nextAction);
    }

    private ControlledRolloutResponse.PhaseSummary buildPhase(PhaseDefinition definition, Map<String, ControlledRolloutResponse.EvidenceItem> evidence) {
        var checks = definition.checks().stream().map(check -> {
            var key = definition.phaseId() + ":" + check.checkId();
            var item = evidence.get(key);
            return item == null
                    ? new ControlledRolloutResponse.Check(check.checkId(), check.title(), check.status(), check.evidence(), check.ownerRole(), check.nextAction())
                    : new ControlledRolloutResponse.Check(check.checkId(), check.title(), item.status(), item.evidenceSummary(), item.ownerRole(), item.nextAction());
        }).toList();
        var score = (int) (checks.stream().filter(check -> "in_place".equals(check.status())).count() * 100 / checks.size());
        var status = score == 100 ? "in_place" : score >= 50 ? "partial" : "planned";
        return new ControlledRolloutResponse.PhaseSummary(definition.phaseId(), definition.title(), score, status, checks);
    }

    private Map<String, ControlledRolloutResponse.EvidenceItem> evidence(String organizationId) {
        var result = new HashMap<String, ControlledRolloutResponse.EvidenceItem>();
        jdbcTemplate.query("""
                select evidence_id, phase_id, check_id, status, owner_role, evidence_summary, next_action, actor_id, updated_at
                from controlled_rollout_evidence
                where organization_id = ?
                order by phase_id, check_id
                """, rs -> {
            var item = new ControlledRolloutResponse.EvidenceItem(
                    rs.getString("evidence_id"),
                    rs.getString("phase_id"),
                    rs.getString("check_id"),
                    rs.getString("status"),
                    rs.getString("owner_role"),
                    rs.getString("evidence_summary"),
                    rs.getString("next_action"),
                    rs.getString("actor_id"),
                    rs.getTimestamp("updated_at").toInstant()
            );
            result.put(item.phaseId() + ":" + item.checkId(), item);
        }, organizationId);
        return result;
    }

    private int count(String sql, Object... args) {
        var value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private record PhaseDefinition(String phaseId, String title, List<CheckDefinition> checks) {
    }

    private record CheckDefinition(String checkId, String title, String status, String evidence, String ownerRole, String nextAction) {
    }

    private record CheckLookup(String phaseId, String checkId) {
    }
}
