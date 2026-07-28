package dev.healthforge.platform.evaluation;

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
class EvaluationDashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requiresAuditorOrAdministratorForDashboard() throws Exception {
        mockMvc.perform(get("/v1/evaluation/dashboard")
                        .header("X-HealthForge-Actor", "reviewer.one")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsEvaluationDashboardForAuditor() throws Exception {
        mockMvc.perform(get("/v1/evaluation/dashboard")
                        .header("X-HealthForge-Actor", "auditor.one")
                        .header("X-HealthForge-Role", "auditor")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quality_gate.gate_id").value("mvp-retrieval-quality-gate-v2"));
    }

    @Test
    void returnsPolicySafetyReportForAuditor() throws Exception {
        mockMvc.perform(get("/v1/evaluation/policy-safety-report")
                        .header("X-HealthForge-Actor", "auditor.one")
                        .header("X-HealthForge-Role", "auditor")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy_version").isNotEmpty());
    }
}
