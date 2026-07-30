package dev.healthforge.platform.corpus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CorpusSnapshotServiceIntegrationTest {

    @Autowired
    private CorpusSnapshotService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from corpus_snapshot_source where corpus_id like 'test-corpus-%'");
        jdbcTemplate.update("delete from corpus_snapshot where corpus_id like 'test-corpus-%'");
        jdbcTemplate.update("delete from source_passage where source_version_id like 'srcver-test-%'");
        jdbcTemplate.update("update source_version set superseded_by_source_version_id = null where superseded_by_source_version_id like 'srcver-test-%'");
        jdbcTemplate.update("update source_version set superseded_by_source_version_id = null where source_version_id like 'srcver-test-%'");
        jdbcTemplate.update("delete from source_version where source_version_id like 'srcver-test-%'");
    }

    @Test
    void currentSnapshotsRejectWithdrawnVersionsButHistoricalSnapshotsAllowThem() {
        var activeVersionId = insertSourceVersion("cms-0057-f-final-rule", "2024-final", "active", "approved");
        var withdrawnVersionId = insertSourceVersion("cms-prior-auth-api-workflow", "2024-withdrawn", "withdrawn", "approved");

        assertThatThrownBy(() -> service.create(new CorpusSnapshotRequest(
                "test-corpus-" + UUID.randomUUID(),
                "current",
                List.of(activeVersionId, withdrawnVersionId),
                false
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not eligible for a current snapshot");

        var historical = service.create(new CorpusSnapshotRequest(
                "test-corpus-" + UUID.randomUUID(),
                "historical",
                List.of(activeVersionId, withdrawnVersionId),
                true
        ));

        assertThat(historical.historicalReconstruction()).isTrue();
        assertThat(historical.sourceVersionIds()).containsExactly(activeVersionId, withdrawnVersionId);
    }

    @Test
    void newerSnapshotSupersedesThePreviouslyActiveVersion() {
        var originalVersionId = insertSourceVersion("cms-0057-f-final-rule", "2024-final", "active", "approved");
        var newerVersionId = insertSourceVersion("cms-0057-f-final-rule", "2024-final-update", "indexed", "approved");

        var response = service.create(new CorpusSnapshotRequest(
                "test-corpus-" + UUID.randomUUID(),
                "v-next",
                List.of(newerVersionId),
                false
        ));

        assertThat(response.historicalReconstruction()).isFalse();

        var rows = jdbcTemplate.query(
                "select status, superseded_by_source_version_id from source_version where source_version_id = ?",
                (rs, rowNum) -> rs.getString("status") + "|" + rs.getString("superseded_by_source_version_id"),
                originalVersionId
        );

        assertThat(rows.getFirst()).isEqualTo("superseded|" + newerVersionId);
    }

    @Test
    void comparesSnapshotDifferencesAcrossVersions() {
        var baseVersionId = insertSourceVersion("cms-0057-f-final-rule", "2024-final", "active", "approved");
        var nextVersionId = insertSourceVersion("cms-0057-f-final-rule", "2024-final-update", "indexed", "approved");
        var addedVersionId = insertSourceVersion("cms-extra-guidance", "2026-guidance", "active", "approved");
        var corpusId = "test-corpus-" + UUID.randomUUID();

        service.create(new CorpusSnapshotRequest(corpusId, "v1", List.of(baseVersionId), false));
        service.create(new CorpusSnapshotRequest(corpusId, "v2", List.of(nextVersionId, addedVersionId), false));

        var diff = service.diff(corpusId, "v2", "v1");

        assertThat(diff.summary().addedCount()).isEqualTo(1);
        assertThat(diff.summary().changedCount()).isEqualTo(1);
        assertThat(diff.addedSources()).extracting(CorpusSnapshotDiffResponse.AddedSource::manifestSourceId)
                .contains("cms-extra-guidance");
        assertThat(diff.changedSources()).extracting(CorpusSnapshotDiffResponse.ChangedSource::manifestSourceId)
                .contains("cms-0057-f-final-rule");
    }

    private String insertSourceVersion(String manifestSourceId, String sourceVersion, String status, String termsReviewDecision) {
        var sourceVersionId = "srcver-test-" + UUID.randomUUID();
        jdbcTemplate.update(
                """
                insert into source_version (
                    source_version_id, manifest_source_id, source_version, source_type, title,
                    canonical_url, artifact_uri, artifact_sha256, content_type, retrieved_at,
                    parser_version, chunking_version, status, allowed_use,
                    terms_review_decision, terms_reviewed_by, terms_reviewed_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                sourceVersionId,
                manifestSourceId,
                sourceVersion,
                "governing_regulation",
                "Test Source " + sourceVersion,
                "https://example.test/" + sourceVersion,
                "file:///tmp/" + sourceVersion + ".pdf",
                UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""),
                "application/pdf",
                Timestamp.from(Instant.parse("2026-07-25T12:00:00Z")),
                "pdfbox-v1",
                "page-v1",
                status,
                "public_reference",
                termsReviewDecision,
                "local.admin",
                Timestamp.from(Instant.parse("2026-07-25T12:00:00Z"))
        );
        return sourceVersionId;
    }
}
