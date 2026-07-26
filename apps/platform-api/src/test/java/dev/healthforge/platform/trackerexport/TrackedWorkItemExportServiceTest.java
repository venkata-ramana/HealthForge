package dev.healthforge.platform.trackerexport;

import dev.healthforge.platform.auth.ActorRole;
import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.brief.BriefAuditEventService;
import dev.healthforge.platform.brief.BriefResponse;
import dev.healthforge.platform.brief.BriefService;
import dev.healthforge.platform.brief.BriefWorkItemExportResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrackedWorkItemExportServiceTest {

    @Test
    void buildsGithubPreviewAndRecordsAuditEvent() {
        var briefService = mock(BriefService.class);
        var auditEventService = mock(BriefAuditEventService.class);
        var service = new TrackedWorkItemExportService(briefService, auditEventService);

        when(briefService.exportWorkItems(eq("brief-1"), any())).thenReturn(new BriefWorkItemExportResponse(
                "brief-1",
                "approved",
                Instant.parse("2026-07-25T18:00:00Z"),
                Instant.parse("2026-07-25T18:05:00Z"),
                "approved_for_export",
                "preview",
                List.of(new BriefWorkItemExportResponse.WorkItem(
                        "work-1",
                        "Implement PAS claim handling",
                        "Implement PAS claim handling with cited evidence.",
                        "prior_authorization_workflow",
                        List.of("FHIR R4 Claim (4.0.1)"),
                        List.of("Human review required."),
                        "approved_brief_human_review_retained",
                        List.of("find-1"),
                        List.of(new BriefWorkItemExportResponse.Evidence(
                                "cms-0057-f-final-rule",
                                "2024-final",
                                "CMS Interoperability and Prior Authorization Final Rule (CMS-0057-F)",
                                "https://www.cms.gov/files/document/cms-0057-f.pdf",
                                "Page 380",
                                "support",
                                "reviewer.one",
                                Instant.parse("2026-07-25T18:04:00Z"),
                                "accepted"
                        ))
                )),
                List.<BriefResponse.Approval>of(),
                List.<BriefResponse.AuditEvent>of()
        ));

        var response = service.preview(new TrackedWorkItemExportRequest(
                "brief-1",
                "github",
                List.of(),
                true,
                false,
                "Prepare a local engineering backlog preview."
        ), new AuthenticatedActor("admin.one", ActorRole.ADMINISTRATOR));

        assertThat(response.targetSystem()).isEqualTo("github");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().github().title()).isEqualTo("Implement PAS claim handling");
        verify(auditEventService).record(eq("brief-1"), any(), eq("tracker_export_preview_generated"), any(), any());
    }

    @Test
    void rejectsDirectWritebackRequests() {
        var service = new TrackedWorkItemExportService(mock(BriefService.class), mock(BriefAuditEventService.class));

        assertThatThrownBy(() -> service.preview(new TrackedWorkItemExportRequest(
                "brief-1",
                "github",
                List.of(),
                true,
                true,
                "Create tickets now."
        ), new AuthenticatedActor("admin.one", ActorRole.ADMINISTRATOR)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Preview mode only");
    }
}
