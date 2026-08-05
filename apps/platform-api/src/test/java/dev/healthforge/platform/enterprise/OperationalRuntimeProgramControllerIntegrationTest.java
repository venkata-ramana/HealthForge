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
class OperationalRuntimeProgramControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void exposesPhases71Through75AndRuntimeGates() throws Exception {
        mockMvc.perform(get("/v1/enterprise/operational-runtime")
                        .header("X-HealthForge-Actor", "runtime.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.operational.runtime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posture").value("operational_runtime_foundation_ready"))
                .andExpect(jsonPath("$.phases.length()").value(5))
                .andExpect(jsonPath("$.phases[0].phase").value("phase_71"))
                .andExpect(jsonPath("$.phases[4].phase").value("phase_75"))
                .andExpect(jsonPath("$.runtime_gates.length()").value(5));
    }
}
