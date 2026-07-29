package dev.healthforge.platform.developer;

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
class DeveloperWorkflowControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from brief_approval where organization_id = 'org.developer.test'");
        jdbcTemplate.update("delete from brief_review_decision where organization_id = 'org.developer.test'");
        jdbcTemplate.update("delete from brief_finding where brief_id = 'brief_developer_test'");
        jdbcTemplate.update("delete from brief_source where brief_id = 'brief_developer_test'");
        jdbcTemplate.update("delete from engineering_brief where brief_id = 'brief_developer_test'");
    }

    @Test
    void returnsOverviewAndRepoGuidance() throws Exception {
        jdbcTemplate.update("""
                insert into engineering_brief (brief_id, organization_id, status, created_at, question, project_context, corpus_id, corpus_version)
                values ('brief_developer_test', 'org.developer.test', 'approved', now(),
                'How should PAS claim submission flow through a provider workflow?',
                'Synthetic provider implementation planning',
                'mvp-regulatory-corpus',
                '2026-07-24-expanded-web-core-v4')
                """);
        jdbcTemplate.update("""
                insert into brief_source (brief_id, source_id, source_version, source_type, title, canonical_url)
                values ('brief_developer_test', 'cms-0057-f-final-rule', '2024-final', 'regulation', 'CMS final rule', 'https://example.test/cms-rule')
                """);
        jdbcTemplate.update("""
                insert into brief_finding (finding_id, brief_id, kind, statement, confidence, source_id, source_version, locator, support)
                values ('find_developer_test', 'brief_developer_test', 'interpretation',
                'PAS submission should preserve workflow traceability.',
                'medium', 'cms-0057-f-final-rule', '2024-final', 'Page 1', 'Grounded support')
                """);
        jdbcTemplate.update("""
                insert into brief_review_decision (review_id, brief_id, finding_id, organization_id, decision, reviewer, decided_at, rationale, corrected_statement)
                values ('review_developer_test', 'brief_developer_test', 'find_developer_test', 'org.developer.test',
                'accept', 'developer.reviewer', now(), 'Grounded and implementation-ready.', null)
                """);
        jdbcTemplate.update("""
                insert into brief_approval (approval_id, brief_id, organization_id, approver, approver_role, approved_at, rationale)
                values ('approval_developer_test', 'brief_developer_test', 'org.developer.test',
                'developer.approver', 'approver', now(), 'Approved for implementation guidance.')
                """);

        mockMvc.perform(get("/v1/developer/overview")
                        .header("X-HealthForge-Actor", "developer.reviewer")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "org.developer.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").value("org.developer.test"))
                .andExpect(jsonPath("$.workspace_surfaces.length()").value(3))
                .andExpect(jsonPath("$.approved_briefs[0].brief_id").value("brief_developer_test"));

        mockMvc.perform(post("/v1/developer/repo-guidance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brief_id": "brief_developer_test",
                                  "repository_name": "HealthForge",
                                  "workspace_root": "/Users/ivn/code/HealthForge",
                                  "repository_inventory": [
                                    "apps/platform-api/src/main/java/dev/healthforge/platform/implementation/ImplementationBundleService.java",
                                    "apps/platform-api/src/main/java/dev/healthforge/platform/brief/BriefController.java",
                                    "apps/platform-api/src/main/resources/static/app.js"
                                  ],
                                  "changed_files": [
                                    "apps/platform-api/src/main/resources/static/app.js"
                                  ]
                                }
                                """)
                        .header("X-HealthForge-Actor", "developer.approver")
                        .header("X-HealthForge-Role", "approver")
                        .header("X-HealthForge-Organization", "org.developer.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repository_name").value("HealthForge"))
                .andExpect(jsonPath("$.implementation_focus.length()").isNotEmpty())
                .andExpect(jsonPath("$.file_suggestions.length()").isNotEmpty())
                .andExpect(jsonPath("$.automation_steps.length()").value(4));
    }
}
