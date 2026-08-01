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
class ControlledRolloutControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from controlled_rollout_evidence where organization_id like 'tenant.rollout.%'");
    }

    @Test
    void returnsFiveExecutionScorecardsForAuditor() throws Exception {
        mockMvc.perform(get("/v1/enterprise/controlled-rollout")
                        .header("X-HealthForge-Actor", "rollout.auditor")
                        .header("X-HealthForge-Role", "auditor")
                        .header("X-HealthForge-Organization", "tenant.rollout.empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").value("tenant.rollout.empty"))
                .andExpect(jsonPath("$.phases.length()").value(5))
                .andExpect(jsonPath("$.phases[0].phase_id").value("phase_31"))
                .andExpect(jsonPath("$.phases[4].phase_id").value("phase_35"))
                .andExpect(jsonPath("$.decision").value("not_ready"));
    }

    @Test
    void recordsAdministratorEvidenceForAReadinessCheck() throws Exception {
        mockMvc.perform(post("/v1/enterprise/controlled-rollout/evidence")
                        .header("X-HealthForge-Actor", "rollout.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "tenant.rollout.evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phase_id", "phase_31",
                                "check_id", "identity_provider",
                                "status", "in_place",
                                "owner_role", "administrator",
                                "evidence_summary", "Trusted proxy configuration reviewed.",
                                "next_action", "Recheck during the next release review."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidence.length()").value(1))
                .andExpect(jsonPath("$.evidence[0].check_id").value("identity_provider"))
                .andExpect(jsonPath("$.phases[0].checks[0].status").value("in_place"));
    }

    @Test
    void rejectsReviewerFromRecordingRolloutEvidence() throws Exception {
        mockMvc.perform(post("/v1/enterprise/controlled-rollout/evidence")
                        .header("X-HealthForge-Actor", "rollout.reviewer")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.rollout.empty")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phase_id", "phase_31",
                                "check_id", "identity_provider",
                                "status", "in_place",
                                "owner_role", "administrator",
                                "evidence_summary", "not allowed",
                                "next_action", "not allowed"
                        ))))
                .andExpect(status().isForbidden());
    }
}
