package dev.healthforge.platform.retrieval;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/v1/retrieval")
public class RetrievalController {

    private final JdbcTemplate jdbcTemplate;

    public RetrievalController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/search")
    public RetrievalResponse search(@Valid @RequestBody RetrievalRequest request) {
        var limit = request.limit() == null ? 8 : request.limit();
        var sourceTypes = request.sourceTypes() == null ? Set.<String>of() : Set.copyOf(request.sourceTypes());
        var candidateLimit = sourceTypes.isEmpty() ? limit : Math.min(limit * 10, 100);
        var candidates = jdbcTemplate.query(
                """
                select p.passage_id, p.normalized_text, p.ordinal, p.locator,
                       v.manifest_source_id, v.source_version, v.source_type, v.title, v.canonical_url
                from source_passage p
                join source_version v on v.source_version_id = p.source_version_id
                where v.status = 'indexed'
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
                request.query(), request.query(), candidateLimit
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
