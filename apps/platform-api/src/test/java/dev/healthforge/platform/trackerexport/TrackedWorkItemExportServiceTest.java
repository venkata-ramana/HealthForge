package dev.healthforge.platform.trackerexport;

import dev.healthforge.platform.auth.ActorRole;
import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.brief.BriefAuditEventService;
import dev.healthforge.platform.brief.BriefResponse;
import dev.healthforge.platform.brief.BriefService;
import dev.healthforge.platform.brief.BriefWorkItemExportResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
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
                        "provider",
                        "request_submission",
                        List.of("approved_brief_review", "request_packaging_boundary"),
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
                List.of(new BriefWorkItemExportResponse.ImplementationTrack(
                        "track_provider",
                        "Provider implementation track",
                        "provider",
                        "Provider-facing workflow planning.",
                        List.of("approved_brief_review", "request_packaging_boundary"),
                        List.of("request_submission"),
                        List.of("FHIR R4 Claim (4.0.1)"),
                        List.of("work-1")
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
                "Prepare a local engineering backlog preview.",
                null,
                null,
                null
        ), new AuthenticatedActor("admin.one", ActorRole.ADMINISTRATOR));

        assertThat(response.targetSystem()).isEqualTo("github");
        assertThat(response.mode()).isEqualTo("preview_only");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().github().title()).isEqualTo("Implement PAS claim handling");
        assertThat(response.writebackExecution().executionStatus()).isEqualTo("preview_generated");
        verify(auditEventService).record(eq("brief-1"), any(), eq("tracker_export_preview_generated"), any(), any());
    }

    @Test
    void blocksWritebackWithoutApprovalRecord() {
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
                        "provider",
                        "request_submission",
                        List.of("approved_brief_review", "request_packaging_boundary"),
                        List.of("FHIR R4 Claim (4.0.1)"),
                        List.of("Human review required."),
                        "approved_brief_human_review_retained",
                        List.of("find-1"),
                        List.of()
                )),
                List.of(),
                List.of(),
                List.of()
        ));

        var response = service.preview(new TrackedWorkItemExportRequest(
                "brief-1",
                "github",
                List.of(),
                true,
                true,
                "Create tickets now.",
                null,
                "openai/healthforge",
                null
        ), new AuthenticatedActor("admin.one", ActorRole.ADMINISTRATOR));

        assertThat(response.mode()).isEqualTo("governed_writeback");
        assertThat(response.writebackEnabled()).isTrue();
        assertThat(response.writebackExecution().executionStatus()).isEqualTo("writeback_blocked");
        verify(auditEventService).record(eq("brief-1"), any(), eq("tracker_writeback_blocked"), any(), any());
    }

    @Test
    void executesGovernedWritebackWithApprovalAndRetryMetadata() {
        var briefService = mock(BriefService.class);
        var auditEventService = mock(BriefAuditEventService.class);
        var jdbcTemplate = mock(org.springframework.jdbc.core.JdbcTemplate.class);
        var service = new TrackedWorkItemExportService(briefService, auditEventService, jdbcTemplate);

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
                        "provider",
                        "request_submission",
                        List.of("approved_brief_review", "request_packaging_boundary"),
                        List.of("FHIR R4 Claim (4.0.1)"),
                        List.of("Human review required."),
                        "approved_brief_human_review_retained",
                        List.of("find-1"),
                        List.of()
                )),
                List.of(),
                List.of(),
                List.of()
        ));
        when(jdbcTemplate.queryForList(contains("from brief_approval"), eq("approval_1"), eq("brief-1"), eq("tenant.alpha")))
                .thenReturn(List.of(new java.util.LinkedHashMap<>(Map.of(
                        "approval_id", "approval_1",
                        "approver", "approver.one",
                        "approver_role", "approver",
                        "approved_at", Timestamp.from(Instant.parse("2026-07-28T12:00:00Z"))
                ))));
        when(jdbcTemplate.queryForList(contains("from tracked_export_event"), eq("tracked_export_event_prev"), eq("brief-1"), eq("tenant.alpha")))
                .thenReturn(List.of(new java.util.LinkedHashMap<>(Map.of(
                        "tracked_export_event_id", "tracked_export_event_prev",
                        "retry_count", 0
                ))));

        var response = service.preview(new TrackedWorkItemExportRequest(
                "brief-1",
                "github",
                List.of(),
                true,
                true,
                "Create approved ticket.",
                "approval_1",
                "openai/healthforge",
                "tracked_export_event_prev"
        ), new AuthenticatedActor("admin.one", ActorRole.ADMINISTRATOR, "tenant.alpha", "local_header"));

        assertThat(response.mode()).isEqualTo("governed_writeback");
        assertThat(response.writebackEnabled()).isTrue();
        assertThat(response.approvalGate().approvalId()).isEqualTo("approval_1");
        assertThat(response.writebackExecution().executionStatus()).isEqualTo("writeback_retried");
        assertThat(response.writebackExecution().retryCount()).isEqualTo(1);
        assertThat(response.writebackExecution().externalReference()).contains("github.com/openai/healthforge/issues/retry-1");
        verify(auditEventService).record(eq("brief-1"), any(), eq("tracker_writeback_executed"), any(), any());
    }
}
