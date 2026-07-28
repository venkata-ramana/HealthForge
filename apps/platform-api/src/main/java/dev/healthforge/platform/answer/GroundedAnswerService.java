package dev.healthforge.platform.answer;

import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.retrieval.RetrievalRequest;
import dev.healthforge.platform.retrieval.RetrievalService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class GroundedAnswerService {

    private final RetrievalService retrievalService;
    private final GroundedAnswerAssembler assembler;
    private final UnsupportedQuestionPolicy unsupportedQuestionPolicy;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public GroundedAnswerService(
            RetrievalService retrievalService,
            GroundedAnswerAssembler assembler,
            UnsupportedQuestionPolicy unsupportedQuestionPolicy,
            JdbcTemplate jdbcTemplate
    ) {
        this.retrievalService = retrievalService;
        this.assembler = assembler;
        this.unsupportedQuestionPolicy = unsupportedQuestionPolicy;
        this.jdbcTemplate = jdbcTemplate;
    }

    public GroundedAnswerResponse answer(GroundedAnswerRequest request) {
        return answer(request, null);
    }

    public GroundedAnswerResponse answer(GroundedAnswerRequest request, AuthenticatedActor actor) {
        var occurredAt = Instant.now(clock);
        var unsupportedDecision = unsupportedQuestionPolicy.evaluate(request.question());
        if (unsupportedDecision.isPresent()) {
            var response = assembler.assembleUnsupported(request, unsupportedDecision.get(), occurredAt);
            recordTelemetry(request, response, 0, true, actor, occurredAt);
            return response;
        }
        var retrieval = retrievalService.search(new RetrievalRequest(
                request.corpusId(), request.corpusVersion(), request.question(), request.sourceTypes(), 5
        ));
        var response = assembler.assemble(request, retrieval, occurredAt);
        recordTelemetry(request, response, retrieval.results().size(), false, actor, occurredAt);
        return response;
    }

    private void recordTelemetry(
            GroundedAnswerRequest request,
            GroundedAnswerResponse response,
            int retrievalResultCount,
            boolean unsupportedTriggered,
            AuthenticatedActor actor,
            Instant occurredAt
    ) {
        var organizationId = actor == null ? "local.default" : actor.organizationId();
        var actorId = actor == null ? "anonymous" : actor.actorId();
        var actorRole = actor == null ? "anonymous" : actor.role().name().toLowerCase();
        jdbcTemplate.update("""
                insert into answer_generation_event (
                    answer_generation_event_id, organization_id, actor_id, actor_role, corpus_id, corpus_version,
                    answer_status, retrieval_result_count, unsupported_triggered, question_hash, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "answer_generation_event_" + UUID.randomUUID(),
                organizationId,
                actorId,
                actorRole,
                request.corpusId(),
                request.corpusVersion(),
                response.status(),
                retrievalResultCount,
                unsupportedTriggered,
                hash(request.question()),
                Timestamp.from(occurredAt)
        );
    }

    private String hash(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hashing is unavailable", exception);
        }
    }
}
