package dev.healthforge.platform.ingestion;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SourceVersionService {

    private final JdbcTemplate jdbcTemplate;

    public SourceVersionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SourceVersionResponse get(String sourceVersionId) {
        var rows = jdbcTemplate.query(
                """
                select source_version_id, manifest_source_id, source_version, source_type, title,
                       canonical_url, artifact_sha256, content_type, retrieved_at, parser_version,
                       chunking_version, status, allowed_use, terms_review_decision,
                       terms_reviewed_by, terms_reviewed_at, superseded_by_source_version_id
                from source_version
                where source_version_id = ?
                """,
                (rs, rowNum) -> new SourceVersionResponse(
                        rs.getString("source_version_id"),
                        rs.getString("manifest_source_id"),
                        rs.getString("source_version"),
                        rs.getString("source_type"),
                        rs.getString("title"),
                        rs.getString("canonical_url"),
                        rs.getString("artifact_sha256"),
                        rs.getString("content_type"),
                        rs.getTimestamp("retrieved_at").toInstant(),
                        rs.getString("parser_version"),
                        rs.getString("chunking_version"),
                        rs.getString("status"),
                        rs.getString("allowed_use"),
                        rs.getString("terms_review_decision"),
                        rs.getString("terms_reviewed_by"),
                        rs.getTimestamp("terms_reviewed_at").toInstant(),
                        rs.getString("superseded_by_source_version_id"),
                        isEligibleForCurrentSnapshot(rs.getString("status"), rs.getString("terms_review_decision"))
                ),
                sourceVersionId
        );
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source version was not found");
        return rows.getFirst();
    }

    public SourceVersionResponse updateLifecycle(String sourceVersionId, SourceLifecycleUpdateRequest request) {
        var normalizedStatus = request.status().trim().toLowerCase();
        if (!normalizedStatus.equals("withdrawn") && !normalizedStatus.equals("active")) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Lifecycle updates currently support only active or withdrawn");
        }

        var updated = jdbcTemplate.update(
                "update source_version set status = ?, superseded_by_source_version_id = null where source_version_id = ?",
                normalizedStatus, sourceVersionId
        );
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source version was not found");
        return get(sourceVersionId);
    }

    private boolean isEligibleForCurrentSnapshot(String status, String termsReviewDecision) {
        return ("indexed".equals(status) || "active".equals(status)) && "approved".equalsIgnoreCase(termsReviewDecision);
    }
}
