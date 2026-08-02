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
class SaasReadinessControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesPhases37Through40AndLaunchGates() throws Exception {
        mockMvc.perform(get("/v1/enterprise/saas-readiness")
                        .header("X-HealthForge-Actor", "saas.auditor")
                        .header("X-HealthForge-Role", "auditor")
                        .header("X-HealthForge-Organization", "org.saas.readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posture").value("saas_ready_private_pilot_foundation"))
                .andExpect(jsonPath("$.phases.length()").value(4))
                .andExpect(jsonPath("$.phases[0].phase").value("phase_37"))
                .andExpect(jsonPath("$.phases[3].phase").value("phase_40"))
                .andExpect(jsonPath("$.launch_gates.length()").value(4));
    }
}
