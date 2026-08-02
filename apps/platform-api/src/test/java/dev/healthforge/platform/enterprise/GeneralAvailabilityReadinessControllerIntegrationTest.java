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
class GeneralAvailabilityReadinessControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesPhases46Through50AndGaGates() throws Exception {
        mockMvc.perform(get("/v1/enterprise/ga-readiness")
                        .header("X-HealthForge-Actor", "ga.auditor")
                        .header("X-HealthForge-Role", "auditor")
                        .header("X-HealthForge-Organization", "org.ga.readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posture").value("ga_candidate_not_ga_approved"))
                .andExpect(jsonPath("$.phases.length()").value(5))
                .andExpect(jsonPath("$.phases[0].phase").value("phase_46"))
                .andExpect(jsonPath("$.phases[4].phase").value("phase_50"))
                .andExpect(jsonPath("$.ga_gates.length()").value(5));
    }
}
