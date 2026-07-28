package dev.healthforge.platform.evaluation;

import dev.healthforge.platform.auth.ActorRole;
import dev.healthforge.platform.auth.AuthenticatedActor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EvaluationDashboardServiceIntegrationTest {

    @Autowired
    private EvaluationDashboardService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("delete from answer_generation_event where organization_id = 'tenant.eval'");
        jdbcTemplate.update("delete from brief_review_decision where organization_id = 'tenant.eval'");
        jdbcTemplate.update("delete from brief_approval where organization_id = 'tenant.eval'");
        jdbcTemplate.update("delete from brief_finding where brief_id like 'brief-eval-%'");
        jdbcTemplate.update("delete from engineering_brief where organization_id = 'tenant.eval'");
    }

    @Test
    void buildsEvaluationDashboardFromRuntimeAndBaselineSignals() {
        var briefId = "brief-eval-" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into engineering_brief (
                    brief_id, organization_id, status, created_at, question, project_context, corpus_id, corpus_version
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                briefId, "tenant.eval", "approved", Timestamp.from(Instant.parse("2026-07-27T12:00:00Z")),
                "What changes do we need for prior auth workflows?", "Synthetic planning", "mvp-regulatory-corpus", "2026-07-24-expanded-web-core-v4");
        jdbcTemplate.update("""
                insert into brief_finding (
                    finding_id, brief_id, kind, statement, confidence, source_id, source_version, locator, support
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "find-eval-1", briefId, "interpretation", "Example finding", "medium",
                "cms-0057-f-final-rule", "2024-final", "Page 380", "support");
        jdbcTemplate.update("""
                insert into brief_review_decision (
                    review_id, brief_id, finding_id, organization_id, decision, reviewer, decided_at, rationale, corrected_statement
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "review-eval-1", briefId, "find-eval-1", "tenant.eval", "accept", "reviewer.one",
                Timestamp.from(Instant.parse("2026-07-27T12:10:00Z")), "Grounded", null);
        jdbcTemplate.update("""
                insert into brief_approval (
                    approval_id, brief_id, organization_id, approver, approver_role, approved_at, rationale
                ) values (?, ?, ?, ?, ?, ?, ?)
                """,
                "approval-eval-1", briefId, "tenant.eval", "approver.one", "approver",
                Timestamp.from(Instant.parse("2026-07-27T12:20:00Z")), "Approved");
        jdbcTemplate.update("""
                insert into answer_generation_event (
                    answer_generation_event_id, organization_id, actor_id, actor_role, corpus_id, corpus_version,
                    answer_status, retrieval_result_count, unsupported_triggered, question_hash, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "answer-event-1", "tenant.eval", "reviewer.one", "reviewer", "mvp-regulatory-corpus",
                "2026-07-24-expanded-web-core-v4", "grounded", 5, false, "hash-1",
                Timestamp.from(Instant.parse("2026-07-27T12:30:00Z")));

        var response = service.dashboard(new AuthenticatedActor("auditor.one", ActorRole.AUDITOR, "tenant.eval", "local_header"));

        assertThat(response.qualityGate().gateId()).isEqualTo("mvp-retrieval-quality-gate-v2");
        assertThat(response.qualityGate().candidateReport()).contains("evals/reports/");
        assertThat(response.evidenceHealth().totalAnswers()).isEqualTo(1);
        assertThat(response.reviewQuality().approvals()).isEqualTo(1);
        assertThat(response.workflowQuality().recentBriefApprovals()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void buildsPolicySafetyReport() {
        jdbcTemplate.update("""
                insert into answer_generation_event (
                    answer_generation_event_id, organization_id, actor_id, actor_role, corpus_id, corpus_version,
                    answer_status, retrieval_result_count, unsupported_triggered, question_hash, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "answer-event-2", "tenant.eval", "anonymous", "anonymous", "mvp-regulatory-corpus",
                "2026-07-24-expanded-web-core-v4", "insufficient_evidence", 0, true, "hash-2",
                Timestamp.from(Instant.parse("2026-07-27T13:00:00Z")));

        var response = service.policySafetyReport(new AuthenticatedActor("auditor.one", ActorRole.AUDITOR, "tenant.eval", "local_header"));

        assertThat(response.policyVersion()).isNotBlank();
        assertThat(response.unsupportedOutputSummary().insufficientEvidenceAnswers()).isGreaterThanOrEqualTo(1);
        assertThat(response.enabledControls()).isNotEmpty();
    }
}
