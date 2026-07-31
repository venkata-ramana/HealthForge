package dev.healthforge.platform.enterprise;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PilotAnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from pilot_feedback where organization_id like 'tenant.analytics.%'");
    }

    @Test
    void returnsBoundedAnalyticsForAuditor() throws Exception {
        mockMvc.perform(get("/v1/pilot/analytics")
                        .header("X-HealthForge-Actor", "analytics.auditor")
                        .header("X-HealthForge-Role", "auditor")
                        .header("X-HealthForge-Organization", "tenant.analytics.alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").value("tenant.analytics.alpha"))
                .andExpect(jsonPath("$.funnel.question_starts").value(0))
                .andExpect(jsonPath("$.expansion_readiness.current_stage").value("demo_ready"))
                .andExpect(jsonPath("$.bounded_statements.length()").value(3));
    }

    @Test
    void recordsFeedbackForReviewerAndReturnsUpdatedAnalytics() throws Exception {
        mockMvc.perform(post("/v1/pilot/analytics/feedback")
                        .header("X-HealthForge-Actor", "analytics.reviewer")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.analytics.feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "feedback_type", "reviewer_confidence",
                                "rating", 4,
                                "note", "The cited evidence was easy to review."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedback.total_records").value(1))
                .andExpect(jsonPath("$.feedback.by_type[0].feedback_type").value("reviewer_confidence"))
                .andExpect(jsonPath("$.feedback.recent[0].rating").value(4))
                .andExpect(jsonPath("$.expansion_readiness.checks[4].status").value("in_place"));
    }

    @Test
    void rejectsUnsupportedFeedbackType() throws Exception {
        mockMvc.perform(post("/v1/pilot/analytics/feedback")
                        .header("X-HealthForge-Actor", "analytics.reviewer")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.analytics.invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "feedback_type", "other",
                                "rating", 3
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsReviewerFromAnalyticsRead() throws Exception {
        mockMvc.perform(get("/v1/pilot/analytics")
                        .header("X-HealthForge-Actor", "analytics.reviewer")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.analytics.alpha"))
                .andExpect(status().isForbidden());
    }
}
