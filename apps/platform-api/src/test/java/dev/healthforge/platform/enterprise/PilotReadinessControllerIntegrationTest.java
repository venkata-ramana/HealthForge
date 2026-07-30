package dev.healthforge.platform.enterprise;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PilotReadinessControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from pilot_success_checkpoint where organization_id like 'tenant.pilot.%'");
    }

    @Test
    void returnsReadinessForAuditor() throws Exception {
        mockMvc.perform(get("/v1/pilot/readiness")
                        .header("X-HealthForge-Actor", "pilot.auditor")
                        .header("X-HealthForge-Role", "auditor")
                        .header("X-HealthForge-Organization", "tenant.pilot.alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").value("tenant.pilot.alpha"))
                .andExpect(jsonPath("$.readiness_summary.readiness_tier").value("private_pilot_ready"))
                .andExpect(jsonPath("$.checklist[0].item_id").value("solution-packs"));
    }

    @Test
    void returnsExpandedSolutionPacksForReviewer() throws Exception {
        mockMvc.perform(get("/v1/pilot/solution-packs")
                        .header("X-HealthForge-Actor", "pilot.reviewer")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.pilot.alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").value("tenant.pilot.alpha"))
                .andExpect(jsonPath("$.packs.length()").value(6))
                .andExpect(jsonPath("$.packs[0].pack_id").value("prior_auth_provider"))
                .andExpect(jsonPath("$.packs[0].domain").value("prior_authorization"))
                .andExpect(jsonPath("$.workflow_presets.length()").value(4))
                .andExpect(jsonPath("$.workflow_presets[0].preset_id").value("retrieval_review_path"))
                .andExpect(jsonPath("$.onboarding_flows.length()").value(3))
                .andExpect(jsonPath("$.stakeholder_packs.length()").value(3))
                .andExpect(jsonPath("$.bounded_statements.length()").value(3));
    }

    @Test
    void rejectsAuditorForSolutionPacks() throws Exception {
        mockMvc.perform(get("/v1/pilot/solution-packs")
                        .header("X-HealthForge-Actor", "pilot.auditor")
                        .header("X-HealthForge-Role", "auditor")
                        .header("X-HealthForge-Organization", "tenant.pilot.alpha"))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsSeededSuccessPlanForReviewer() throws Exception {
        mockMvc.perform(get("/v1/pilot/success")
                        .header("X-HealthForge-Actor", "pilot.reviewer")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.pilot.success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").value("tenant.pilot.success"))
                .andExpect(jsonPath("$.checkpoints.length()").value(4))
                .andExpect(jsonPath("$.follow_up_patterns.length()").value(3));
    }
}
