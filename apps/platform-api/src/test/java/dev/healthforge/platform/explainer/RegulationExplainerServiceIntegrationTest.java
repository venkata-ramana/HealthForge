package dev.healthforge.platform.explainer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RegulationExplainerServiceIntegrationTest {

    @Autowired
    private RegulationExplainerService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from corpus_snapshot_source where corpus_id like 'test-explainer-%'");
        jdbcTemplate.update("delete from corpus_snapshot where corpus_id like 'test-explainer-%'");
        jdbcTemplate.update("delete from source_passage where source_version_id like 'srcver-explainer-%'");
        jdbcTemplate.update("delete from source_version where source_version_id like 'srcver-explainer-%'");
    }

    @Test
    void returnsGroundedExplainerForSelectedSource() {
        var sourceVersionId = "srcver-explainer-" + UUID.randomUUID();
        var corpusId = "test-explainer-" + UUID.randomUUID();
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
                "cms-0057-f-final-rule",
                "2024-final",
                "governing_regulation",
                "CMS Interoperability and Prior Authorization Final Rule (CMS-0057-F)",
                "https://www.cms.gov/files/document/cms-0057-f.pdf",
                "file:///tmp/cms-0057-f.pdf",
                UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""),
                "application/pdf",
                Timestamp.from(Instant.parse("2026-07-25T18:00:00Z")),
                "pdfbox-v1",
                "page-v1",
                "active",
                "public_reference",
                "approved",
                "local.admin",
                Timestamp.from(Instant.parse("2026-07-25T18:00:00Z"))
        );

        jdbcTemplate.update("""
                insert into source_passage (passage_id, source_version_id, ordinal, locator, normalized_text)
                values (?, ?, ?, ?, ?)
                """,
                "passage_" + UUID.randomUUID(),
                sourceVersionId,
                1,
                "Page 380",
                "The Prior Authorization API would allow a provider to query the payer system and submit prior authorization requests from the EHR workflow."
        );

        jdbcTemplate.update("""
                insert into corpus_snapshot (corpus_id, corpus_version, created_at, retrieval_configuration)
                values (?, ?, ?, ?)
                """,
                corpusId,
                corpusVersion,
                Timestamp.from(Instant.parse("2026-07-25T18:01:00Z")),
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

        var response = service.explain(new RegulationExplainerRequest(
                corpusId,
                corpusVersion,
                "cms-0057-f-final-rule",
                "What does this source imply for prior authorization APIs?",
                "Synthetic provider planning scenario."
        ));

        assertThat(response.status()).isEqualTo("grounded");
        assertThat(response.source()).isNotNull();
        assertThat(response.source().sourceId()).isEqualTo("cms-0057-f-final-rule");
        assertThat(response.findings()).isNotEmpty();
    }
}
