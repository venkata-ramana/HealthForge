package dev.healthforge.platform.retrieval;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RetrievalServiceIntegrationTest {

    @Autowired
    private RetrievalService retrievalService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from corpus_snapshot_source where corpus_id like 'test-retrieval-%'");
        jdbcTemplate.update("delete from corpus_snapshot where corpus_id like 'test-retrieval-%'");
        jdbcTemplate.update("delete from source_passage where source_version_id like 'srcver-retrieval-%'");
        jdbcTemplate.update("delete from source_version where source_version_id like 'srcver-retrieval-%'");
    }

    @Test
    void returnsResultsForActiveSourcesIncludedInSnapshot() {
        var sourceVersionId = "srcver-retrieval-" + UUID.randomUUID();
        var corpusId = "test-retrieval-" + UUID.randomUUID();
        var corpusVersion = "v1";

        jdbcTemplate.update("""
                insert into source_version (
                    source_version_id, manifest_source_id, source_version, source_type, title,
                    canonical_url, artifact_uri, artifact_sha256, content_type, retrieved_at,
                    parser_version, chunking_version, status, allowed_use,
                    terms_review_decision, terms_reviewed_by, terms_reviewed_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                sourceVersionId,
                "healthforge-prior-auth-workflow",
                "2026-07-25-test",
                "internal_project_material",
                "Prior Authorization Workflow Test Source",
                "hf-local:///docs/07-electronic-prior-authorization-workflow.md",
                "file:///tmp/prior-auth-workflow.md",
                UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""),
                "text/markdown",
                Timestamp.from(Instant.parse("2026-07-25T17:00:00Z")),
                "md-v1",
                "section-v1",
                "active",
                "public_reference",
                "approved",
                "local.admin",
                Timestamp.from(Instant.parse("2026-07-25T17:00:00Z"))
        );

        jdbcTemplate.update("""
                insert into source_passage (passage_id, source_version_id, ordinal, locator, normalized_text)
                values (?, ?, ?, ?, ?)
                """,
                "passage_" + UUID.randomUUID(),
                sourceVersionId,
                1,
                "section-1",
                "Electronic prior authorization workflow for provider EHR integration using PAS and Claim submissions."
        );

        jdbcTemplate.update("""
                insert into corpus_snapshot (corpus_id, corpus_version, created_at, retrieval_configuration)
                values (?, ?, ?, ?)
                """,
                corpusId,
                corpusVersion,
                Timestamp.from(Instant.parse("2026-07-25T17:01:00Z")),
                "postgres-fts-v1"
        );

        jdbcTemplate.update("""
                insert into corpus_snapshot_source (corpus_id, corpus_version, source_version_id)
                values (?, ?, ?)
                """,
                corpusId,
                corpusVersion,
                sourceVersionId
        );

        var response = retrievalService.search(new RetrievalRequest(
                corpusId,
                corpusVersion,
                "prior authorization workflow PAS Claim",
                List.of(),
                5
        ));

        assertThat(response.results()).isNotEmpty();
        assertThat(response.results().getFirst().source().sourceId()).isEqualTo("healthforge-prior-auth-workflow");
    }
}
