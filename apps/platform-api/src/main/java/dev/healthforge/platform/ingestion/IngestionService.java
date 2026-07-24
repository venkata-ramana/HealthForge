package dev.healthforge.platform.ingestion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class IngestionService {

    private static final String REQUESTED = "requested";

    private final JdbcTemplate jdbcTemplate;
    private final SourcePolicyValidator sourcePolicyValidator;
    private final SourceArtifactFetcher sourceArtifactFetcher;
    private final PdfPassageExtractor pdfPassageExtractor;
    private final HtmlPassageExtractor htmlPassageExtractor;
    private final ArtifactStorageProperties artifactStorageProperties;
    private final Clock clock;

    public IngestionService(
            JdbcTemplate jdbcTemplate,
            SourcePolicyValidator sourcePolicyValidator,
            SourceArtifactFetcher sourceArtifactFetcher,
            PdfPassageExtractor pdfPassageExtractor,
            HtmlPassageExtractor htmlPassageExtractor,
            ArtifactStorageProperties artifactStorageProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.sourcePolicyValidator = sourcePolicyValidator;
        this.sourceArtifactFetcher = sourceArtifactFetcher;
        this.pdfPassageExtractor = pdfPassageExtractor;
        this.htmlPassageExtractor = htmlPassageExtractor;
        this.artifactStorageProperties = artifactStorageProperties;
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
                null,
                null
        );

        jdbcTemplate.update(
                """
                insert into ingestion_job (
                    ingestion_id, manifest_source_id, source_version, canonical_url,
                    expected_content_type, requested_by, status, requested_at, source_version_id, error_message
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                job.ingestionId(), request.manifestSourceId(), request.sourceVersion(), request.canonicalUrl(),
                request.expectedContentType(), request.requestedBy(), job.status(), Timestamp.from(job.requestedAt()), null, null
        );
        try {
            var artifact = sourceArtifactFetcher.fetch(request);
            var checksum = sha256(artifact.bytes());
            var isPdf = "application/pdf".equalsIgnoreCase(artifact.contentType());
            var sourceVersionId = findExistingSourceVersion(request.manifestSourceId(), checksum);
            if (sourceVersionId == null) {
                var passages = isPdf ? pdfPassageExtractor.extract(artifact.bytes()) : htmlPassageExtractor.extract(artifact.bytes());
                var parserVersion = isPdf ? PdfPassageExtractor.PARSER_VERSION : HtmlPassageExtractor.PARSER_VERSION;
                var chunkingVersion = isPdf ? PdfPassageExtractor.CHUNKING_VERSION : HtmlPassageExtractor.CHUNKING_VERSION;
                var artifactUri = storeArtifact(checksum, artifact.bytes(), isPdf ? "pdf" : "html");
                sourceVersionId = "srcver_" + UUID.randomUUID();
                var sourcePolicy = sourcePolicyValidator.sourcePolicyFor(request.manifestSourceId());
                jdbcTemplate.update(
                        """
                        insert into source_version (
                            source_version_id, manifest_source_id, source_version, source_type, title,
                            canonical_url, artifact_uri, artifact_sha256, content_type, retrieved_at,
                            parser_version, chunking_version, status
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        sourceVersionId, request.manifestSourceId(), request.sourceVersion(), sourcePolicy.sourceType(),
                        sourcePolicy.title(), artifact.finalUri().toString(), artifactUri, checksum, artifact.contentType(),
                        Timestamp.from(Instant.now(clock)), parserVersion, chunkingVersion, "indexed"
                );
                for (var passage : passages) {
                    jdbcTemplate.update(
                            """
                            insert into source_passage (passage_id, source_version_id, ordinal, locator, normalized_text)
                            values (?, ?, ?, ?, ?)
                            """,
                            "passage_" + UUID.randomUUID(), sourceVersionId, passage.ordinal(), passage.locator(), passage.normalizedText()
                    );
                }
            }
            jdbcTemplate.update("update ingestion_job set status = ?, source_version_id = ? where ingestion_id = ?", "indexed", sourceVersionId, job.ingestionId());
            return new IngestionJob(job.ingestionId(), "indexed", job.manifestSourceId(), job.requestedAt(), sourceVersionId,
                    checksum, isPdf ? PdfPassageExtractor.PARSER_VERSION : HtmlPassageExtractor.PARSER_VERSION,
                    isPdf ? PdfPassageExtractor.CHUNKING_VERSION : HtmlPassageExtractor.CHUNKING_VERSION, null);
        } catch (ResponseStatusException exception) {
            jdbcTemplate.update("update ingestion_job set status = ?, error_message = ? where ingestion_id = ?", "rejected", exception.getReason(), job.ingestionId());
            throw exception;
        }
    }

    public IngestionJob get(String ingestionId) {
        var jobs = jdbcTemplate.query(
                """
                select j.ingestion_id, j.status, j.manifest_source_id, j.requested_at,
                       j.source_version_id, j.error_message, v.artifact_sha256,
                       v.parser_version, v.chunking_version
                from ingestion_job j
                left join source_version v on v.source_version_id = j.source_version_id
                where j.ingestion_id = ?
                """,
                (resultSet, rowNumber) -> new IngestionJob(
                        resultSet.getString("ingestion_id"),
                        resultSet.getString("status"),
                        resultSet.getString("manifest_source_id"),
                        resultSet.getTimestamp("requested_at").toInstant(),
                        resultSet.getString("source_version_id"), resultSet.getString("artifact_sha256"),
                        resultSet.getString("parser_version"), resultSet.getString("chunking_version"),
                        resultSet.getString("error_message")
                ),
                ingestionId
        );
        if (jobs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingestion job was not found");
        }
        return jobs.getFirst();
    }

    private String findExistingSourceVersion(String manifestSourceId, String checksum) {
        var sourceVersionIds = jdbcTemplate.query(
                "select source_version_id from source_version where manifest_source_id = ? and artifact_sha256 = ?",
                (resultSet, rowNumber) -> resultSet.getString("source_version_id"), manifestSourceId, checksum
        );
        return sourceVersionIds.isEmpty() ? null : sourceVersionIds.getFirst();
    }

    private String storeArtifact(String checksum, byte[] bytes, String extension) {
        try {
            Files.createDirectories(artifactStorageProperties.artifactDirectory());
            var artifactPath = artifactStorageProperties.artifactDirectory().resolve(checksum + "." + extension);
            if (Files.notExists(artifactPath)) {
                Files.write(artifactPath, bytes, StandardOpenOption.CREATE_NEW);
            }
            return artifactPath.toUri().toString();
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not persist the approved source artifact");
        }
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JVM", exception);
        }
    }
}
