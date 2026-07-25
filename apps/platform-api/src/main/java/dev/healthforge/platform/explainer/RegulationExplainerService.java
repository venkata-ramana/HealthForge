package dev.healthforge.platform.explainer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class RegulationExplainerService {
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-zA-Z0-9$]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "what", "which", "that", "this", "from", "into",
            "about", "does", "is", "are", "was", "were", "how", "when", "where", "who",
            "why", "its", "their", "then", "than", "have", "has", "had", "not", "use"
    );

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public RegulationExplainerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RegulationExplainerResponse explain(RegulationExplainerRequest request) {
        var findings = query(request.corpusId(), request.corpusVersion(), request.sourceId(), request.question(), 5);
        if (findings.isEmpty()) {
            var fallback = fallbackQuery(request.question());
            if (!fallback.isBlank()) {
                findings = query(request.corpusId(), request.corpusVersion(), request.sourceId(), fallback, 5);
            }
        }

        var input = new RegulationExplainerResponse.Input(
                request.corpusId(),
                request.corpusVersion(),
                request.sourceId(),
                request.question(),
                request.projectContext()
        );

        if (findings.isEmpty()) {
            return new RegulationExplainerResponse(
                    "explainer_" + UUID.randomUUID(),
                    "insufficient_evidence",
                    Instant.now(clock),
                    input,
                    null,
                    "The selected source is in scope, but the current corpus snapshot did not yield enough cited evidence for the requested explainer. Refine the question or expand the selected source coverage.",
                    List.of(),
                    List.of(
                            "The request remained bounded to the selected source, so the system did not fall back to other corpus documents.",
                            "Human review is still required even when a later explainer produces grounded evidence."
                    ),
                    List.of(
                            "Does the selected source contain the specific workflow detail being requested?",
                            "Should the explainer be widened to include related implementation guidance or standards sources?"
                    ),
                    List.of(),
                    List.of("retrieve_grounded_evidence", "expand_corpus_scope"),
                    true,
                    "This explainer workflow never substitutes for legal, compliance, or implementation approval."
            );
        }

        var first = findings.getFirst();
        var source = first.citation();
        var normalized = (request.question() + " " + (request.projectContext() == null ? "" : request.projectContext())).toLowerCase(Locale.ROOT);

        return new RegulationExplainerResponse(
                "explainer_" + UUID.randomUUID(),
                "grounded",
                Instant.now(clock),
                input,
                new RegulationExplainerResponse.Source(
                        source.sourceId(),
                        source.sourceVersion(),
                        source.sourceType(),
                        source.title(),
                        source.canonicalUrl()
                ),
                plainEnglishSummary(source.title(), normalized, findings.size()),
                technicalImplications(normalized),
                List.of(
                        "The explainer summarizes only the selected approved source and does not infer support from unrelated payer or vendor documentation.",
                        "Engineering implications remain hypotheses for human review until a reviewer validates deployment context and counterparties."
                ),
                unresolvedQuestions(normalized),
                findings,
                List.of("architecture_review", "brief_review", "implementation_work_item_export"),
                true,
                "This regulation explainer is evidence-backed but non-authoritative. Human review is mandatory before implementation planning or compliance interpretation."
        );
    }

    private List<RegulationExplainerResponse.Finding> query(
            String corpusId,
            String corpusVersion,
            String sourceId,
            String query,
            int limit
    ) {
        return jdbcTemplate.query("""
                select p.passage_id, p.normalized_text, p.locator,
                       v.manifest_source_id, v.source_version, v.source_type, v.title, v.canonical_url
                from source_passage p
                join source_version v on v.source_version_id = p.source_version_id
                join corpus_snapshot_source css on css.source_version_id = v.source_version_id
                join corpus_snapshot cs on cs.corpus_id = css.corpus_id and cs.corpus_version = css.corpus_version
                where cs.corpus_id = ? and cs.corpus_version = ?
                  and v.manifest_source_id = ?
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
                """, (rs, row) -> new RegulationExplainerResponse.Finding(
                "exp_find_" + (row + 1),
                excerpt(rs.getString("normalized_text")),
                new RegulationExplainerResponse.Citation(
                        rs.getString("passage_id"),
                        rs.getString("manifest_source_id"),
                        rs.getString("source_version"),
                        rs.getString("source_type"),
                        rs.getString("title"),
                        rs.getString("canonical_url"),
                        rs.getString("locator"),
                        "This cited passage supports the regulation explainer."
                )
        ), corpusId, corpusVersion, sourceId, query, query, limit);
    }

    private String plainEnglishSummary(String title, String normalized, int count) {
        if (normalized.contains("prior authorization") || normalized.contains("api")) {
            return "This explainer summarizes how " + title + " appears to affect prior-authorization workflow or API planning, using " + count + " cited passage(s) from the selected source.";
        }
        if (normalized.contains("workflow")) {
            return "This explainer translates the selected regulation into plain-English workflow guidance, using only cited passages from the chosen source.";
        }
        return "This explainer turns the selected authoritative source into a plain-English technical summary anchored in cited evidence from the local corpus.";
    }

    private List<String> technicalImplications(String normalized) {
        var implications = new ArrayList<String>();
        implications.add("Validate whether the selected source changes engineering scope, not just compliance interpretation.");
        if (normalized.contains("prior authorization") || normalized.contains("claim")) {
            implications.add("Review request, status, and follow-up handling for prior-authorization flows before implementation planning.");
        }
        if (normalized.contains("api") || normalized.contains("fhir")) {
            implications.add("Map cited requirements to explicit API, resource, or integration boundaries rather than assuming one-step implementation.");
        }
        if (normalized.contains("documentation") || normalized.contains("dtr")) {
            implications.add("Treat documentation capture as a separate workflow concern with reviewer-visible checkpoints.");
        }
        if (normalized.contains("authentication") || normalized.contains("consent")) {
            implications.add("Identity, authentication, and consent implications should be reviewed before making interoperability design claims.");
        }
        return implications;
    }

    private List<String> unresolvedQuestions(String normalized) {
        var unresolved = new ArrayList<String>();
        unresolved.add("Which parts of the cited regulation are mandatory versus contextual commentary for the target implementation?");
        unresolved.add("Does the target deployment require related standards guidance in addition to the selected regulation source?");
        if (normalized.contains("prior authorization") || normalized.contains("claim")) {
            unresolved.add("Which workflow stages are payer-specific versus generally implied by the cited source?");
        }
        return unresolved;
    }

    private String fallbackQuery(String query) {
        return TOKEN_SPLIT.splitAsStream(query.toLowerCase(Locale.ROOT))
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .filter(token -> !STOP_WORDS.contains(token))
                .distinct()
                .limit(6)
                .reduce((left, right) -> left + " OR " + right)
                .orElse("");
    }

    private String excerpt(String value) {
        return value.length() <= 900 ? value : value.substring(0, 900) + "…";
    }
}
