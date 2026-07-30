package dev.healthforge.platform.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.enterprise.AuditPolicyProperties;
import dev.healthforge.platform.ingestion.WorkspaceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class EvaluationDashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final WorkspaceProperties workspaceProperties;
    private final AuditPolicyProperties auditPolicyProperties;
    private final Clock clock = Clock.systemUTC();

    public EvaluationDashboardService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            WorkspaceProperties workspaceProperties,
            AuditPolicyProperties auditPolicyProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.workspaceProperties = workspaceProperties;
        this.auditPolicyProperties = auditPolicyProperties;
    }

    public EvaluationDashboardResponse dashboard(AuthenticatedActor actor) {
        var organizationId = actor.organizationId();
        var qualityGate = qualityGate();
        var sourceHealth = sourceHealth();
        var evidenceHealth = evidenceHealth(organizationId);
        var reviewQuality = reviewQuality(organizationId);
        var workflowQuality = workflowQuality(organizationId);

        return new EvaluationDashboardResponse(
                organizationId,
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                Instant.now(clock),
                qualityGate,
                sourceHealth,
                evidenceHealth,
                answerReadiness(qualityGate, evidenceHealth),
                reviewQuality,
                workflowQuality,
                List.of(
                        "Evaluation metrics combine persisted workflow telemetry with pinned regression reports under evals/.",
                        "Reviewer disagreement and consistency signals are advisory quality indicators, not automated judgments about reviewer correctness.",
                        "Unsupported-output and insufficient-evidence rates help identify where corpus coverage or prompt boundaries need attention."
                ),
                "This dashboard turns baseline files, runtime evidence telemetry, and human-review workflow data into an operator-friendly trust view."
        );
    }

    private EvaluationDashboardResponse.AnswerReadiness answerReadiness(
            EvaluationDashboardResponse.QualityGate qualityGate,
            EvaluationDashboardResponse.EvidenceHealth evidenceHealth
    ) {
        var insufficientEvidenceRate = evidenceHealth.totalAnswers() == 0
                ? 0.0
                : (double) evidenceHealth.insufficientEvidenceAnswers() / evidenceHealth.totalAnswers();
        return new EvaluationDashboardResponse.AnswerReadiness(
                Math.max(qualityGate.highlightedFailures().size(), 1),
                insufficientEvidenceRate,
                qualityGate.retrievalRecall(),
                qualityGate.citationCoverageRate(),
                insufficientEvidenceRate > 0.35
                        ? "Answer readiness is limited mainly by evidence sufficiency and query precision rather than workflow governance."
                        : "Answer readiness is within the current bounded baseline, but source freshness and citation quality should still be watched.",
                List.of(
                        "Improve evidence sufficiency diagnostics for weak-answer scenarios.",
                        "Track stale or superseded source families before reusing approved planning outputs.",
                        "Prefer question packs and reusable analyst context for repeated workflows."
                )
        );
    }

    public PolicySafetyReportResponse policySafetyReport(AuthenticatedActor actor) {
        var organizationId = actor.organizationId();
        var unsupportedSummary = unsupportedOutputSummary(organizationId);
        var approvalSummary = approvalPolicySummary(organizationId);
        var integrationSummary = integrationPolicySummary(organizationId);

        return new PolicySafetyReportResponse(
                organizationId,
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                Instant.now(clock),
                auditPolicyProperties.policyVersion(),
                unsupportedSummary,
                approvalSummary,
                integrationSummary,
                List.of(
                        "Approval is still required before governed tracker writeback, documentation publishing, and comparable downstream export actions.",
                        "Unsupported or insufficient-evidence answers are preserved as telemetry rather than silently hidden from operators.",
                        "Preview-first delivery modes remain available so teams can inspect payloads before enabling governed send or publish behavior.",
                        "Organization-scoped access and audit logging remain enforced for evaluation and safety reporting."
                ),
                List.of(
                        "This phase still operates on public, synthetic, or non-sensitive content only.",
                        "Runtime safety metrics do not certify compliance or production readiness.",
                        "Anonymous answer traffic without actor headers is attributed to the default local organization in this phase.",
                        "Regression baselines reflect the current public evaluation dataset and should evolve through reviewed updates rather than silent replacement."
                ),
                "This report explains how HealthForge currently handles unsupported requests, approval gates, and governed integration actions in a way that enterprise reviewers can inspect."
        );
    }

    private EvaluationDashboardResponse.QualityGate qualityGate() {
        var baseline = readJson(workspaceProperties.rootDirectory().resolve("evals/baselines/mvp-retrieval-quality-gate-v2.json"));
        var reportPath = latestReportPath();
        var report = readJson(reportPath);

        var retrievalRecall = metric(report, "retrieval_recall");
        var citationCoverageRate = metric(report, "citation_coverage_rate");
        var unsupportedAnswerPassRate = metric(report, "unsupported_answer_pass_rate");

        var baselineRetrieval = metric(baseline.path("approved_metrics"), "retrieval_recall");
        var baselineCoverage = metric(baseline.path("approved_metrics"), "citation_coverage_rate");
        var baselineUnsupported = metric(baseline.path("approved_metrics"), "unsupported_answer_pass_rate");
        var minimumRetrieval = metric(baseline.path("minimum_metrics"), "retrieval_recall");
        var minimumCoverage = metric(baseline.path("minimum_metrics"), "citation_coverage_rate");
        var minimumUnsupported = metric(baseline.path("minimum_metrics"), "unsupported_answer_pass_rate");
        var materialRetrieval = metric(baseline.path("material_regression_thresholds"), "retrieval_recall");
        var materialCoverage = metric(baseline.path("material_regression_thresholds"), "citation_coverage_rate");
        var materialUnsupported = metric(baseline.path("material_regression_thresholds"), "unsupported_answer_pass_rate");

        var belowMinimum = retrievalRecall < minimumRetrieval
                || citationCoverageRate < minimumCoverage
                || unsupportedAnswerPassRate < minimumUnsupported;
        var materialRegression = (baselineRetrieval - retrievalRecall) > materialRetrieval
                || (baselineCoverage - citationCoverageRate) > materialCoverage
                || (baselineUnsupported - unsupportedAnswerPassRate) > materialUnsupported;
        var decision = belowMinimum ? "blocked" : materialRegression ? "human_review_required" : "pass";

        var regressionSignals = new ArrayList<String>();
        if (retrievalRecall < baselineRetrieval) {
            regressionSignals.add("Retrieval recall is below the approved baseline and should be inspected for source coverage or ranking drift.");
        }
        if (citationCoverageRate < baselineCoverage) {
            regressionSignals.add("Citation coverage fell below the approved baseline, which threatens reviewability even if prose still looks plausible.");
        }
        if (unsupportedAnswerPassRate < baselineUnsupported) {
            regressionSignals.add("Unsupported-boundary handling regressed relative to the approved safety baseline.");
        }
        if (regressionSignals.isEmpty()) {
            regressionSignals.add("The latest pinned evaluation report meets the current approved baseline thresholds.");
        }

        return new EvaluationDashboardResponse.QualityGate(
                baseline.path("gate_id").asText("unknown-gate"),
                LocalDate.parse(baseline.path("approved_on").asText("2026-07-24")),
                baseline.path("approved_baseline_report").asText(),
                workspaceProperties.rootDirectory().relativize(reportPath).toString(),
                parseInstant(report.path("generated_at").asText(null)),
                retrievalRecall,
                citationCoverageRate,
                unsupportedAnswerPassRate,
                decision,
                regressionSignals,
                highlightedFailures(report)
        );
    }

    private List<EvaluationDashboardResponse.FailureCase> highlightedFailures(JsonNode report) {
        var failures = new ArrayList<EvaluationDashboardResponse.FailureCase>();
        for (var item : iterable(report.path("cases"))) {
            var category = item.path("category").asText();
            var retrievalHit = item.path("retrieval_hit").asBoolean(true);
            var unsupportedBoundaryPass = item.path("unsupported_boundary_pass");
            var isFailure = !retrievalHit
                    || ("unsupported".equals(category) && !unsupportedBoundaryPass.isMissingNode() && !unsupportedBoundaryPass.isNull() && !unsupportedBoundaryPass.asBoolean(true));
            if (isFailure) {
                failures.add(new EvaluationDashboardResponse.FailureCase(
                        item.path("id").asText(),
                        category,
                        item.path("severity").asText("unknown"),
                        retrievalHit,
                        item.path("answer_status").asText("unknown")
                ));
            }
            if (failures.size() == 5) {
                break;
            }
        }
        return failures;
    }

    private EvaluationDashboardResponse.SourceHealth sourceHealth() {
        var now = Instant.now(clock);
        return new EvaluationDashboardResponse.SourceHealth(
                count("select count(*) from source_version"),
                count("select count(*) from source_version where status = 'active'"),
                count("select count(*) from source_version where retrieved_at < ?", Timestamp.from(now.minus(30, ChronoUnit.DAYS))),
                count("select count(*) from source_version where superseded_by_source_version_id is not null"),
                count("select count(*) from corpus_snapshot"),
                averageSourceAgeDays(now),
                queryInstant("select max(created_at) from corpus_snapshot")
        );
    }

    private EvaluationDashboardResponse.EvidenceHealth evidenceHealth(String organizationId) {
        var totalAnswers = count("select count(*) from answer_generation_event where organization_id = ?", organizationId);
        var insufficientEvidenceAnswers = count("""
                select count(*) from answer_generation_event
                where organization_id = ? and answer_status = 'insufficient_evidence'
                """, organizationId);
        var unsupportedTriggeredAnswers = count("""
                select count(*) from answer_generation_event
                where organization_id = ? and unsupported_triggered = true
                """, organizationId);
        var approvedBriefs = count("""
                select count(*) from engineering_brief
                where organization_id = ? and status = 'approved'
                """, organizationId);
        var evidenceBearingBriefs = count("""
                select count(distinct eb.brief_id)
                from engineering_brief eb
                join brief_finding bf on bf.brief_id = eb.brief_id
                where eb.organization_id = ?
                """, organizationId);
        return new EvaluationDashboardResponse.EvidenceHealth(
                totalAnswers,
                insufficientEvidenceAnswers,
                unsupportedTriggeredAnswers,
                average("""
                        select avg(retrieval_result_count::double precision)
                        from answer_generation_event
                        where organization_id = ?
                        """, organizationId),
                approvedBriefs,
                evidenceBearingBriefs,
                approvedBriefs == 0 ? 0.0 : (double) evidenceBearingBriefs / approvedBriefs
        );
    }

    private EvaluationDashboardResponse.ReviewQuality reviewQuality(String organizationId) {
        var approvals = count("""
                select count(*) from brief_approval where organization_id = ?
                """, organizationId);
        var changesRequested = count("""
                select count(*) from engineering_brief
                where organization_id = ? and status = 'changes_requested'
                """, organizationId);
        return new EvaluationDashboardResponse.ReviewQuality(
                count("""
                        select count(*) from brief_review_decision where organization_id = ?
                        """, organizationId),
                count("""
                        select count(*) from (
                            select finding_id
                            from brief_review_decision
                            where organization_id = ?
                            group by finding_id
                            having count(distinct decision) > 1
                        ) disagreement
                        """, organizationId),
                count("""
                        select count(*) from (
                            select bf.source_id
                            from brief_review_decision brd
                            join brief_finding bf on bf.finding_id = brd.finding_id
                            where brd.organization_id = ?
                            group by bf.source_id
                            having count(distinct brd.decision) > 1
                        ) source_disagreement
                        """, organizationId),
                count("""
                        select count(*) from brief_review_decision
                        where organization_id = ? and corrected_statement is not null and length(trim(corrected_statement)) > 0
                        """, organizationId),
                approvals,
                changesRequested,
                approvals + changesRequested == 0 ? 0.0 : (double) approvals / (approvals + changesRequested),
                "Disagreement and consistency signals are meant to spotlight rubric drift, ambiguous evidence, or workflow coaching needs. They should not be treated as automatic reviewer scorecards."
        );
    }

    private EvaluationDashboardResponse.WorkflowQuality workflowQuality(String organizationId) {
        var previewOnlyWebhookDeliveries = count("""
                select count(*) from outbound_webhook_delivery
                where organization_id = ? and delivery_mode = 'preview_only'
                """, organizationId);
        var governedWebhookDeliveries = count("""
                select count(*) from outbound_webhook_delivery
                where organization_id = ? and delivery_mode <> 'preview_only' and delivery_status in ('delivered', 'retried')
                """, organizationId);
        var blockedIntegrationActions =
                count("""
                        select count(*) from tracked_export_event
                        where organization_id = ? and execution_status = 'writeback_blocked'
                        """, organizationId)
                        + count("""
                        select count(*) from collaboration_notification_event
                        where organization_id = ? and delivery_status = 'notification_blocked'
                        """, organizationId)
                        + count("""
                        select count(*) from documentation_export_event
                        where organization_id = ? and delivery_status = 'publish_blocked'
                        """, organizationId);
        var approvalsLast30Days = count("""
                select count(*) from brief_approval
                where organization_id = ? and approved_at >= ?
                """, organizationId, Timestamp.from(Instant.now(clock).minus(30, ChronoUnit.DAYS)));
        var changesRequestedLast30Days = count("""
                select count(*) from engineering_brief
                where organization_id = ? and status = 'changes_requested' and created_at >= ?
                """, organizationId, Timestamp.from(Instant.now(clock).minus(30, ChronoUnit.DAYS)));
        return new EvaluationDashboardResponse.WorkflowQuality(
                count("select count(*) from workflow_event where organization_id = ?", organizationId),
                blockedIntegrationActions,
                governedWebhookDeliveries,
                previewOnlyWebhookDeliveries,
                approvalsLast30Days,
                changesRequestedLast30Days,
                blockedIntegrationActions == 0
                        ? "Governed workflow actions have remained preview-first or successfully delivered within the current telemetry window."
                        : "Some governed workflow actions were blocked, which is expected when approval records, targets, or environment controls are intentionally missing."
        );
    }

    private PolicySafetyReportResponse.UnsupportedOutputSummary unsupportedOutputSummary(String organizationId) {
        var totalAnswers = count("select count(*) from answer_generation_event where organization_id = ?", organizationId);
        var insufficient = count("""
                select count(*) from answer_generation_event
                where organization_id = ? and answer_status = 'insufficient_evidence'
                """, organizationId);
        var unsupported = count("""
                select count(*) from answer_generation_event
                where organization_id = ? and unsupported_triggered = true
                """, organizationId);
        return new PolicySafetyReportResponse.UnsupportedOutputSummary(
                totalAnswers,
                insufficient,
                unsupported,
                totalAnswers == 0 ? 0.0 : (double) insufficient / totalAnswers,
                queryInstant("""
                        select max(created_at) from answer_generation_event where organization_id = ?
                        """, organizationId)
        );
    }

    private PolicySafetyReportResponse.ApprovalPolicySummary approvalPolicySummary(String organizationId) {
        var approved = count("""
                select count(*) from engineering_brief where organization_id = ? and status = 'approved'
                """, organizationId);
        var changesRequested = count("""
                select count(*) from engineering_brief where organization_id = ? and status = 'changes_requested'
                """, organizationId);
        var disagreement = count("""
                select count(*) from (
                    select finding_id
                    from brief_review_decision
                    where organization_id = ?
                    group by finding_id
                    having count(distinct decision) > 1
                ) disagreement
                """, organizationId);
        return new PolicySafetyReportResponse.ApprovalPolicySummary(
                approved,
                changesRequested,
                disagreement,
                auditPolicyProperties.approvalRequiredForExports(),
                "Approvals remain a deliberate governance step. Disagreement counts help identify where evidence interpretation or reviewer guidance may need refinement."
        );
    }

    private PolicySafetyReportResponse.IntegrationPolicySummary integrationPolicySummary(String organizationId) {
        var blockedTrackerWritebacks = count("""
                select count(*) from tracked_export_event
                where organization_id = ? and execution_status = 'writeback_blocked'
                """, organizationId);
        var blockedCollaborationSends = count("""
                select count(*) from collaboration_notification_event
                where organization_id = ? and delivery_status = 'notification_blocked'
                """, organizationId);
        var blockedDocumentationPublishes = count("""
                select count(*) from documentation_export_event
                where organization_id = ? and delivery_status = 'publish_blocked'
                """, organizationId);
        var previewOnlyWebhookDeliveries = count("""
                select count(*) from outbound_webhook_delivery
                where organization_id = ? and delivery_mode = 'preview_only'
                """, organizationId);
        var governedDeliveries = count("""
                select count(*) from outbound_webhook_delivery
                where organization_id = ? and delivery_mode <> 'preview_only' and delivery_status in ('delivered', 'retried')
                """, organizationId);
        return new PolicySafetyReportResponse.IntegrationPolicySummary(
                blockedTrackerWritebacks,
                blockedCollaborationSends,
                blockedDocumentationPublishes,
                previewOnlyWebhookDeliveries,
                governedDeliveries,
                "Blocked integration actions are a sign that approval gates and target requirements are still being enforced. Preview-only webhook deliveries allow safe inspection before enabling broader automation."
        );
    }

    private int count(String sql, Object... args) {
        var value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private double average(String sql, Object... args) {
        var value = jdbcTemplate.queryForObject(sql, Double.class, args);
        return value == null ? 0.0 : value;
    }

    private double averageSourceAgeDays(Instant now) {
        return jdbcTemplate.query("""
                select retrieved_at from source_version
                """, rs -> {
            double total = 0.0;
            int count = 0;
            while (rs.next()) {
                var retrievedAt = rs.getTimestamp("retrieved_at");
                if (retrievedAt != null) {
                    total += (double) ChronoUnit.DAYS.between(retrievedAt.toInstant(), now);
                    count++;
                }
            }
            return count == 0 ? 0.0 : total / count;
        });
    }

    private Instant queryInstant(String sql, Object... args) {
        var value = jdbcTemplate.queryForObject(sql, Timestamp.class, args);
        return value == null ? null : value.toInstant();
    }

    private JsonNode readJson(Path path) {
        try {
            return objectMapper.readTree(Files.readString(path));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read evaluation artifact " + path, exception);
        }
    }

    private Path latestReportPath() {
        var reportsDir = workspaceProperties.rootDirectory().resolve("evals/reports");
        try (Stream<Path> stream = Files.list(reportsDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .max(Comparator.comparing(this::lastModified))
                    .orElseThrow(() -> new IllegalStateException("No evaluation reports were found under " + reportsDir));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect evaluation reports under " + reportsDir, exception);
        }
    }

    private Instant lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect " + path, exception);
        }
    }

    private double metric(JsonNode node, String name) {
        return node.path("metrics").path(name).asDouble(node.path(name).asDouble(0.0));
    }

    private Instant parseInstant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }

    private Iterable<JsonNode> iterable(JsonNode node) {
        var items = new ArrayList<JsonNode>();
        node.forEach(items::add);
        return items;
    }
}
