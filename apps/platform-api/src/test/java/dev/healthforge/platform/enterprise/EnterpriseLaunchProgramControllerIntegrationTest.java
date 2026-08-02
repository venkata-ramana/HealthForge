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
class EnterpriseLaunchProgramControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesPhases41Through45AndGoNoGoGates() throws Exception {
        mockMvc.perform(get("/v1/enterprise/launch-program")
                        .header("X-HealthForge-Actor", "launch.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.launch.program"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.program_status").value("bounded_enterprise_pilot_ready"))
                .andExpect(jsonPath("$.phases.length()").value(5))
                .andExpect(jsonPath("$.phases[0].phase").value("phase_41"))
                .andExpect(jsonPath("$.phases[4].phase").value("phase_45"))
                .andExpect(jsonPath("$.go_no_go_gates.length()").value(5));
    }
}
