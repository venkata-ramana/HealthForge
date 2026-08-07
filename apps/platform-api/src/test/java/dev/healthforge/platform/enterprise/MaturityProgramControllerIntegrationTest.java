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
class MaturityProgramControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void exposesPhases76Through80AndMaturityGates() throws Exception {
        mockMvc.perform(get("/v1/enterprise/maturity-program")
                        .header("X-HealthForge-Actor", "maturity.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.maturity.program"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posture").value("platform_maturity_foundation_ready"))
                .andExpect(jsonPath("$.phases.length()").value(5))
                .andExpect(jsonPath("$.phases[0].phase").value("phase_76"))
                .andExpect(jsonPath("$.phases[4].phase").value("phase_80"))
                .andExpect(jsonPath("$.maturity_gates.length()").value(5));
    }
}
