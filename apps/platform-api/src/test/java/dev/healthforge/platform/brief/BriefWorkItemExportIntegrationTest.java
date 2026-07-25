package dev.healthforge.platform.brief;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class BriefWorkItemExportIntegrationTest {

    @Autowired
    private BriefService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from brief_audit_event where brief_id like 'brief-test-%'");
        jdbcTemplate.update("delete from brief_approval where brief_id like 'brief-test-%'");
        jdbcTemplate.update("delete from brief_review_decision where brief_id like 'brief-test-%'");
        jdbcTemplate.update("delete from brief_finding where brief_id like 'brief-test-%'");
        jdbcTemplate.update("delete from brief_source where brief_id like 'brief-test-%'");
        jdbcTemplate.update("delete from engineering_brief where brief_id like 'brief-test-%'");
    }

    @Test
    void rejectsExportWhenBriefIsNotApproved() {
        var briefId = insertBrief("draft");
        insertSource(briefId, "hl7-fhir-r4", "4.0.1", "candidate_technical_guidance", "HL7 FHIR R4", "https://hl7.org/fhir/R4/");
        var findingId = "find_test_" + UUID.randomUUID();
        insertFinding(findingId, briefId, "Add FHIR Claim review handling", "hl7-fhir-r4", "4.0.1");
        insertDecision("review_" + UUID.randomUUID(), briefId, findingId, "accept", "reviewer.one", "Accepted");

        assertThatThrownBy(() -> service.exportWorkItems(briefId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only approved Briefs");
    }

    @Test
    void exportsOnlyAcceptedFindingsForApprovedBriefs() {
        var briefId = insertBrief("approved");
        insertSource(briefId, "hl7-fhir-r4", "4.0.1", "candidate_technical_guidance", "HL7 FHIR R4", "https://hl7.org/fhir/R4/");
        insertSource(briefId, "cms-0057-f-final-rule", "2024-final", "governing_regulation", "CMS-0057-F", "https://www.cms.gov/files/document/cms-0057-f.pdf");

        var acceptedFindingId = "find_test_" + UUID.randomUUID();
        var rejectedFindingId = "find_test_" + UUID.randomUUID();

        insertFinding(acceptedFindingId, briefId, "Implement prior authorization Claim intake workflow", "hl7-fhir-r4", "4.0.1");
        insertFinding(rejectedFindingId, briefId, "Unsupported rejected finding", "cms-0057-f-final-rule", "2024-final");

        insertDecision("review_" + UUID.randomUUID(), briefId, acceptedFindingId, "accept", "reviewer.one", "Ready for engineering planning");
        insertDecision("review_" + UUID.randomUUID(), briefId, rejectedFindingId, "reject", "reviewer.two", "Needs more evidence");
        insertApproval(briefId);
        insertAuditEvent(briefId, "brief_approved", "administrator", "Recorded final approval for the Brief.");

        var export = service.exportWorkItems(briefId);

        assertThat(export.briefStatus()).isEqualTo("approved");
        assertThat(export.approvalStatus()).isEqualTo("approved_for_export");
        assertThat(export.workItems()).singleElement().satisfies(workItem -> {
            assertThat(workItem.relatedFindingIds()).containsExactly(acceptedFindingId);
            assertThat(workItem.affectedCapability()).isEqualTo("prior_authorization_workflow");
            assertThat(workItem.standardsTouchpoints()).contains("HL7 FHIR R4 (4.0.1)");
            assertThat(workItem.evidence()).singleElement().satisfies(evidence -> {
                assertThat(evidence.sourceId()).isEqualTo("hl7-fhir-r4");
                assertThat(evidence.acceptedBy()).isEqualTo("reviewer.one");
            });
        });
    }

    private String insertBrief(String status) {
        var briefId = "brief-test-" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into engineering_brief (brief_id, status, created_at, question, project_context, corpus_id, corpus_version)
                values (?, ?, ?, ?, ?, ?, ?)
                """,
                briefId,
                status,
                Timestamp.from(Instant.parse("2026-07-25T16:00:00Z")),
                "What changes do we need for prior authorization workflows?",
                "Synthetic local planning context only.",
                "mvp-regulatory-corpus",
                "2026-07-24-expanded-web-core-v4");
        return briefId;
    }

    private void insertSource(String briefId, String sourceId, String sourceVersion, String sourceType, String title, String canonicalUrl) {
        jdbcTemplate.update("""
                insert into brief_source (brief_id, source_id, source_version, source_type, title, canonical_url)
                values (?, ?, ?, ?, ?, ?)
                """, briefId, sourceId, sourceVersion, sourceType, title, canonicalUrl);
    }

    private void insertFinding(String findingId, String briefId, String statement, String sourceId, String sourceVersion) {
        jdbcTemplate.update("""
                insert into brief_finding (finding_id, brief_id, kind, statement, confidence, source_id, source_version, locator, support)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                findingId,
                briefId,
                "interpretation",
                statement,
                "medium",
                sourceId,
                sourceVersion,
                "section-1",
                "Synthetic support for export testing.");
    }

    private void insertDecision(String reviewId, String briefId, String findingId, String decision, String reviewer, String rationale) {
        jdbcTemplate.update("""
                insert into brief_review_decision (review_id, brief_id, finding_id, decision, reviewer, decided_at, rationale, corrected_statement)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                reviewId,
                briefId,
                findingId,
                decision,
                reviewer,
                Timestamp.from(Instant.parse("2026-07-25T16:05:00Z")),
                rationale,
                null);
    }

    private void insertApproval(String briefId) {
        jdbcTemplate.update("""
                insert into brief_approval (approval_id, brief_id, approver, approver_role, approved_at, rationale)
                values (?, ?, ?, ?, ?, ?)
                """,
                "approval_" + UUID.randomUUID(),
                briefId,
                "admin.one",
                "administrator",
                Timestamp.from(Instant.parse("2026-07-25T16:10:00Z")),
                "Reviewed and approved for local export.");
    }

    private void insertAuditEvent(String briefId, String eventType, String actorRole, String summary) {
        jdbcTemplate.update("""
                insert into brief_audit_event (audit_event_id, brief_id, actor_id, actor_role, event_type, occurred_at, summary, details)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "audit_" + UUID.randomUUID(),
                briefId,
                "actor.one",
                actorRole,
                eventType,
                Timestamp.from(Instant.parse("2026-07-25T16:10:30Z")),
                summary,
                null);
    }
}
