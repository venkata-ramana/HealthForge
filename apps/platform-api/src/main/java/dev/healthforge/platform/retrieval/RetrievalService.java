package dev.healthforge.platform.retrieval;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RetrievalService {

    private final JdbcTemplate jdbcTemplate;

    public RetrievalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RetrievalResponse search(RetrievalRequest request) {
        var limit = request.limit() == null ? 8 : request.limit();
        var sourceTypes = request.sourceTypes() == null ? Set.<String>of() : Set.copyOf(request.sourceTypes());
        var candidateLimit = sourceTypes.isEmpty() ? limit : Math.min(limit * 10, 100);
        var candidates = jdbcTemplate.query(
                """
                select p.passage_id, p.normalized_text, p.ordinal, p.locator,
                       v.manifest_source_id, v.source_version, v.source_type, v.title, v.canonical_url
                from source_passage p
                join source_version v on v.source_version_id = p.source_version_id
                join corpus_snapshot_source css on css.source_version_id = v.source_version_id
                join corpus_snapshot cs on cs.corpus_id = css.corpus_id and cs.corpus_version = css.corpus_version
                where cs.corpus_id = ? and cs.corpus_version = ?
                  and v.status = 'indexed'
                  and to_tsvector('english', p.normalized_text) @@ websearch_to_tsquery('english', ?)
                order by ts_rank(to_tsvector('english', p.normalized_text), websearch_to_tsquery('english', ?)) desc
                limit ?
                """,
                (resultSet, rowNumber) -> new RetrievalResponse.RetrievalResult(
                        resultSet.getString("passage_id"),
                        excerpt(resultSet.getString("normalized_text")),
                        rowNumber + 1,
                        1.0 / (rowNumber + 1),
                        new RetrievalResponse.CiteableSource(
                                resultSet.getString("manifest_source_id"),
                                resultSet.getString("source_version"),
                                resultSet.getString("source_type"),
                                resultSet.getString("title"),
                                resultSet.getString("canonical_url"),
                                resultSet.getString("locator")
                        )
                ),
                request.corpusId(), request.corpusVersion(), request.query(), request.query(), candidateLimit
        );
        var results = candidates.stream()
                .filter(result -> sourceTypes.isEmpty() || sourceTypes.contains(result.source().sourceType()))
                .limit(limit)
                .toList();
        return new RetrievalResponse(
                request.corpusId(),
                request.corpusVersion(),
                "postgres-fts-v1",
                results
        );
    }

    private String excerpt(String value) {
        return value.length() <= 800 ? value : value.substring(0, 800) + "…";
    }
}
