package dev.healthforge.platform.enterprise;

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
class AccessReviewReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticatedActorRegistry registry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from actor_role_assignment where actor_user_id like 'access.%'");
        jdbcTemplate.update("delete from actor_organization_membership where actor_user_id like 'access.%'");
        jdbcTemplate.update("delete from actor_user where actor_user_id like 'access.%'");
        jdbcTemplate.update("delete from actor_organization where organization_id like 'tenant.access.%'");
    }

    @Test
    void returnsOrganizationScopedAccessReviewForAdministrator() throws Exception {
        registry.recordResolvedActor(new AuthenticatedActor("access.admin", ActorRole.ADMINISTRATOR, "tenant.access.alpha", "local_header"));
        registry.recordResolvedActor(new AuthenticatedActor("access.approver", ActorRole.APPROVER, "tenant.access.alpha", "local_header"));

        mockMvc.perform(get("/v1/admin/access-review")
                        .header("X-HealthForge-Actor", "access.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "tenant.access.alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").value("tenant.access.alpha"))
                .andExpect(jsonPath("$.access_summary.total_users").value(2))
                .andExpect(jsonPath("$.access_summary.administrator_assignments").value(1))
                .andExpect(jsonPath("$.access_summary.approver_assignments").value(1))
                .andExpect(jsonPath("$.audit_policy.policy_version").value("private-demo-v1"));
    }

    @Test
    void rejectsNonAdministratorForAccessReview() throws Exception {
        registry.recordResolvedActor(new AuthenticatedActor("access.auditor", ActorRole.AUDITOR, "tenant.access.alpha", "local_header"));

        mockMvc.perform(get("/v1/admin/access-review")
                        .header("X-HealthForge-Actor", "access.auditor")
                        .header("X-HealthForge-Role", "auditor")
                        .header("X-HealthForge-Organization", "tenant.access.alpha"))
                .andExpect(status().isForbidden());
    }
}
