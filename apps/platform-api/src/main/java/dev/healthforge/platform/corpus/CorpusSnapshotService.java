package dev.healthforge.platform.corpus;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;

@Service
public class CorpusSnapshotService {
    private final JdbcTemplate jdbcTemplate;
    public CorpusSnapshotService(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
    public CorpusSnapshotResponse create(CorpusSnapshotRequest request) {
        var existing = jdbcTemplate.queryForObject("select count(*) from corpus_snapshot where corpus_id = ? and corpus_version = ?", Integer.class, request.corpusId(), request.corpusVersion());
        if (existing != null && existing > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Corpus snapshot already exists and is immutable");
        for (var sourceVersionId : request.sourceVersionIds()) {
            var indexed = jdbcTemplate.queryForObject("select count(*) from source_version where source_version_id = ? and status = 'indexed'", Integer.class, sourceVersionId);
            if (indexed == null || indexed == 0) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Snapshot source version is not indexed");
        }
        var createdAt = Instant.now();
        jdbcTemplate.update("insert into corpus_snapshot (corpus_id, corpus_version, created_at, retrieval_configuration) values (?, ?, ?, ?)", request.corpusId(), request.corpusVersion(), Timestamp.from(createdAt), "postgres-fts-v1");
        for (var sourceVersionId : request.sourceVersionIds()) jdbcTemplate.update("insert into corpus_snapshot_source (corpus_id, corpus_version, source_version_id) values (?, ?, ?)", request.corpusId(), request.corpusVersion(), sourceVersionId);
        return new CorpusSnapshotResponse(request.corpusId(), request.corpusVersion(), createdAt, "postgres-fts-v1", request.sourceVersionIds());
    }
    public CorpusSnapshotResponse get(String corpusId, String corpusVersion) {
        var rows = jdbcTemplate.query("select created_at, retrieval_configuration from corpus_snapshot where corpus_id = ? and corpus_version = ?", (rs, row) -> new Object[]{rs.getTimestamp("created_at").toInstant(), rs.getString("retrieval_configuration")}, corpusId, corpusVersion);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Corpus snapshot was not found");
        var sourceIds = jdbcTemplate.query("select source_version_id from corpus_snapshot_source where corpus_id = ? and corpus_version = ? order by source_version_id", (rs,row)->rs.getString(1), corpusId, corpusVersion);
        return new CorpusSnapshotResponse(corpusId, corpusVersion, (Instant) rows.getFirst()[0], (String) rows.getFirst()[1], sourceIds);
    }
}
