package dev.healthforge.platform.intake;

import dev.healthforge.platform.auth.ActorRole;
import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.auth.AuthenticatedActorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InboundCaseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticatedActorRegistry registry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from inbound_case where organization_id = 'org.intake.test'");
        jdbcTemplate.update("delete from tracked_export_event where organization_id = 'org.intake.test'");
        jdbcTemplate.update("delete from documentation_export_event where organization_id = 'org.intake.test'");
        jdbcTemplate.update("delete from brief_approval where organization_id = 'org.intake.test'");
        jdbcTemplate.update("delete from engineering_brief where organization_id = 'org.intake.test'");
    }

    @Test
    void listsInboundCasesWithWorkflowLineage() throws Exception {
        registry.recordResolvedActor(new AuthenticatedActor("reviewer.one", ActorRole.REVIEWER, "org.intake.test", "local_header"));
        jdbcTemplate.update("""
                insert into engineering_brief (brief_id, organization_id, status, created_at, question, project_context, corpus_id, corpus_version)
                values ('brief_intake_test', 'org.intake.test', 'approved', now(), 'How should prior auth intake link to delivery?', 'Synthetic context', 'mvp-regulatory-corpus', '2026-07-24-expanded-web-core-v4')
                """);
        jdbcTemplate.update("""
                insert into brief_approval (approval_id, brief_id, organization_id, approver, approver_role, approved_at, rationale)
                values ('approval_intake_test', 'brief_intake_test', 'org.intake.test', 'approver.one', 'approver', now(), 'Approved for downstream handoff.')
                """);
        jdbcTemplate.update("""
                insert into tracked_export_event (
                    tracked_export_event_id, brief_id, organization_id, actor_id, actor_role, target_system,
                    export_mode, work_item_count, export_reason, retention_until, occurred_at,
                    writeback_approval_id, approval_actor_id, approval_actor_role, approval_recorded_at,
                    target_locator, execution_status, execution_result, external_reference,
                    retry_count, retried_from_event_id, executed_at
                ) values (
                    'tracked_export_intake_test', 'brief_intake_test', 'org.intake.test', 'approver.one', 'approver', 'github',
                    'governed_writeback', 1, 'Synthetic export', now(), now(),
                    'approval_intake_test', 'approver.one', 'approver', now(),
                    'openai/healthforge', 'simulated_execution', 'Simulated writeback', 'github://openai/healthforge/sim-1',
                    0, null, now()
                )
                """);
        jdbcTemplate.update("""
                insert into documentation_export_event (
                    documentation_export_event_id, brief_id, organization_id, actor_id, actor_role,
                    target_system, export_mode, package_format, approval_id, target_locator,
                    delivery_status, external_reference, trace_summary, retention_until, occurred_at
                ) values (
                    'documentation_export_intake_test', 'brief_intake_test', 'org.intake.test', 'approver.one', 'approver',
                    'confluence', 'governed_publish', 'markdown', 'approval_intake_test', 'HealthForge/PA',
                    'published', 'confluence://HealthForge/PA/page-1', 'Synthetic trace', now(), now()
                )
                """);

        mockMvc.perform(post("/v1/intake/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source_system":"jira",
                                  "external_case_id":"HF-101",
                                  "title":"Provider prior auth follow-up",
                                  "summary":"Synthetic inbound case for lineage testing.",
                                  "requested_role":"reviewer",
                                  "requested_assignee":"reviewer.one",
                                  "source_locator":"https://jira.local/HF-101",
                                  "create_brief":false
                                }
                                """)
                        .header("X-HealthForge-Actor", "reviewer.one")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "org.intake.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflow_lineage.latest_delivery_status").value("no_linked_brief"));

        jdbcTemplate.update("""
                update inbound_case set linked_brief_id = 'brief_intake_test', intake_status = 'brief_created'
                where organization_id = 'org.intake.test'
                """);

        mockMvc.perform(get("/v1/intake/cases")
                        .header("X-HealthForge-Actor", "reviewer.one")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "org.intake.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].linked_brief_id").value("brief_intake_test"))
                .andExpect(jsonPath("$[0].workflow_lineage.linked_brief_status").value("approved"))
                .andExpect(jsonPath("$[0].workflow_lineage.approval_count").value(1))
                .andExpect(jsonPath("$[0].workflow_lineage.tracked_export_count").value(1))
                .andExpect(jsonPath("$[0].workflow_lineage.documentation_export_count").value(1));
    }
}
