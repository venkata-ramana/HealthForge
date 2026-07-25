package dev.healthforge.platform.architecture;

import dev.healthforge.platform.answer.GroundedAnswerResponse;
import dev.healthforge.platform.answer.GroundedAnswerService;
import dev.healthforge.platform.standards.StandardsArtifactResponse;
import dev.healthforge.platform.standards.StandardsArtifactService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArchitectureReviewServiceTest {

    @Test
    void returnsStructuredArchitectureReviewArtifact() {
        var groundedAnswerService = mock(GroundedAnswerService.class);
        var standardsArtifactService = mock(StandardsArtifactService.class);

        when(groundedAnswerService.answer(any())).thenReturn(new GroundedAnswerResponse(
                "ans_1",
                "grounded",
                Instant.parse("2026-07-25T17:00:00Z"),
                "How should we design prior authorization claim exchange?",
                "Grounded answer",
                List.of(new GroundedAnswerResponse.EvidenceFinding(
                        "finding_1",
                        "PAS and Claim workflows require reviewable submission and status handling.",
                        new GroundedAnswerResponse.Citation(
                                "passage_1",
                                "davinci-pas-stu-2-1",
                                "STU2.1",
                                "candidate_technical_guidance",
                                "Da Vinci PAS",
                                "https://hl7.org/fhir/us/davinci-pas/STU2.1/",
                                "section-2",
                                "Describes request and response patterns."
                        )
                )),
                List.of("Human review required"),
                "Use only as cited planning support."
        ));

        when(standardsArtifactService.list(eq(null), eq("claim"))).thenReturn(new StandardsArtifactResponse(List.of(
                new StandardsArtifactResponse.Artifact("fhir-r4-claim", "http://hl7.org/fhir/StructureDefinition/Claim", "FHIR R4 Claim", "4.0.1", "resource", "hl7.fhir.r4.core", "hl7-fhir-r4", "4.0.1", "boundary", List.of(), List.of())
        )));
        when(standardsArtifactService.list(eq(null), eq("pas"))).thenReturn(new StandardsArtifactResponse(List.of(
                new StandardsArtifactResponse.Artifact("pas-claim-profile", "http://hl7.org/fhir/us/davinci-pas/StructureDefinition/profile-claim", "PAS Claim Profile", "2.1.0", "profile", "hl7.fhir.us.davinci-pas", "davinci-pas-stu-2-1", "STU2.1", "boundary", List.of(), List.of())
        )));
        when(standardsArtifactService.list(eq(null), eq("claimresponse"))).thenReturn(new StandardsArtifactResponse(List.of()));
        when(standardsArtifactService.list(eq(null), eq("crd"))).thenReturn(new StandardsArtifactResponse(List.of()));
        when(standardsArtifactService.list(eq(null), eq("dtr"))).thenReturn(new StandardsArtifactResponse(List.of()));

        var service = new ArchitectureReviewService(groundedAnswerService, standardsArtifactService);
        var response = service.review(new ArchitectureReviewRequest(
                "mvp-regulatory-corpus",
                "2026-07-24-expanded-web-core-v4",
                "How should we design prior authorization claim exchange with PAS?",
                "Synthetic architecture planning scenario for a provider EHR integration.",
                List.of("candidate_technical_guidance", "governing_regulation")
        ));

        assertThat(response.status()).isEqualTo("grounded");
        assertThat(response.humanReviewRequired()).isTrue();
        assertThat(response.components()).isNotEmpty();
        assertThat(response.integrations()).isNotEmpty();
        assertThat(response.standardsTouchpoints()).contains("FHIR R4 Claim [resource]", "PAS Claim Profile [profile]");
        assertThat(response.risks()).extracting(ArchitectureReviewResponse.Risk::name)
                .contains("Applicability mismatch", "Version mismatch");
        assertThat(response.evidenceFindings()).hasSize(1);
    }
}
