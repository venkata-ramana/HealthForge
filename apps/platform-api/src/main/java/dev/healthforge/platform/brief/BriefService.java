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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
                insert into engineering_brief (brief_id, organization_id, status, created_at, question, project_context, corpus_id, corpus_version)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, briefId, actor.organizationId(), "draft", Timestamp.from(createdAt), request.question(), request.projectContext(), request.corpusId(), request.corpusVersion());

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
        return get(briefId, actor);
    }

    public BriefResponse recordDecision(String briefId, ReviewDecisionRequest request, AuthenticatedActor actor) {
        get(briefId, actor);
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
                insert into brief_review_decision (review_id, brief_id, finding_id, organization_id, decision, reviewer, decided_at, rationale, corrected_statement)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, "review_" + UUID.randomUUID(), briefId, request.findingId(), actor.organizationId(), request.decision(), actor.actorId(),
                Timestamp.from(decidedAt), request.rationale(), request.correctedStatement());
        var status = switch (request.decision()) {
            case "reject", "correct", "needs_information" -> "changes_requested";
            default -> "in_review";
        };
        jdbcTemplate.update("update engineering_brief set status = ? where brief_id = ? and organization_id = ?", status, briefId, actor.organizationId());
        auditEventService.record(briefId, actor, "review_decision_recorded",
                "Recorded review decision '" + request.decision() + "' for a Brief finding.",
                "finding_id=" + request.findingId() + ", status=" + status);
        return get(briefId, actor);
    }

    public BriefResponse approve(String briefId, ApprovalRequest request, AuthenticatedActor actor) {
        var brief = loadBriefRow(briefId, actor.organizationId());
        if (!"in_review".equals(brief.status())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A Brief can only be approved from the in_review state.");
        }
        var acceptCount = jdbcTemplate.queryForObject("""
                select count(*) from brief_review_decision
                where brief_id = ? and organization_id = ? and decision = 'accept'
                """, Integer.class, briefId, actor.organizationId());
        if (acceptCount == null || acceptCount == 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A Brief requires at least one accepted review decision before approval.");
        }
        var approvalId = "approval_" + UUID.randomUUID();
        var approvedAt = Instant.now(clock);
        jdbcTemplate.update("""
                insert into brief_approval (approval_id, brief_id, organization_id, approver, approver_role, approved_at, rationale)
                values (?, ?, ?, ?, ?, ?, ?)
                """, approvalId, briefId, actor.organizationId(), actor.actorId(), actor.role().name().toLowerCase(), Timestamp.from(approvedAt), request.rationale());
        jdbcTemplate.update("update engineering_brief set status = ? where brief_id = ? and organization_id = ?", "approved", briefId, actor.organizationId());
        auditEventService.record(briefId, actor, "brief_approved",
                "Recorded final approval for the Brief.",
                "approval_id=" + approvalId + ", status=approved");
        return get(briefId, actor);
    }

    public BriefResponse get(String briefId, AuthenticatedActor actor) {
        var brief = loadBriefRow(briefId, actor.organizationId());
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
        var approvals = jdbcTemplate.query("""
                select approval_id, approver, approver_role, approved_at, rationale
                from brief_approval where brief_id = ? and organization_id = ? order by approved_at
                """, (rs, row) -> new BriefResponse.Approval(
                rs.getString("approval_id"),
                rs.getString("approver"),
                rs.getString("approver_role"),
                rs.getTimestamp("approved_at").toInstant(),
                rs.getString("rationale")
        ), briefId, actor.organizationId());
        var auditEvents = auditEventService.listForBrief(briefId, actor.organizationId());
        return new BriefResponse(brief.id(), brief.status(), brief.createdAt(), new BriefResponse.Input(brief.question(), brief.context(), brief.corpusId(), brief.corpusVersion()),
                sources, findings, "Draft assembled from cited evidence excerpts; human review is required.",
                List.of("This local MVP does not make a legal, regulatory, clinical, or compliance determination."), decisions, approvals, auditEvents, true);
    }

    public BriefAuditExportResponse exportAudit(String briefId, AuthenticatedActor actor) {
        var brief = get(briefId, actor);
        return new BriefAuditExportResponse(
                brief.briefId(),
                brief.status(),
                brief.createdAt(),
                brief.input().corpusId(),
                brief.input().corpusVersion(),
                brief.reviewDecisions(),
                brief.approvals(),
                brief.auditEvents()
        );
    }

    public BriefAuditExportResponse exportAudit(String briefId) {
        return exportAudit(briefId, new AuthenticatedActor("local.system", dev.healthforge.platform.auth.ActorRole.ADMINISTRATOR));
    }

    public BriefWorkItemExportResponse exportWorkItems(String briefId, AuthenticatedActor actor) {
        var brief = get(briefId, actor);
        if (!"approved".equals(brief.status())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only approved Briefs can be exported as implementation work items.");
        }

        var latestAcceptedDecisionsByFinding = brief.reviewDecisions().stream()
                .filter(decision -> "accept".equals(decision.decision()))
                .collect(Collectors.toMap(
                        BriefResponse.ReviewDecision::findingId,
                        decision -> decision,
                        (left, right) -> left.decidedAt().isAfter(right.decidedAt()) ? left : right
                ));

        var sourceByKey = brief.sources().stream()
                .collect(Collectors.toMap(
                        source -> source.sourceId() + "|" + source.sourceVersion(),
                        source -> source
                ));

        var standardsTouchpoints = brief.sources().stream()
                .filter(source -> "candidate_technical_guidance".equals(source.sourceType())
                        || source.canonicalUrl().contains("hl7.org/fhir"))
                .map(source -> source.title() + " (" + source.sourceVersion() + ")")
                .sorted()
                .toList();

        var workItems = brief.findings().stream()
                .filter(finding -> latestAcceptedDecisionsByFinding.containsKey(finding.findingId()))
                .sorted(Comparator.comparing(BriefResponse.Finding::findingId))
                .map(finding -> toWorkItem(finding, latestAcceptedDecisionsByFinding.get(finding.findingId()), sourceByKey, standardsTouchpoints, brief))
                .toList();

        return new BriefWorkItemExportResponse(
                brief.briefId(),
                brief.status(),
                brief.createdAt(),
                Instant.now(clock),
                "approved_for_export",
                "JSON export only. This artifact excludes unapproved findings and does not perform external tracker writeback.",
                workItems,
                brief.approvals(),
                brief.auditEvents()
        );
    }

    public BriefWorkItemExportResponse exportWorkItems(String briefId) {
        return exportWorkItems(briefId, new AuthenticatedActor("local.system", dev.healthforge.platform.auth.ActorRole.ADMINISTRATOR));
    }

    private BriefRow loadBriefRow(String briefId, String organizationId) {
        var briefs = jdbcTemplate.query("""
                select brief_id, organization_id, status, created_at, question, project_context, corpus_id, corpus_version
                from engineering_brief where brief_id = ? and organization_id = ?
                """, (rs, row) -> new BriefRow(rs.getString("brief_id"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(), rs.getString("question"), rs.getString("project_context"),
                rs.getString("corpus_id"), rs.getString("corpus_version"), rs.getString("organization_id")), briefId, organizationId);
        if (briefs.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Brief was not found");
        return briefs.getFirst();
    }

    public List<BriefSummary> list(AuthenticatedActor actor) {
        return jdbcTemplate.query("""
                select brief_id, status, created_at, question
                from engineering_brief
                where organization_id = ?
                order by created_at desc
                """, (rs, row) -> new BriefSummary(rs.getString("brief_id"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(), rs.getString("question")), actor.organizationId());
    }

    private BriefWorkItemExportResponse.WorkItem toWorkItem(
            BriefResponse.Finding finding,
            BriefResponse.ReviewDecision acceptedDecision,
            Map<String, BriefResponse.Source> sourceByKey,
            List<String> standardsTouchpoints,
            BriefResponse brief
    ) {
        var source = sourceByKey.get(finding.citation().sourceId() + "|" + finding.citation().sourceVersion());
        var effectiveStatement = acceptedDecision.correctedStatement() != null && !acceptedDecision.correctedStatement().isBlank()
                ? acceptedDecision.correctedStatement()
                : finding.statement();

        var evidence = List.of(new BriefWorkItemExportResponse.Evidence(
                finding.citation().sourceId(),
                finding.citation().sourceVersion(),
                source == null ? finding.citation().sourceId() : source.title(),
                source == null ? null : source.canonicalUrl(),
                finding.citation().locator(),
                finding.citation().support(),
                acceptedDecision.reviewer(),
                acceptedDecision.decidedAt(),
                acceptedDecision.rationale()
        ));

        return new BriefWorkItemExportResponse.WorkItem(
                "work_" + finding.findingId().replaceFirst("^find_", ""),
                buildTitle(effectiveStatement),
                effectiveStatement,
                inferCapability(brief.input().question(), effectiveStatement),
                standardsTouchpoints,
                List.of(
                        "Derived from an approved Brief and limited to findings with an accepted review decision.",
                        "Evidence remains non-sensitive and citeable; human implementation review is still required.",
                        "No direct GitHub, Jira, or external tracker writeback occurs in this phase."
                ),
                "approved_brief_human_review_retained",
                List.of(finding.findingId()),
                evidence
        );
    }

    private String buildTitle(String statement) {
        var compact = statement.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 72) {
            return compact;
        }
        return compact.substring(0, 69) + "...";
    }

    private String inferCapability(String question, String statement) {
        var text = (question + " " + statement).toLowerCase(Locale.ROOT);
        if (text.contains("prior authorization")) return "prior_authorization_workflow";
        if (text.contains("claimresponse") || text.contains("decision")) return "authorization_decision_handling";
        if (text.contains("claim")) return "authorization_request_submission";
        if (text.contains("coverage")) return "coverage_and_eligibility_context";
        if (text.contains("audit")) return "review_and_audit_traceability";
        return "engineering_review_workflow";
    }

    private record BriefRow(String id, String status, Instant createdAt, String question, String context, String corpusId, String corpusVersion, String organizationId) {}
}
