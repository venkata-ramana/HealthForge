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
        jdbcTemplate.update("delete from workspace_assignment where organization_id = 'org.workspace.test'");
        jdbcTemplate.update("delete from workspace_saved_view where organization_id = 'org.workspace.test'");
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
                .andExpect(jsonPath("$.auth_foundation.supported_modes[0]").value("local_header"));

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
    }
}
