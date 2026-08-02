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
class ProductionImplementationProgramControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesPhases51Through55AndImplementationGates() throws Exception {
        mockMvc.perform(get("/v1/enterprise/production-program")
                        .header("X-HealthForge-Actor", "production.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.production.program"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posture").value("production_implementation_contract_ready"))
                .andExpect(jsonPath("$.phases.length()").value(5))
                .andExpect(jsonPath("$.phases[0].phase").value("phase_51"))
                .andExpect(jsonPath("$.phases[4].phase").value("phase_55"))
                .andExpect(jsonPath("$.implementation_gates.length()").value(5));
    }
}
