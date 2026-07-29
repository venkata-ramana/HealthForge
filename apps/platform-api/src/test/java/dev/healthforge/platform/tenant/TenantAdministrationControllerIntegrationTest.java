package dev.healthforge.platform.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TenantAdministrationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from tenant_provisioning_request where organization_id = 'org.tenant.test'");
        jdbcTemplate.update("delete from workspace_project where organization_id = 'org.tenant.test'");
        jdbcTemplate.update("delete from actor_role_assignment where organization_id = 'org.tenant.test'");
        jdbcTemplate.update("delete from actor_organization_membership where organization_id = 'org.tenant.test'");
        jdbcTemplate.update("delete from actor_user where actor_user_id like 'tenant.%'");
        jdbcTemplate.update("delete from actor_organization where organization_id = 'org.tenant.test'");
    }

    @Test
    void returnsTenantOverviewAnalyticsAndProvisioningWorkflow() throws Exception {
        mockMvc.perform(get("/v1/admin/tenants/overview")
                        .header("X-HealthForge-Actor", "tenant.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.tenant.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested_organization_id").value("org.tenant.test"))
                .andExpect(jsonPath("$.customer_tenants.length()").isNotEmpty())
                .andExpect(jsonPath("$.isolation_boundaries.length()").value(3));

        mockMvc.perform(post("/v1/admin/tenants/provisioning-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenant_key": "tenant_delta_provider",
                                  "tenant_name": "Delta Provider Network",
                                  "deployment_model": "private_customer_space",
                                  "environment_shape": "single-tenant-private",
                                  "delegated_admin": "tenant.delta.admin",
                                  "requested_capabilities": ["team_workspace", "developer_workflows"],
                                  "onboarding_summary": "Private provider tenant with delegated admin ownership."
                                }
                                """)
                        .header("X-HealthForge-Actor", "tenant.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.tenant.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant_key").value("tenant_delta_provider"))
                .andExpect(jsonPath("$.requested_capabilities.length()").value(2));

        mockMvc.perform(get("/v1/admin/tenants/provisioning-requests")
                        .header("X-HealthForge-Actor", "tenant.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.tenant.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organization_id").value("org.tenant.test"));

        mockMvc.perform(get("/v1/admin/tenants/analytics")
                        .header("X-HealthForge-Actor", "tenant.admin")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "org.tenant.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested_organization_id").value("org.tenant.test"))
                .andExpect(jsonPath("$.usage_summary.total_tenants").isNotEmpty())
                .andExpect(jsonPath("$.product_packaging.length()").value(2));
    }
}
