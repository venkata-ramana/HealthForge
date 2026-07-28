package dev.healthforge.platform.intelligence;

import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.evaluation.EvaluationDashboardResponse;
import dev.healthforge.platform.evaluation.EvaluationDashboardService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class IntelligenceService {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-zA-Z0-9]+");
    private static final Set<String> STOP_WORDS = Set.of("the", "and", "for", "with", "what", "which", "that", "this", "from", "into", "about", "does", "is", "are", "was", "were", "how", "when", "where", "who", "why", "its", "their", "then", "than", "have", "has", "had", "not", "use");

    private final JdbcTemplate jdbcTemplate;
    private final EvaluationDashboardService evaluationDashboardService;
    private final Clock clock = Clock.systemUTC();

    public IntelligenceService(JdbcTemplate jdbcTemplate, EvaluationDashboardService evaluationDashboardService) {
        this.jdbcTemplate = jdbcTemplate;
        this.evaluationDashboardService = evaluationDashboardService;
    }

    public RetrievalFeedbackResponse recordFeedback(RetrievalFeedbackRequest request, AuthenticatedActor actor) {
        ensureFinding(request.briefId(), request.findingId(), actor.organizationId());
        var feedbackType = normalized(request.feedbackType());
        if (!List.of("helpful", "missing_evidence", "ranking_issue", "duplicate_result").contains(feedbackType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "feedback_type must be helpful, missing_evidence, ranking_issue, or duplicate_result");
        }
        var createdAt = Instant.now(clock);
        var feedbackId = "retrieval_feedback_" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into retrieval_feedback (
                    retrieval_feedback_id, organization_id, brief_id, finding_id, source_id,
                    actor_id, actor_role, feedback_type, note, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                feedbackId,
                actor.organizationId(),
                request.briefId(),
                request.findingId(),
                blankToNull(request.sourceId()),
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                feedbackType,
                blankToNull(request.note()),
                Timestamp.from(createdAt)
        );
        return new RetrievalFeedbackResponse(
                feedbackId,
                actor.organizationId(),
                request.briefId(),
                request.findingId(),
                feedbackType,
                blankToNull(request.sourceId()),
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                blankToNull(request.note()),
                createdAt
        );
    }

    public IntelligenceOverviewResponse overview(AuthenticatedActor actor) {
        var eval = evaluationDashboardService.dashboard(actor);
        return new IntelligenceOverviewResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                Instant.now(clock),
                retrievalImprovements(actor.organizationId(), eval),
                evidenceGaps(actor.organizationId(), eval),
                similarityClusters(actor.organizationId()),
                personaRecommendations(actor.organizationId(), actor, eval),
                workflowTuningRecommendations(actor.organizationId(), eval),
                List.of(
                        "All recommendations in this phase are advisory and must remain reviewable.",
                        "Similarity and tuning signals help prioritize work but do not replace reviewer or approver judgment.",
                        "Corpus expansion suggestions point only to public-source candidates and do not authorize ingestion automatically."
                ),
                "This intelligence view turns review feedback, evaluation signals, and workflow telemetry into bounded next-step recommendations."
        );
    }

    private List<IntelligenceOverviewResponse.RetrievalImprovement> retrievalImprovements(String organizationId, EvaluationDashboardResponse eval) {
        var feedbackCounts = jdbcTemplate.query("""
                select feedback_type, count(*) as total
                from retrieval_feedback
                where organization_id = ?
                group by feedback_type
                order by total desc
                """, (rs, row) -> Map.entry(rs.getString("feedback_type"), rs.getInt("total")), organizationId);
        var map = feedbackCounts.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var improvements = new ArrayList<IntelligenceOverviewResponse.RetrievalImprovement>();
        if (map.getOrDefault("missing_evidence", 0) > 0 || eval.evidenceHealth().insufficientEvidenceAnswers() > 0) {
            improvements.add(new IntelligenceOverviewResponse.RetrievalImprovement(
                    "Prioritize missing-evidence questions",
                    "high",
                    "Reviewers are signaling that some grounded answers still miss key evidence, and insufficient-evidence responses are visible in the telemetry.",
                    List.of(
                            "missing_evidence feedback: " + map.getOrDefault("missing_evidence", 0),
                            "insufficient evidence answers: " + eval.evidenceHealth().insufficientEvidenceAnswers()
                    )
            ));
        }
        if (map.getOrDefault("ranking_issue", 0) > 0 || eval.qualityGate().retrievalRecall() < 0.9) {
            improvements.add(new IntelligenceOverviewResponse.RetrievalImprovement(
                    "Tune result ranking for top evidence",
                    "medium",
                    "Ranking-related feedback and retrieval recall trends suggest work on which passages surface first.",
                    List.of(
                            "ranking_issue feedback: " + map.getOrDefault("ranking_issue", 0),
                            "retrieval recall: " + String.format(Locale.ROOT, "%.2f", eval.qualityGate().retrievalRecall())
                    )
            ));
        }
        if (map.getOrDefault("duplicate_result", 0) > 0 || eval.reviewQuality().disagreementFindings() > 0) {
            improvements.add(new IntelligenceOverviewResponse.RetrievalImprovement(
                    "Reduce duplicate or low-diversity result sets",
                    "medium",
                    "Repeated evidence patterns can make review slower and hide more useful adjacent sources.",
                    List.of(
                            "duplicate_result feedback: " + map.getOrDefault("duplicate_result", 0),
                            "disagreement findings: " + eval.reviewQuality().disagreementFindings()
                    )
            ));
        }
        if (improvements.isEmpty()) {
            improvements.add(new IntelligenceOverviewResponse.RetrievalImprovement(
                    "Maintain current retrieval posture",
                    "low",
                    "No urgent retrieval regressions are visible right now, so continue monitoring feedback and evaluation trends.",
                    List.of("feedback records: " + feedbackCounts.stream().mapToInt(Map.Entry::getValue).sum())
            ));
        }
        return improvements;
    }

    private List<IntelligenceOverviewResponse.EvidenceGap> evidenceGaps(String organizationId, EvaluationDashboardResponse eval) {
        var gaps = new ArrayList<IntelligenceOverviewResponse.EvidenceGap>();
        if (eval.evidenceHealth().insufficientEvidenceAnswers() > 0) {
            gaps.add(new IntelligenceOverviewResponse.EvidenceGap(
                    "gap_insufficient_evidence",
                    "high",
                    "Questions are hitting insufficient-evidence boundaries often enough to justify corpus expansion review.",
                    List.of(
                            "CMS implementation memos and FAQ pages tied to the current prior-auth workflow topics",
                            "Additional HL7 Da Vinci workflow guidance for PAS, CRD, and DTR implementation details"
                    ),
                    List.of(
                            "insufficient evidence answers: " + eval.evidenceHealth().insufficientEvidenceAnswers(),
                            "average retrieval results: " + String.format(Locale.ROOT, "%.2f", eval.evidenceHealth().averageRetrievalResults())
                    )
            ));
        }
        if (eval.sourceHealth().staleSourcesOlderThan30Days() > 0) {
            gaps.add(new IntelligenceOverviewResponse.EvidenceGap(
                    "gap_stale_sources",
                    "medium",
                    "Some active sources are aging and should be revalidated or refreshed before teams rely on them repeatedly.",
                    List.of(
                            "Refresh CMS workflow PDFs and related implementation references",
                            "Review whether newer public interoperability guidance should be admitted into the corpus"
                    ),
                    List.of("stale sources older than 30 days: " + eval.sourceHealth().staleSourcesOlderThan30Days())
            ));
        }
        if (gaps.isEmpty()) {
            gaps.add(new IntelligenceOverviewResponse.EvidenceGap(
                    "gap_none",
                    "low",
                    "No urgent corpus sufficiency gaps are visible right now, but advisory review should continue as new questions appear.",
                    List.of("Continue adding public-source guidance only when repeated question patterns justify it."),
                    List.of("active sources: " + eval.sourceHealth().activeSources())
            ));
        }
        return gaps;
    }

    private List<IntelligenceOverviewResponse.SimilarityCluster> similarityClusters(String organizationId) {
        var briefs = jdbcTemplate.query("""
                select brief_id, question
                from engineering_brief
                where organization_id = ?
                order by created_at desc
                limit 24
                """, (rs, row) -> Map.of("brief_id", rs.getString("brief_id"), "question", rs.getString("question")), organizationId);
        var clusters = new ArrayList<IntelligenceOverviewResponse.SimilarityCluster>();
        var grouped = new LinkedHashMap<String, List<Map<String, String>>>();
        for (var brief : briefs) {
            var theme = theme(String.valueOf(brief.get("question")));
            grouped.computeIfAbsent(theme, key -> new ArrayList<>()).add(brief);
        }
        grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .limit(4)
                .forEach(entry -> clusters.add(new IntelligenceOverviewResponse.SimilarityCluster(
                        "cluster_" + entry.getKey().replace(' ', '_'),
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().map(item -> String.valueOf(item.get("brief_id"))).toList(),
                        List.of(
                                "These briefs share repeated question language and may benefit from reuse of prior review context.",
                                "Keep reuse advisory: reviewers should still compare sources, decisions, and approvals directly."
                        )
                )));
        if (clusters.isEmpty()) {
            clusters.add(new IntelligenceOverviewResponse.SimilarityCluster(
                    "cluster_none",
                    "No repeated brief themes yet",
                    0,
                    List.of(),
                    List.of("Create more related briefs to unlock stronger clustering and reuse recommendations.")
            ));
        }
        return clusters;
    }

    private List<IntelligenceOverviewResponse.PersonaRecommendation> personaRecommendations(String organizationId, AuthenticatedActor actor, EvaluationDashboardResponse eval) {
        var role = actor.role().name().toLowerCase(Locale.ROOT);
        var recommendations = new ArrayList<IntelligenceOverviewResponse.PersonaRecommendation>();
        var draftCount = count("select count(*) from engineering_brief where organization_id = ? and status in ('draft', 'changes_requested')", organizationId);
        var reviewCount = count("select count(*) from engineering_brief where organization_id = ? and status = 'in_review'", organizationId);
        var blockedIntegrations = eval.workflowQuality().blockedIntegrationActions();

        if ("reviewer".equals(role)) {
            recommendations.add(new IntelligenceOverviewResponse.PersonaRecommendation(
                    "persona_reviewer_triage",
                    role,
                    draftCount > 0 ? "brief_triage" : "evidence_review",
                    draftCount > 0 ? "Review the briefs waiting in draft or changes-requested state." : "Capture retrieval feedback on the next grounded brief you inspect.",
                    "Reviewer guidance is based on current queue state and the Phase 13 feedback loop."
            ));
        } else if ("approver".equals(role)) {
            recommendations.add(new IntelligenceOverviewResponse.PersonaRecommendation(
                    "persona_approver_handoff",
                    role,
                    reviewCount > 0 ? "approval_handoff" : "delivery_readiness",
                    reviewCount > 0 ? "Prioritize briefs already in review so accepted findings do not stall." : "Inspect governed delivery receipts before approving the next export path.",
                    "Approver guidance is stage-aware and keeps the workflow review-first."
            ));
        } else if ("auditor".equals(role)) {
            recommendations.add(new IntelligenceOverviewResponse.PersonaRecommendation(
                    "persona_auditor_signals",
                    role,
                    "oversight",
                    blockedIntegrations > 0 ? "Inspect blocked integration actions and the related trust signals." : "Review evidence-gap and tuning recommendations for trend changes.",
                    "Auditor guidance connects evaluation, workflow quality, and operator-visible controls."
            ));
        } else {
            recommendations.add(new IntelligenceOverviewResponse.PersonaRecommendation(
                    "persona_admin_operations",
                    role,
                    "operations",
                    blockedIntegrations > 0 ? "Resolve blocked connector actions and review recovery queue items." : "Inspect inbound intake, orchestration templates, and tuning recommendations together.",
                    "Administrator guidance focuses on the next operational bottleneck rather than automatic execution."
            ));
        }
        return recommendations;
    }

    private List<IntelligenceOverviewResponse.WorkflowTuningRecommendation> workflowTuningRecommendations(String organizationId, EvaluationDashboardResponse eval) {
        var list = new ArrayList<IntelligenceOverviewResponse.WorkflowTuningRecommendation>();
        if (!"pass".equals(eval.qualityGate().decision())) {
            list.add(new IntelligenceOverviewResponse.WorkflowTuningRecommendation(
                    "tuning_quality_gate",
                    "high",
                    "Address quality-gate regressions before widening workflow automation",
                    "Evaluation signals show that retrieval or unsupported-boundary behavior needs attention before making workflows feel more automatic.",
                    eval.qualityGate().regressionSignals()
            ));
        }
        if (eval.reviewQuality().disagreementFindings() > 0) {
            list.add(new IntelligenceOverviewResponse.WorkflowTuningRecommendation(
                    "tuning_disagreements",
                    "medium",
                    "Use disagreement patterns to prioritize review rubric or ranking improvements",
                    "Repeated disagreement on findings suggests the review workflow would benefit from clearer evidence ordering or bounded recommendation hints.",
                    List.of(
                            "disagreement findings: " + eval.reviewQuality().disagreementFindings(),
                            "corrected statements: " + eval.reviewQuality().correctedStatements()
                    )
            ));
        }
        if (eval.workflowQuality().blockedIntegrationActions() > 0) {
            list.add(new IntelligenceOverviewResponse.WorkflowTuningRecommendation(
                    "tuning_delivery_blocks",
                    "medium",
                    "Reduce operator friction around governed delivery handoffs",
                    "Blocked downstream actions indicate a workflow stage where better guidance or defaults would help operators recover faster.",
                    List.of(
                            "blocked integration actions: " + eval.workflowQuality().blockedIntegrationActions(),
                            "preview-only deliveries: " + eval.workflowQuality().previewOnlyDeliveries()
                    )
            ));
        }
        if (list.isEmpty()) {
            list.add(new IntelligenceOverviewResponse.WorkflowTuningRecommendation(
                    "tuning_none",
                    "low",
                    "No urgent workflow tuning items",
                    "Current telemetry does not show an urgent need for workflow retuning, so keep monitoring and collecting reviewer feedback.",
                    List.of("governed deliveries: " + eval.workflowQuality().governedDeliveries())
            ));
        }
        return list;
    }

    private int count(String sql, Object... args) {
        var value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private void ensureFinding(String briefId, String findingId, String organizationId) {
        var count = jdbcTemplate.queryForObject("""
                select count(*) from brief_finding f
                join engineering_brief b on b.brief_id = f.brief_id
                where f.brief_id = ? and f.finding_id = ? and b.organization_id = ?
                """, Integer.class, briefId, findingId, organizationId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding was not found in the current organization.");
        }
    }

    private String theme(String question) {
        var tokens = TOKEN_SPLIT.splitAsStream(question.toLowerCase(Locale.ROOT))
                .map(String::trim)
                .filter(token -> token.length() >= 4)
                .filter(token -> !STOP_WORDS.contains(token))
                .distinct()
                .limit(3)
                .toList();
        return tokens.isEmpty() ? "general review work" : String.join(" / ", tokens);
    }

    private String normalized(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
