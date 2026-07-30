package dev.healthforge.platform.workspace;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticatedActorRegistry registry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from workspace_review_escalation where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from workspace_research_notebook where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from workspace_question_pack where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from workspace_assignment where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from workspace_saved_view where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from workspace_research_pack where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from evidence_collection where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from workflow_configuration where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from workspace_group_role_mapping where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from workspace_identity_provider where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from workspace_project_brief where project_id like 'org.workspace.test.%' or project_id like 'project_%'");
        jdbcTemplate.update("delete from workspace_project where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from engineering_brief where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from actor_role_assignment where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from actor_organization_membership where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from actor_organization where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from actor_user where actor_user_id like 'workspace.%'");
    }

    @Test
    void returnsOverviewAndSupportsProjectCreation() throws Exception {
        registry.recordResolvedActor(new AuthenticatedActor("workspace.admin", ActorRole.ADMINISTRATOR, "org.workspace.test", "local_header"));
        jdbcTemplate.update("""
                insert into engineering_brief (brief_id, organization_id, status, created_at, question, project_context, corpus_id, corpus_version)
                values ('brief_workspace_test', 'org.workspace.test', 'draft', now(), 'What changes do we need?', 'Synthetic context', 'mvp-regulatory-corpus', '2026-07-24-expanded-web-core-v4')
                """);

        mockMvc.perform(get("/v1/workspace/overview")
                        .header("X-HealthForge-Actor", "workspace.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.workspace.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").value("org.workspace.test"))
                .andExpect(jsonPath("$.projects.length()").value(2))
                .andExpect(jsonPath("$.auth_foundation.supported_modes[0]").value("local_header"))
                .andExpect(jsonPath("$.research_packs.length()").value(2))
                .andExpect(jsonPath("$.question_packs.length()").value(2))
                .andExpect(jsonPath("$.scenario_templates.length()").value(2))
                .andExpect(jsonPath("$.persona_presets.length()").value(3))
                .andExpect(jsonPath("$.research_notebooks.length()").value(1))
                .andExpect(jsonPath("$.reviewer_operations.total_assignments").value(1));

        mockMvc.perform(post("/v1/workspace/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Provider pilot","kind":"workspace","description":"Pilot workspace","tags":"pilot,provider"}
                                """)
                        .header("X-HealthForge-Actor", "workspace.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.workspace.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Provider pilot"))
                .andExpect(jsonPath("$.tags[0]").value("pilot"));

        mockMvc.perform(post("/v1/workspace/research-packs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "project_id":"",
                                  "name":"Provider recurring research",
                                  "summary":"Reusable analyst prompts for provider planning.",
                                  "recurring_questions":"What changes do we need for CMS prior authorization workflows?\\nHow should a provider workflow handle documentation and status exchange for prior authorization?",
                                  "next_review_date":"2026-08-15"
                                }
                                """)
                        .header("X-HealthForge-Actor", "workspace.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.workspace.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Provider recurring research"))
                .andExpect(jsonPath("$.question_count").value(2));

        mockMvc.perform(post("/v1/workspace/question-packs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "project_id":"",
                                  "name":"Reviewer starter pack",
                                  "summary":"Reusable reviewer prompts for repeated starts.",
                                  "persona":"reviewer",
                                  "template_kind":"analyst_starter",
                                  "starter_question":"What changes do we need for CMS prior authorization workflows?",
                                  "question_prompts":"What changes do we need for CMS prior authorization workflows?\\nWhich passages directly affect documentation?"
                                }
                                """)
                        .header("X-HealthForge-Actor", "workspace.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.workspace.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.persona").value("reviewer"))
                .andExpect(jsonPath("$.question_prompts.length()").value(2));

        mockMvc.perform(post("/v1/workspace/research-notebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "project_id":"",
                                  "brief_id":"brief_workspace_test",
                                  "title":"Reviewer notebook",
                                  "summary":"Bounded notebook for repeated research continuity.",
                                  "key_takeaways":"Capture governing evidence first.\\nFlag stale sources before approval.",
                                  "evidence_bundle_name":"CMS prior auth bundle",
                                  "handoff_summary":"Hand this interpretation to the approver after reviewer checks.",
                                  "continuity_note":"Refresh when corpus evidence changes."
                                }
                                """)
                        .header("X-HealthForge-Actor", "workspace.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.workspace.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Reviewer notebook"))
                .andExpect(jsonPath("$.key_takeaways.length()").value(2));

        mockMvc.perform(post("/v1/workspace/review-escalations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brief_id":"brief_workspace_test",
                                  "escalation_reason":"Evidence questions need reviewer follow-up before approval.",
                                  "urgency":"high",
                                  "destination_queue":"reviewer-queue",
                                  "note":"Bring this back to the reviewer before approver time is spent."
                                }
                                """)
                        .header("X-HealthForge-Actor", "workspace.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.workspace.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urgency").value("high"))
                .andExpect(jsonPath("$.status").value("open"));

        mockMvc.perform(post("/v1/workspace/discovery/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query":"prior authorization",
                                  "facet":"all"
                                }
                                """)
                        .header("X-HealthForge-Actor", "workspace.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.workspace.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("prior authorization"))
                .andExpect(jsonPath("$.total_hits").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }
}
