package dev.healthforge.platform.trackerexport;

import dev.healthforge.platform.auth.ActorRole;
import dev.healthforge.platform.auth.AuthenticatedActor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TrackedWorkItemExportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackedWorkItemExportService service;

    @Test
    void allowsApproverToGenerateTrackedPreview() throws Exception {
        when(service.preview(any(), any())).thenReturn(response());

                mockMvc.perform(post("/v1/tracker-exports/preview")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "brief_id": "brief-1",
                                  "target_system": "github",
                                  "work_item_ids": [],
                                  "approval_acknowledgement": true,
                                  "writeback_requested": false,
                                  "export_reason": "Prepare engineering backlog preview.",
                                  "writeback_approval_id": null,
                                  "target_locator": null,
                                  "retry_from_export_id": null
                                }
                                """)
                        .header("X-HealthForge-Actor", "approver.one")
                        .header("X-HealthForge-Role", "approver")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target_system").value("github"));

        verify(service).preview(any(), argThat(actor ->
                actor.actorId().equals("approver.one")
                        && actor.role() == ActorRole.APPROVER
                        && actor.organizationId().equals("tenant.alpha")));
    }

    @Test
    void rejectsReviewerForTrackedPreview() throws Exception {
                mockMvc.perform(post("/v1/tracker-exports/preview")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "brief_id": "brief-1",
                                  "target_system": "github",
                                  "work_item_ids": [],
                                  "approval_acknowledgement": true,
                                  "writeback_requested": false,
                                  "export_reason": "Prepare engineering backlog preview.",
                                  "writeback_approval_id": null,
                                  "target_locator": null,
                                  "retry_from_export_id": null
                                }
                                """)
                        .header("X-HealthForge-Actor", "reviewer.one")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    private TrackedWorkItemExportResponse response() {
        return new TrackedWorkItemExportResponse(
                "tracked_export_1",
                "brief-1",
                "github",
                "preview_only",
                Instant.parse("2026-07-26T12:00:00Z"),
                "approver.one",
                "approver",
                List.of(),
                List.of("Preview only."),
                false,
                null,
                new TrackedWorkItemExportResponse.WritebackExecution(
                        "preview_generated",
                        "Preview payload generated without external writeback.",
                        null,
                        null,
                        0,
                        null,
                        null
                ),
                "Human review remains required."
        );
    }
}
