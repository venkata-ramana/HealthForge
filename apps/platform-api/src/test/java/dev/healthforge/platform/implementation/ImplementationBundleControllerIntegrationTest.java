package dev.healthforge.platform.implementation;

import dev.healthforge.platform.architecture.ArchitectureReviewResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ImplementationBundleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImplementationBundleService service;

    @Test
    void allowsApproverToLoadReleaseBundleData() throws Exception {
        when(service.generate(any(), any())).thenReturn(new ImplementationBundleResponse(
                "brief-1",
                "tenant.alpha",
                "approver.one",
                "approver",
                Instant.parse("2026-07-30T12:00:00Z"),
                new ImplementationBundleResponse.HandoffSummary("Question?", "approved", 1, 1, "summary"),
                List.of(),
                new ImplementationBundleResponse.TestPlan(List.of(), List.of(), List.of(), List.of()),
                new ImplementationBundleResponse.ChangeImpact(List.of(), List.of(), "stable"),
                new ImplementationBundleResponse.HandoffBundle(
                        null,
                        new ArchitectureReviewResponse(
                                "review-1", "grounded", Instant.parse("2026-07-30T11:00:00Z"),
                                new ArchitectureReviewResponse.Input("corpus", "v1", "Question?", "Context"),
                                "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), true, "notice"
                        ),
                        List.of("implementation summary"),
                        List.of("artifact")
                ),
                new ImplementationBundleResponse.ReleaseBundle(
                        "grouped_for_downstream_handoff",
                        List.of(new ImplementationBundleResponse.ArtifactGroup("Reviewed planning artifacts", "implementation_team", List.of("approved Brief context"))),
                        List.of(new ImplementationBundleResponse.DownstreamPackage("pkg1", "implementation_team", "json", List.of("work_item_export"), "Preserve reasoning")),
                        List.of("traceability summary"),
                        "Operator handoff summary"
                ),
                List.of(),
                List.of("guardrail")
        ));

        mockMvc.perform(get("/v1/implementation/briefs/brief-1/bundle")
                        .header("X-HealthForge-Actor", "approver.one")
                        .header("X-HealthForge-Role", "approver")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.release_bundle.release_status").value("grouped_for_downstream_handoff"))
                .andExpect(jsonPath("$.release_bundle.downstream_packages[0].package_id").value("pkg1"));

        verify(service).generate(any(), any());
    }

    @Test
    void rejectsReviewerForImplementationBundle() throws Exception {
        mockMvc.perform(get("/v1/implementation/briefs/brief-1/bundle")
                        .header("X-HealthForge-Actor", "reviewer.one")
                        .header("X-HealthForge-Role", "reviewer")
                        .header("X-HealthForge-Organization", "tenant.alpha"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}
