package dev.healthforge.platform.regulatory;

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
class RegulatedReadinessControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requiresAuditorOrAdministratorForRegulatedReadiness() throws Exception {
        mockMvc.perform(get("/v1/admin/regulated-readiness")
                        .header("X-HealthForge-Actor", "reviewer.one")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsRegulatedReadinessPackForAuditor() throws Exception {
        mockMvc.perform(get("/v1/admin/regulated-readiness")
                        .header("X-HealthForge-Actor", "auditor.one")
                        .header("X-HealthForge-Role", "auditor")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.security_posture.dependency_evidence.length()").value(3))
                .andExpect(jsonPath("$.compliance_evidence_pack.control_mappings.length()").value(4))
                .andExpect(jsonPath("$.deployment_architecture_pack.architecture_views.length()").value(2))
                .andExpect(jsonPath("$.release_governance_pack.release_controls.length()").value(3))
                .andExpect(jsonPath("$.resilience_readiness_pack.recovery_artifacts.length()").value(3));
    }
}
