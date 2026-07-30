package dev.healthforge.platform.integration;

import dev.healthforge.platform.auth.ActorRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IntegrationOperationsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IntegrationOperationsService service;

    @Test
    void allowsAdministratorToInspectAuditExportAndGovernanceChecks() throws Exception {
        when(service.auditExport(any())).thenReturn(new IntegrationAuditExportResponse(
                "tenant.alpha",
                Instant.parse("2026-07-30T12:00:00Z"),
                new IntegrationStatusResponse.ReconciliationSummary(4, 2, 1, 1, 1, 0, "Operator summary"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("Audit export note")
        ));
        when(service.governanceCheck(any(), any())).thenReturn(new IntegrationGovernanceCheckResponse(
                "tenant.alpha",
                Instant.parse("2026-07-30T12:05:00Z"),
                "github",
                "tracker_writeback",
                "live",
                "governed_preview_only",
                false,
                new IntegrationGovernanceCheckResponse.ApprovalGateSummary(
                        "approval_missing", null, null, null, null, "Approval is required."
                ),
                new IntegrationGovernanceCheckResponse.EnvironmentPolicySummary(
                        true, "simulated", false, "openai/healthforge", "Preview-only posture."
                ),
                List.of("Use preview first."),
                List.of("Approval trace stays mandatory.")
        ));

        mockMvc.perform(get("/v1/integrations/audit-export")
                        .header("X-HealthForge-Actor", "admin.one")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization_id").value("tenant.alpha"))
                .andExpect(jsonPath("$.audit_notes[0]").value("Audit export note"));

        mockMvc.perform(post("/v1/integrations/governance-checks")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "connector_type":"github",
                                  "action_type":"tracker_writeback",
                                  "brief_id":"brief-1",
                                  "approval_id":"",
                                  "target_locator":"openai/healthforge",
                                  "requested_execution_mode":"live"
                                }
                                """)
                        .header("X-HealthForge-Actor", "admin.one")
                        .header("X-HealthForge-Role", "administrator")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connector_type").value("github"))
                .andExpect(jsonPath("$.final_decision").value("governed_preview_only"));

        verify(service).auditExport(any());
        verify(service).governanceCheck(any(), any());
    }

    @Test
    void rejectsReviewerForIntegrationAdminEndpoints() throws Exception {
        mockMvc.perform(get("/v1/integrations/audit-export")
                        .header("X-HealthForge-Actor", "reviewer.one")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/v1/integrations/governance-checks")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "connector_type":"github",
                                  "action_type":"tracker_writeback"
                                }
                                """)
                        .header("X-HealthForge-Actor", "reviewer.one")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}
