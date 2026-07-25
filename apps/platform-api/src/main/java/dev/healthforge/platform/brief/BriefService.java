package dev.healthforge.platform.brief;

import dev.healthforge.platform.answer.GroundedAnswerRequest;
import dev.healthforge.platform.answer.GroundedAnswerService;
import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BriefService {
    private final JdbcTemplate jdbcTemplate;
    private final GroundedAnswerService groundedAnswerService;
    private final BriefAuditEventService auditEventService;
    private final Clock clock = Clock.systemUTC();

    public BriefService(
            JdbcTemplate jdbcTemplate,
            GroundedAnswerService groundedAnswerService,
            BriefAuditEventService auditEventService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.groundedAnswerService = groundedAnswerService;
        this.auditEventService = auditEventService;
    }

    public BriefResponse create(BriefRequest request, AuthenticatedActor actor) {
        var packet = groundedAnswerService.answer(new GroundedAnswerRequest(
                request.corpusId(), request.corpusVersion(), request.question(), request.projectContext(), request.sourceTypes()
        ));
        if (!"grounded".equals(packet.status())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A Brief cannot be created without cited evidence from the selected corpus");
        }
        var briefId = "brief_" + UUID.randomUUID();
        var createdAt = Instant.now(clock);
        jdbcTemplate.update("""
                insert into engineering_brief (brief_id, status, created_at, question, project_context, corpus_id, corpus_version)
                values (?, ?, ?, ?, ?, ?, ?)
                """, briefId, "draft", Timestamp.from(createdAt), request.question(), request.projectContext(), request.corpusId(), request.corpusVersion());

        for (var source : packet.findings().stream().map(f -> f.citation()).distinct().toList()) {
            jdbcTemplate.update("""
                    insert into brief_source (brief_id, source_id, source_version, source_type, title, canonical_url)
                    values (?, ?, ?, ?, ?, ?) on conflict do nothing
                    """, briefId, source.sourceId(), source.sourceVersion(), source.sourceType(), source.title(), source.canonicalUrl());
        }
        for (var finding : packet.findings()) {
            var findingId = "find_" + UUID.randomUUID();
            jdbcTemplate.update("""
                    insert into brief_finding (finding_id, brief_id, kind, statement, confidence, source_id, source_version, locator, support)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, findingId, briefId, "interpretation", finding.statement(), "medium",
                    finding.citation().sourceId(), finding.citation().sourceVersion(), finding.citation().locator(), finding.citation().support());
        }
        auditEventService.record(briefId, actor, "brief_created",
                "Created a draft Brief from grounded evidence.",
                "corpus=" + request.corpusId() + "/" + request.corpusVersion());
        auditEventService.record(briefId, actor, "evidence_selected",
                "Selected cited evidence for the Brief draft.",
                "findings=" + packet.findings().size() + ", sources=" + packet.findings().stream().map(f -> f.citation().sourceId()).distinct().count());
        return get(briefId);
    }

    public BriefResponse recordDecision(String briefId, ReviewDecisionRequest request, AuthenticatedActor actor) {
        get(briefId);
        var count = jdbcTemplate.queryForObject(
                "select count(*) from brief_finding where brief_id = ? and finding_id = ?", Integer.class, briefId, request.findingId());
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding does not belong to this Brief");
        }
        if (request.reviewer() != null && !request.reviewer().isBlank() && !actor.actorId().equals(request.reviewer().trim())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Reviewer in the request body must match the authenticated actor.");
        }
        var decidedAt = Instant.now(clock);
        jdbcTemplate.update("""
                insert into brief_review_decision (review_id, brief_id, finding_id, decision, reviewer, decided_at, rationale, corrected_statement)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, "review_" + UUID.randomUUID(), briefId, request.findingId(), request.decision(), actor.actorId(),
                Timestamp.from(decidedAt), request.rationale(), request.correctedStatement());
        var status = switch (request.decision()) {
            case "reject", "correct", "needs_information" -> "changes_requested";
            default -> "in_review";
        };
        jdbcTemplate.update("update engineering_brief set status = ? where brief_id = ?", status, briefId);
        auditEventService.record(briefId, actor, "review_decision_recorded",
                "Recorded review decision '" + request.decision() + "' for a Brief finding.",
                "finding_id=" + request.findingId() + ", status=" + status);
        return get(briefId);
    }

    public BriefResponse get(String briefId) {
        var briefs = jdbcTemplate.query("""
                select brief_id, status, created_at, question, project_context, corpus_id, corpus_version
                from engineering_brief where brief_id = ?
                """, (rs, row) -> new BriefRow(rs.getString("brief_id"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(), rs.getString("question"), rs.getString("project_context"),
                rs.getString("corpus_id"), rs.getString("corpus_version")), briefId);
        if (briefs.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Brief was not found");
        var brief = briefs.getFirst();
        var sources = jdbcTemplate.query("select source_id, source_version, source_type, title, canonical_url from brief_source where brief_id = ?",
                (rs, row) -> new BriefResponse.Source(rs.getString("source_id"), rs.getString("source_version"), rs.getString("source_type"), rs.getString("title"), rs.getString("canonical_url")), briefId);
        var findings = jdbcTemplate.query("""
                select finding_id, kind, statement, confidence, source_id, source_version, locator, support
                from brief_finding where brief_id = ? order by finding_id
                """, (rs, row) -> new BriefResponse.Finding(rs.getString("finding_id"), rs.getString("kind"), rs.getString("statement"),
                rs.getString("confidence"), new BriefResponse.Citation(rs.getString("source_id"), rs.getString("source_version"), rs.getString("locator"), rs.getString("support"))), briefId);
        var decisions = jdbcTemplate.query("""
                select review_id, finding_id, decision, reviewer, decided_at, rationale, corrected_statement
                from brief_review_decision where brief_id = ? order by decided_at
                """, (rs, row) -> new BriefResponse.ReviewDecision(rs.getString("review_id"), rs.getString("finding_id"), rs.getString("decision"),
                rs.getString("reviewer"), rs.getTimestamp("decided_at").toInstant(), rs.getString("rationale"), rs.getString("corrected_statement")), briefId);
        var auditEvents = auditEventService.listForBrief(briefId);
        return new BriefResponse(brief.id(), brief.status(), brief.createdAt(), new BriefResponse.Input(brief.question(), brief.context(), brief.corpusId(), brief.corpusVersion()),
                sources, findings, "Draft assembled from cited evidence excerpts; human review is required.",
                List.of("This local MVP does not make a legal, regulatory, clinical, or compliance determination."), decisions, auditEvents, true);
    }

    public List<BriefSummary> list() {
        return jdbcTemplate.query("""
                select brief_id, status, created_at, question from engineering_brief order by created_at desc
                """, (rs, row) -> new BriefSummary(rs.getString("brief_id"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(), rs.getString("question")));
    }

    private record BriefRow(String id, String status, Instant createdAt, String question, String context, String corpusId, String corpusVersion) {}
}
