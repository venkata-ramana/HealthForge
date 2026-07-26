package dev.healthforge.platform.codegen;

import dev.healthforge.platform.brief.BriefResponse;
import dev.healthforge.platform.brief.BriefService;
import dev.healthforge.platform.brief.BriefWorkItemExportResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StarterCodeGenerationServiceTest {

    @Test
    void generatesExampleStarterCodeFromApprovedWorkItem() {
        var briefService = mock(BriefService.class);
        when(briefService.exportWorkItems(eq("brief_123"), any())).thenReturn(new BriefWorkItemExportResponse(
                "brief_123",
                "approved",
                Instant.parse("2026-07-25T17:00:00Z"),
                Instant.parse("2026-07-25T17:05:00Z"),
                "approved_for_export",
                "boundary",
                List.of(new BriefWorkItemExportResponse.WorkItem(
                        "work_abc",
                        "Prior authorization claim intake",
                        "Implement a reviewed intake boundary for prior-authorization claim submission.",
                        "prior_authorization_workflow",
                        List.of("FHIR R4 Claim [resource]", "PAS Claim Profile [profile]"),
                        List.of("Approved export only"),
                        "approved_brief_human_review_retained",
                        List.of("find_1"),
                        List.of(new BriefWorkItemExportResponse.Evidence(
                                "hl7-fhir-r4",
                                "4.0.1",
                                "FHIR R4",
                                "https://hl7.org/fhir/R4/",
                                "section-1",
                                "support",
                                "reviewer.one",
                                Instant.parse("2026-07-25T17:04:00Z"),
                                "Looks good"
                        ))
                )),
                List.of(new BriefResponse.Approval("approval_1", "admin.one", "administrator", Instant.parse("2026-07-25T17:05:00Z"), "Approved")),
                List.of()
        ));

        var service = new StarterCodeGenerationService(briefService);
        var response = service.generate(new StarterCodeGenerationRequest(
                "brief_123",
                "work_abc",
                "spring_boot_endpoint_stub"
        ));

        assertThat(response.status()).isEqualTo("example_only");
        assertThat(response.fileName()).contains("PriorAuthorizationClaimIntake");
        assertThat(response.code()).contains("EXAMPLE STARTER CODE ONLY");
        assertThat(response.code()).contains("Work Item ID: work_abc");
        assertThat(response.traceability().briefId()).isEqualTo("brief_123");
        assertThat(response.traceability().relatedFindingIds()).containsExactly("find_1");
    }
}
