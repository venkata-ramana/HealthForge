package dev.healthforge.platform.retrieval;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class RetrievalService {
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-zA-Z0-9]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "what", "which", "that", "this", "from", "into",
            "about", "does", "is", "are", "was", "were", "how", "when", "where", "who",
            "why", "its", "their", "then", "than", "have", "has", "had", "not", "use"
    );

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public RetrievalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RetrievalResponse search(RetrievalRequest request) {
        var limit = request.limit() == null ? 8 : request.limit();
        var sourceTypes = request.sourceTypes() == null ? Set.<String>of() : Set.copyOf(request.sourceTypes());
        var candidateLimit = sourceTypes.isEmpty() ? limit : Math.min(limit * 10, 100);
        var candidates = query(request.corpusId(), request.corpusVersion(), request.query(), candidateLimit);
        if (candidates.isEmpty()) {
            var fallbackQuery = fallbackQuery(request.query());
            if (!fallbackQuery.isBlank()) {
                candidates = query(request.corpusId(), request.corpusVersion(), fallbackQuery, candidateLimit);
            }
        }
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

    private List<RetrievalResponse.RetrievalResult> query(
            String corpusId,
            String corpusVersion,
            String query,
            int candidateLimit
    ) {
        return jdbcTemplate.query(
                """
                select p.passage_id, p.normalized_text, p.ordinal, p.locator,
                       v.manifest_source_id, v.source_version, v.source_type, v.title, v.canonical_url,
                       v.retrieved_at, v.status, v.superseded_by_source_version_id
                from source_passage p
                join source_version v on v.source_version_id = p.source_version_id
                join corpus_snapshot_source css on css.source_version_id = v.source_version_id
                join corpus_snapshot cs on cs.corpus_id = css.corpus_id and cs.corpus_version = css.corpus_version
                where cs.corpus_id = ? and cs.corpus_version = ?
                  and (
                    setweight(to_tsvector('english', coalesce(v.title, '')), 'A') ||
                    setweight(to_tsvector('simple', coalesce(v.manifest_source_id, '')), 'A') ||
                    setweight(to_tsvector('english', p.normalized_text), 'B')
                  ) @@ websearch_to_tsquery('english', ?)
                order by ts_rank(
                    (
                      setweight(to_tsvector('english', coalesce(v.title, '')), 'A') ||
                      setweight(to_tsvector('simple', coalesce(v.manifest_source_id, '')), 'A') ||
                      setweight(to_tsvector('english', p.normalized_text), 'B')
                    ),
                    websearch_to_tsquery('english', ?)
                ) desc
                limit ?
                """,
                (resultSet, rowNumber) -> new RetrievalResponse.RetrievalResult(
                        resultSet.getString("passage_id"),
                        excerpt(resultSet.getString("normalized_text")),
                        rowNumber + 1,
                        1.0 / (rowNumber + 1),
                        citeableSource(resultSet)
                ),
                corpusId, corpusVersion, query, query, candidateLimit
        );
    }

    private RetrievalResponse.CiteableSource citeableSource(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        var retrievedAt = resultSet.getTimestamp("retrieved_at").toInstant();
        var lifecycleStatus = resultSet.getString("status");
        var sourceAgeDays = Math.max(0L, ChronoUnit.DAYS.between(retrievedAt, Instant.now(clock)));
        var freshnessStatus = freshnessStatus(lifecycleStatus, sourceAgeDays);
        var changeSummary = resultSet.getString("superseded_by_source_version_id") == null
                ? "No newer lifecycle replacement is currently linked to this source version."
                : "A newer lifecycle replacement exists for this source version and should be reviewed before implementation decisions are reused.";
        return new RetrievalResponse.CiteableSource(
                resultSet.getString("manifest_source_id"),
                resultSet.getString("source_version"),
                resultSet.getString("source_type"),
                resultSet.getString("title"),
                resultSet.getString("canonical_url"),
                resultSet.getString("locator"),
                retrievedAt,
                lifecycleStatus,
                freshnessStatus,
                sourceAgeDays,
                changeSummary
        );
    }

    private String freshnessStatus(String lifecycleStatus, long sourceAgeDays) {
        if ("withdrawn".equalsIgnoreCase(lifecycleStatus)) {
            return "withdrawn";
        }
        if ("superseded".equalsIgnoreCase(lifecycleStatus)) {
            return "superseded";
        }
        if (sourceAgeDays > 30) {
            return "stale";
        }
        return "current";
    }

    private String fallbackQuery(String query) {
        return TOKEN_SPLIT.splitAsStream(query.toLowerCase())
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .filter(token -> !STOP_WORDS.contains(token))
                .distinct()
                .limit(6)
                .reduce((left, right) -> left + " OR " + right)
                .orElse("");
    }

    private String excerpt(String value) {
        return value.length() <= 800 ? value : value.substring(0, 800) + "…";
    }
}
