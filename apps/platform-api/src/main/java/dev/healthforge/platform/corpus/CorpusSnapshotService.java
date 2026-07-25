package dev.healthforge.platform.corpus;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class CorpusSnapshotService {
    private final JdbcTemplate jdbcTemplate;
    public CorpusSnapshotService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    public CorpusSnapshotResponse create(CorpusSnapshotRequest request) {
        var historicalReconstruction = Boolean.TRUE.equals(request.includeHistoricalSources());
        var existing = jdbcTemplate.queryForObject("select count(*) from corpus_snapshot where corpus_id = ? and corpus_version = ?", Integer.class, request.corpusId(), request.corpusVersion());
        if (existing != null && existing > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Corpus snapshot already exists and is immutable");

        var selectedVersions = new LinkedHashMap<String, SourceVersionState>();
        for (var sourceVersionId : request.sourceVersionIds()) {
            var rows = jdbcTemplate.query(
                    """
                    select source_version_id, manifest_source_id, status, terms_review_decision
                    from source_version
                    where source_version_id = ?
                    """,
                    (rs, rowNum) -> new SourceVersionState(
                            rs.getString("source_version_id"),
                            rs.getString("manifest_source_id"),
                            rs.getString("status"),
                            rs.getString("terms_review_decision")
                    ),
                    sourceVersionId
            );
            if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Snapshot source version was not found");
            var version = rows.getFirst();
            selectedVersions.put(sourceVersionId, version);
            if (!historicalReconstruction && !version.isEligibleForCurrentSnapshot()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Snapshot source version is not eligible for a current snapshot");
            }
        }

        if (!historicalReconstruction) {
            var manifestSourceIds = new LinkedHashSet<String>();
            for (var version : selectedVersions.values()) {
                if (!manifestSourceIds.add(version.manifestSourceId())) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Current snapshots cannot include multiple lifecycle variants of the same manifest source");
                }
            }
        }

        var createdAt = Instant.now();
        jdbcTemplate.update(
                "insert into corpus_snapshot (corpus_id, corpus_version, created_at, retrieval_configuration) values (?, ?, ?, ?)",
                request.corpusId(), request.corpusVersion(), Timestamp.from(createdAt), "postgres-fts-v1"
        );
        for (var sourceVersionId : request.sourceVersionIds()) jdbcTemplate.update("insert into corpus_snapshot_source (corpus_id, corpus_version, source_version_id) values (?, ?, ?)", request.corpusId(), request.corpusVersion(), sourceVersionId);

        if (!historicalReconstruction) {
            for (var version : selectedVersions.values()) {
                jdbcTemplate.update(
                        "update source_version set status = 'active', superseded_by_source_version_id = null where source_version_id = ? and status = 'indexed'",
                        version.sourceVersionId()
                );
            }
            for (var version : selectedVersions.values()) {
                var supersededSourceVersionIds = jdbcTemplate.query(
                        """
                        select source_version_id
                        from source_version
                        where manifest_source_id = ?
                          and source_version_id <> ?
                          and status = 'active'
                        """,
                        (rs, rowNum) -> rs.getString("source_version_id"),
                        version.manifestSourceId(), version.sourceVersionId()
                );
                if (!supersededSourceVersionIds.isEmpty()) {
                    jdbcTemplate.update(
                            """
                            update source_version
                            set status = 'superseded', superseded_by_source_version_id = ?
                            where manifest_source_id = ?
                              and source_version_id <> ?
                              and status = 'active'
                            """,
                            version.sourceVersionId(), version.manifestSourceId(), version.sourceVersionId()
                    );
                }
            }
        }

        return new CorpusSnapshotResponse(request.corpusId(), request.corpusVersion(), createdAt, "postgres-fts-v1", request.sourceVersionIds(), historicalReconstruction);
    }
    public CorpusSnapshotResponse get(String corpusId, String corpusVersion) {
        var rows = jdbcTemplate.query("select created_at, retrieval_configuration from corpus_snapshot where corpus_id = ? and corpus_version = ?", (rs, row) -> new Object[]{rs.getTimestamp("created_at").toInstant(), rs.getString("retrieval_configuration")}, corpusId, corpusVersion);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Corpus snapshot was not found");
        var sourceIds = jdbcTemplate.query("select source_version_id from corpus_snapshot_source where corpus_id = ? and corpus_version = ? order by source_version_id", (rs,row)->rs.getString(1), corpusId, corpusVersion);
        var historicalReconstruction = jdbcTemplate.query(
                """
                select count(*)
                from corpus_snapshot_source css
                join source_version sv on sv.source_version_id = css.source_version_id
                where css.corpus_id = ?
                  and css.corpus_version = ?
                  and sv.status in ('superseded', 'withdrawn', 'rejected')
                """,
                (rs, rowNum) -> rs.getInt(1),
                corpusId, corpusVersion
        ).getFirst() > 0;
        return new CorpusSnapshotResponse(corpusId, corpusVersion, (Instant) rows.getFirst()[0], (String) rows.getFirst()[1], sourceIds, historicalReconstruction);
    }

    private record SourceVersionState(String sourceVersionId, String manifestSourceId, String status, String termsReviewDecision) {
        private boolean isEligibleForCurrentSnapshot() {
            return List.of("indexed", "active").contains(status) && "approved".equalsIgnoreCase(termsReviewDecision);
        }
    }
}
