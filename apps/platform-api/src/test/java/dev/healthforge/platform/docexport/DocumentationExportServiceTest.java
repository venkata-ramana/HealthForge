package dev.healthforge.platform.docexport;

import dev.healthforge.platform.auth.ActorRole;
import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.automation.WorkflowAutomationService;
import dev.healthforge.platform.brief.BriefAuditExportResponse;
import dev.healthforge.platform.brief.BriefResponse;
import dev.healthforge.platform.brief.BriefService;
import dev.healthforge.platform.brief.BriefWorkItemExportResponse;
import dev.healthforge.platform.integration.GovernedConnectorGateway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentationExportServiceTest {

    @Test
    void packagesApprovedArtifactForPreview() {
        var briefService = mock(BriefService.class);
        var jdbcTemplate = mock(JdbcTemplate.class);
        var automationService = mock(WorkflowAutomationService.class);
        var connectorGateway = mock(GovernedConnectorGateway.class);
        var service = new DocumentationExportService(briefService, jdbcTemplate, automationService, connectorGateway);

        when(briefService.get(eq("brief-1"), any())).thenReturn(new BriefResponse(
                "brief-1",
                "approved",
                Instant.parse("2026-07-28T10:00:00Z"),
                new BriefResponse.Input("What changes do we need?", "Synthetic planning", "corpus", "v1"),
                List.of(),
                List.of(),
                "Summary",
                List.of(),
                List.of(),
                List.of(new BriefResponse.Approval("approval_1", "approver.one", "approver", Instant.parse("2026-07-28T11:00:00Z"), "Ready")),
                List.of(),
                true
        ));
        when(briefService.exportWorkItems(eq("brief-1"), any())).thenReturn(new BriefWorkItemExportResponse(
                "brief-1",
                "approved",
                Instant.parse("2026-07-28T10:00:00Z"),
                Instant.parse("2026-07-28T11:05:00Z"),
                "approved_for_export",
                "JSON export only.",
                List.of(new BriefWorkItemExportResponse.WorkItem(
                        "work-1",
                        "Implement PAS handling",
                        "Rationale",
                        "prior_authorization_workflow",
                        "provider",
                        "request_submission",
                        List.of("dependency"),
                        List.of("FHIR R4"),
                        List.of("Human review required"),
                        "approved_brief_human_review_retained",
                        List.of("find-1"),
                        List.of()
                )),
                List.of(),
                List.of(new BriefResponse.Approval("approval_1", "approver.one", "approver", Instant.parse("2026-07-28T11:00:00Z"), "Ready")),
                List.of()
        ));

        var response = service.export(new DocumentationExportRequest(
                "brief-1",
                "notion",
                "markdown",
                true,
                false,
                "approval_1",
                null,
                "Prepare oversight package."
        ), new AuthenticatedActor("approver.one", ActorRole.APPROVER, "tenant.alpha", "local_header"));

        assertThat(response.deliveryStatus()).isEqualTo("preview_generated");
        assertThat(response.packageBody()).contains("HealthForge approved artifact package");
        assertThat(response.receiptType()).isEqualTo("documentation_receipt");
    }
}
