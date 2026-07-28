package dev.healthforge.platform.intelligence;

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
class IntelligenceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from retrieval_feedback where organization_id = 'org.intelligence.test'");
        jdbcTemplate.update("delete from brief_review_decision where organization_id = 'org.intelligence.test'");
        jdbcTemplate.update("delete from brief_finding where brief_id = 'brief_intelligence_test'");
        jdbcTemplate.update("delete from engineering_brief where brief_id = 'brief_intelligence_test'");
    }

    @Test
    void recordsFeedbackAndReturnsOverview() throws Exception {
        jdbcTemplate.update("""
                insert into engineering_brief (brief_id, organization_id, status, created_at, question, project_context, corpus_id, corpus_version)
                values ('brief_intelligence_test', 'org.intelligence.test', 'draft', now(), 'How should a provider workflow handle documentation and status exchange?', 'Synthetic planning', 'mvp-regulatory-corpus', '2026-07-24-expanded-web-core-v4')
                """);
        jdbcTemplate.update("""
                insert into brief_finding (finding_id, brief_id, kind, statement, confidence, source_id, source_version, locator, support)
                values ('find_intelligence_test', 'brief_intelligence_test', 'interpretation', 'Synthetic finding', 'medium', 'cms-0057-f-final-rule', '2024-final', 'Page 1', 'support')
                """);

        mockMvc.perform(post("/v1/intelligence/retrieval-feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "brief_id": "brief_intelligence_test",
                                  "finding_id": "find_intelligence_test",
                                  "feedback_type": "missing_evidence",
                                  "source_id": "cms-0057-f-final-rule",
                                  "note": "Need stronger workflow-specific evidence."
                                }
                                """)
                        .header("X-HealthForge-Actor", "reviewer.intelligence")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "org.intelligence.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback_type").value("missing_evidence"));

        mockMvc.perform(get("/v1/intelligence/overview")
                        .header("X-HealthForge-Actor", "reviewer.intelligence")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "org.intelligence.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").value("org.intelligence.test"))
                .andExpect(jsonPath("$.retrieval_improvements.length()").isNotEmpty())
                .andExpect(jsonPath("$.persona_recommendations[0].persona").value("reviewer"));
    }
}
