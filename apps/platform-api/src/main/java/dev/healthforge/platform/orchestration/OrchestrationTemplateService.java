package dev.healthforge.platform.orchestration;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class OrchestrationTemplateService {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public OrchestrationTemplateService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OrchestrationTemplateResponse> list(AuthenticatedActor actor) {
        seed(actor.organizationId());
        return jdbcTemplate.query("""
                select template_id, organization_id, name, template_type, summary, default_queue, default_target_system, workflow_phase, guardrails, updated_at
                from orchestration_template
                where organization_id = ?
                order by name
                """, (rs, row) -> new OrchestrationTemplateResponse(
                rs.getString("template_id"),
                rs.getString("organization_id"),
                rs.getString("name"),
                rs.getString("template_type"),
                rs.getString("summary"),
                rs.getString("default_queue"),
                rs.getString("default_target_system"),
                rs.getString("workflow_phase"),
                List.of(rs.getString("guardrails").split("\\|")),
                rs.getTimestamp("updated_at").toInstant()
        ), actor.organizationId());
    }

    private void seed(String organizationId) {
        var count = jdbcTemplate.queryForObject("select count(*) from orchestration_template where organization_id = ?", Integer.class, organizationId);
        if (count != null && count > 0) return;
        var now = Timestamp.from(Instant.now(clock));
        insert(organizationId, "template_provider_prior_auth", "Provider prior auth planning", "prior_auth_program",
                "Start from an intake request, create a grounded Brief, route to reviewer queue, then package approved exports.",
                "reviewer-queue", "github", "brief_to_delivery",
                "Human review required|Approvals required before external delivery|Public and synthetic content only", now);
        insert(organizationId, "template_payer_interop", "Payer interoperability policy review", "policy_program",
                "Coordinate policy findings, evidence review, approval, and documentation publishing for payer-facing work.",
                "approver-queue", "confluence", "review_to_publish",
                "No uncontrolled publishing|Audit trail retained|Connector status must be visible", now);
        insert(organizationId, "template_demo_intake", "Demo intake to showcase workspace", "demo_program",
                "Convert inbound tickets into repeatable demo-safe briefs, assignments, and operator receipts.",
                "reviewer-queue", "notion", "intake_to_showcase",
                "Sandbox-safe wording only|No PHI|Simulated connectors acceptable for demo mode", now);
    }

    private void insert(String organizationId, String id, String name, String type, String summary, String queue, String target, String phase, String guardrails, Timestamp now) {
        jdbcTemplate.update("""
                insert into orchestration_template
                (template_id, organization_id, name, template_type, summary, default_queue, default_target_system, workflow_phase, guardrails, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (template_id) do nothing
                """, id, organizationId, name, type, summary, queue, target, phase, guardrails, now, now);
    }
}
