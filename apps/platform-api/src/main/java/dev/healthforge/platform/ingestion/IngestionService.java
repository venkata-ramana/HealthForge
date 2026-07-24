package dev.healthforge.platform.ingestion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class IngestionService {

    private static final String REQUESTED = "requested";

    private final JdbcTemplate jdbcTemplate;
    private final SourcePolicyValidator sourcePolicyValidator;
    private final Clock clock;

    public IngestionService(JdbcTemplate jdbcTemplate, SourcePolicyValidator sourcePolicyValidator) {
        this.jdbcTemplate = jdbcTemplate;
        this.sourcePolicyValidator = sourcePolicyValidator;
        this.clock = Clock.systemUTC();
    }

    public IngestionJob request(IngestionRequest request) {
        sourcePolicyValidator.validate(request);

        var job = new IngestionJob(
                "ing_" + UUID.randomUUID(),
                REQUESTED,
                request.manifestSourceId(),
                Instant.now(clock),
                null,
                null,
                null,
                null
        );

        jdbcTemplate.update(
                """
                insert into ingestion_job (
                    ingestion_id, manifest_source_id, source_version, canonical_url,
                    expected_content_type, requested_by, status, requested_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                job.ingestionId(), request.manifestSourceId(), request.sourceVersion(), request.canonicalUrl(),
                request.expectedContentType(), request.requestedBy(), job.status(), Timestamp.from(job.requestedAt())
        );
        return job;
    }

    public IngestionJob get(String ingestionId) {
        var jobs = jdbcTemplate.query(
                """
                select ingestion_id, status, manifest_source_id, requested_at
                from ingestion_job where ingestion_id = ?
                """,
                (resultSet, rowNumber) -> new IngestionJob(
                        resultSet.getString("ingestion_id"),
                        resultSet.getString("status"),
                        resultSet.getString("manifest_source_id"),
                        resultSet.getTimestamp("requested_at").toInstant(),
                        null, null, null, null
                ),
                ingestionId
        );
        if (jobs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingestion job was not found");
        }
        return jobs.getFirst();
    }
}
