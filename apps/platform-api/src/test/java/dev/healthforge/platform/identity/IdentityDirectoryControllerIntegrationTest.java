package dev.healthforge.platform.identity;

import dev.healthforge.platform.auth.ActorRole;
import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.auth.AuthenticatedActorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IdentityDirectoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticatedActorRegistry registry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from actor_role_assignment where actor_user_id like 'controller.%'");
        jdbcTemplate.update("delete from actor_organization_membership where actor_user_id like 'controller.%'");
        jdbcTemplate.update("delete from actor_user where actor_user_id like 'controller.%'");
        jdbcTemplate.update("delete from actor_organization where organization_id like 'org.controller.%'");
    }

    @Test
    void defaultsDirectoryViewToAdministratorsOrganization() throws Exception {
        registry.recordResolvedActor(new AuthenticatedActor("controller.admin", ActorRole.ADMINISTRATOR, "org.controller.alpha", "local_header"));

        mockMvc.perform(get("/v1/admin/identity-directory")
                        .header("X-HealthForge-Actor", "controller.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.controller.alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested_organization_id").value("org.controller.alpha"))
                .andExpect(jsonPath("$.organizations[0].organization_id").value("org.controller.alpha"));
    }

    @Test
    void rejectsCrossTenantDirectoryRequests() throws Exception {
        registry.recordResolvedActor(new AuthenticatedActor("controller.admin", ActorRole.ADMINISTRATOR, "org.controller.alpha", "local_header"));
        registry.recordResolvedActor(new AuthenticatedActor("controller.other", ActorRole.REVIEWER, "org.controller.beta", "local_header"));

        mockMvc.perform(get("/v1/admin/identity-directory")
                        .param("organization_id", "org.controller.beta")
                        .header("X-HealthForge-Actor", "controller.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.controller.alpha"))
                .andExpect(status().isForbidden());
    }
}
