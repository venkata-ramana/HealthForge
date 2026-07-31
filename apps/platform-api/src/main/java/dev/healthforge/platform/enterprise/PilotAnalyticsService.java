package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class PilotAnalyticsService {

    private static final List<String> FEEDBACK_TYPES = List.of(
            "evidence_quality",
            "reviewer_confidence",
            "recommendation_usefulness"
    );

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public PilotAnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PilotAnalyticsResponse analytics(AuthenticatedActor actor) {
        var organizationId = actor.organizationId();
        var questionStarts = count("select count(*) from engineering_brief where organization_id = ?", organizationId);
        var briefCreated = questionStarts;
        var briefsReviewed = count("select count(distinct brief_id) from brief_review_decision where organization_id = ?", organizationId);
        var briefsApproved = count("select count(*) from engineering_brief where organization_id = ? and status = 'approved'", organizationId);
        var implementationHandoffs = count("""
                select count(*) from (
                    select distinct brief_id from tracked_export_event where organization_id = ?
                    union
                    select distinct brief_id from documentation_export_event where organization_id = ?
                    union
                    select distinct brief_id from collaboration_notification_event where organization_id = ?
                ) handoffs
                """, organizationId, organizationId, organizationId);

        var feedback = feedbackSummary(organizationId);
        var funnel = funnel(questionStarts, briefCreated, briefsReviewed, briefsApproved, implementationHandoffs);
        var readiness = expansionReadiness(funnel, feedback.totalRecords());

        return new PilotAnalyticsResponse(
                organizationId,
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                Instant.now(clock),
                funnel,
                new PilotAnalyticsResponse.OutcomeSummary(
                        "approved Briefs with governed implementation handoff",
                        "Handoff counts represent tracked, documentation, or collaboration delivery events; they do not prove external system completion.",
                        List.of(
                                new PilotAnalyticsResponse.OutcomeSignal("approved Briefs", briefsApproved, "briefs", "Shows grounded work that passed the product approval stage."),
                                new PilotAnalyticsResponse.OutcomeSignal("implementation handoffs", implementationHandoffs, "handoffs", "Shows that approved work reached a governed delivery or collaboration surface."),
                                new PilotAnalyticsResponse.OutcomeSignal("feedback records", feedback.totalRecords(), "records", "Shows whether teams are closing the loop on evidence and recommendation quality.")
                        )
                ),
                roleActivity(organizationId),
                stakeholderSummary(funnel, feedback, readiness),
                feedback,
                readiness,
                List.of(
                        "Question starts are represented by created engineering Briefs because the current platform does not persist a separate raw-question event stream.",
                        "Analytics are organization-scoped, synthetic-safe, and intended for pilot conversations rather than billing or clinical-performance claims.",
                        "Readiness scores summarize observable product activity; they do not certify production, regulatory, or external-system readiness."
                ),
                "This pilot analytics view connects workflow movement, outcome signals, stakeholder reporting, feedback loops, and expansion gaps in one bounded operating picture."
        );
    }

    public PilotAnalyticsResponse recordFeedback(AuthenticatedActor actor, PilotFeedbackRequest request) {
        var feedbackType = request.feedbackType().trim().toLowerCase(Locale.ROOT);
        if (!FEEDBACK_TYPES.contains(feedbackType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "feedbackType must be evidence_quality, reviewer_confidence, or recommendation_usefulness");
        }
        var note = request.note() == null ? "" : request.note().trim();
        jdbcTemplate.update("""
                insert into pilot_feedback (
                    feedback_id, organization_id, actor_id, actor_role, feedback_type,
                    rating, brief_id, finding_id, note, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "pilot_feedback_" + java.util.UUID.randomUUID(),
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(Locale.ROOT),
                feedbackType,
                request.rating(),
                blankToNull(request.briefId()),
                blankToNull(request.findingId()),
                note,
                Timestamp.from(Instant.now(clock))
        );
        return analytics(actor);
    }

    private PilotAnalyticsResponse.FunnelSummary funnel(int questionStarts, int briefCreated, int briefsReviewed, int briefsApproved, int implementationHandoffs) {
        return new PilotAnalyticsResponse.FunnelSummary(
                questionStarts,
                briefCreated,
                briefsReviewed,
                briefsApproved,
                implementationHandoffs,
                rate(briefCreated, questionStarts),
                rate(briefsApproved, briefCreated),
                rate(implementationHandoffs, briefsApproved),
                List.of(
                        stage("question_start", questionStarts, questionStarts, "Current local telemetry uses created Briefs as the question-start proxy."),
                        stage("brief_created", briefCreated, questionStarts, "A question has been shaped into a structured, evidence-oriented Brief."),
                        stage("brief_reviewed", briefsReviewed, briefCreated, "A reviewer has recorded at least one finding-level decision."),
                        stage("brief_approved", briefsApproved, briefsReviewed, "The Brief has reached the approved workflow state."),
                        stage("implementation_handoff", implementationHandoffs, briefsApproved, "Approved work has reached a tracked delivery, documentation, or collaboration surface.")
                )
        );
    }

    private PilotAnalyticsResponse.FunnelStage stage(String name, int count, int previous, String interpretation) {
        return new PilotAnalyticsResponse.FunnelStage(name, count, Math.max(0, previous - count), interpretation);
    }

    private List<PilotAnalyticsResponse.RoleActivity> roleActivity(String organizationId) {
        return List.of("reviewer", "approver", "auditor", "administrator").stream()
                .map(role -> {
                    var audits = count("select count(*) from brief_audit_event where organization_id = ? and actor_role = ?", organizationId, role);
                    var feedback = count("select count(*) from pilot_feedback where organization_id = ? and actor_role = ?", organizationId, role);
                    var signal = audits + feedback == 0 ? "no activity recorded" : audits >= feedback ? "workflow activity is leading" : "feedback activity is leading";
                    return new PilotAnalyticsResponse.RoleActivity(role, audits, feedback, signal);
                })
                .toList();
    }

    private PilotAnalyticsResponse.StakeholderSummary stakeholderSummary(
            PilotAnalyticsResponse.FunnelSummary funnel,
            PilotAnalyticsResponse.FeedbackSummary feedback,
            PilotAnalyticsResponse.ExpansionReadiness readiness
    ) {
        return new PilotAnalyticsResponse.StakeholderSummary(
                readiness.currentStage().replace('_', ' '),
                List.of(
                        new PilotAnalyticsResponse.StakeholderMetric("workflow completion", funnel.approvalToHandoffRate() + "%", "pilot sponsor", "Share of approved Briefs with an observable governed handoff."),
                        new PilotAnalyticsResponse.StakeholderMetric("review conversion", funnel.briefToApprovalRate() + "%", "delivery lead", "Share of created Briefs reaching approval."),
                        new PilotAnalyticsResponse.StakeholderMetric("feedback coverage", String.valueOf(feedback.totalRecords()), "product and quality teams", "Number of structured feedback records available for tuning conversations."),
                        new PilotAnalyticsResponse.StakeholderMetric("expansion score", readiness.score() + "/100", "enterprise stakeholders", "Evidence-oriented progression signal, not a production-readiness certification.")
                ),
                List.of(
                        "The product loop can be discussed as a measurable path from evidence question to approved Brief and governed handoff.",
                        "Drop-off is visible by stage, which gives pilot sponsors a clearer improvement conversation than feature counts alone.",
                        "Feedback volume makes quality and usefulness discussions more concrete over time."
                ),
                List.of(
                        "Where do reviewers stall before approval?",
                        "Which handoff path is most valuable to the pilot team?",
                        "What evidence would be required before broader rollout?"
                )
        );
    }

    private PilotAnalyticsResponse.ExpansionReadiness expansionReadiness(PilotAnalyticsResponse.FunnelSummary funnel, int feedbackRecords) {
        var checks = List.of(
                check("workflow_started", "Pilot workflow has started", funnel.questionStarts() > 0, funnel.questionStarts() + " question-start proxy record(s)"),
                check("human_review", "Human review is observable", funnel.briefsReviewed() > 0, funnel.briefsReviewed() + " reviewed Brief(s)"),
                check("approval_gate", "Approval gate is exercised", funnel.briefsApproved() > 0, funnel.briefsApproved() + " approved Brief(s)"),
                check("governed_handoff", "A governed handoff path is exercised", funnel.implementationHandoffs() > 0, funnel.implementationHandoffs() + " implementation handoff(s)"),
                check("feedback_loop", "Feedback loop is active", feedbackRecords > 0, feedbackRecords + " feedback record(s)")
        );
        var passed = (int) checks.stream().filter(check -> "in_place".equals(check.status())).count();
        var score = passed * 20;
        var currentStage = score >= 80 ? "pilot_to_rollout_candidate" : score >= 40 ? "private_pilot_ready" : "demo_ready";
        var nextStage = score >= 80 ? "broader_rollout_readiness" : score >= 40 ? "pilot_to_rollout_candidate" : "private_pilot_ready";
        var gaps = checks.stream()
                .filter(check -> "planned".equals(check.status()))
                .map(check -> check.title() + ": " + check.evidence())
                .toList();
        return new PilotAnalyticsResponse.ExpansionReadiness(currentStage, score, nextStage, checks, gaps);
    }

    private PilotAnalyticsResponse.ReadinessCheck check(String id, String title, boolean passed, String evidence) {
        return new PilotAnalyticsResponse.ReadinessCheck(id, title, passed ? "in_place" : "planned", evidence);
    }

    private PilotAnalyticsResponse.FeedbackSummary feedbackSummary(String organizationId) {
        var byType = jdbcTemplate.query("""
                select feedback_type, count(*) as records, avg(rating) as average_rating
                from pilot_feedback
                where organization_id = ?
                group by feedback_type
                order by feedback_type
                """, (rs, row) -> new PilotAnalyticsResponse.FeedbackTypeSummary(
                rs.getString("feedback_type"),
                rs.getInt("records"),
                rs.getDouble("average_rating")
        ), organizationId);
        var recent = jdbcTemplate.query("""
                select feedback_id, feedback_type, rating, actor_role, brief_id, finding_id, note, created_at
                from pilot_feedback
                where organization_id = ?
                order by created_at desc
                limit 10
                """, (rs, row) -> new PilotAnalyticsResponse.FeedbackRecord(
                rs.getString("feedback_id"),
                rs.getString("feedback_type"),
                rs.getInt("rating"),
                rs.getString("actor_role"),
                rs.getString("brief_id"),
                rs.getString("finding_id"),
                rs.getString("note"),
                rs.getTimestamp("created_at").toInstant()
        ), organizationId);
        return new PilotAnalyticsResponse.FeedbackSummary(
                byType.stream().mapToInt(PilotAnalyticsResponse.FeedbackTypeSummary::records).sum(),
                byType,
                recent
        );
    }

    private int rate(int numerator, int denominator) {
        return denominator == 0 ? 0 : (int) Math.round((numerator * 100.0) / denominator);
    }

    private int count(String sql, Object... args) {
        var value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
