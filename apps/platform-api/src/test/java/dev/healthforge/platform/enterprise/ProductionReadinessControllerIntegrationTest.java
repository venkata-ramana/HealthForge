package dev.healthforge.platform.enterprise;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductionReadinessControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsAllPhaseScorecardsForAuditor() throws Exception {
        mockMvc.perform(get("/v1/enterprise/production-readiness")
                        .header("X-HealthForge-Actor", "readiness.auditor")
                        .header("X-HealthForge-Role", "auditor")
                        .header("X-HealthForge-Organization", "tenant.readiness.empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").value("tenant.readiness.empty"))
                .andExpect(jsonPath("$.decision").value("not_ready"))
                .andExpect(jsonPath("$.phases.length()").value(5))
                .andExpect(jsonPath("$.phases[0].phase_id").value("phase_26"))
                .andExpect(jsonPath("$.phases[4].phase_id").value("phase_30"))
                .andExpect(jsonPath("$.bounded_statements.length()").value(3));
    }

    @Test
    void rejectsReviewerFromProductionReadinessGate() throws Exception {
        mockMvc.perform(get("/v1/enterprise/production-readiness")
                        .header("X-HealthForge-Actor", "readiness.reviewer")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.readiness.empty"))
                .andExpect(status().isForbidden());
    }
}
