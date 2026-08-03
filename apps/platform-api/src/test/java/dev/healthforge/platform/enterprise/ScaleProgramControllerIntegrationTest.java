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
class ScaleProgramControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void exposesPhases61Through65AndScaleGates() throws Exception {
        mockMvc.perform(get("/v1/enterprise/scale-program")
                        .header("X-HealthForge-Actor", "scale.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.scale.program"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posture").value("scale_foundation_ready"))
                .andExpect(jsonPath("$.phases.length()").value(5))
                .andExpect(jsonPath("$.phases[0].phase").value("phase_61"))
                .andExpect(jsonPath("$.phases[4].phase").value("phase_65"))
                .andExpect(jsonPath("$.scale_gates.length()").value(5));
    }
}
