package dev.healthforge.platform.crosswalk;

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

class PriorAuthStandardsCrosswalkServiceTest {

    @Test
    void buildsInspectablePasCrosswalkFromGroundedFindings() {
        var groundedAnswerService = mock(GroundedAnswerService.class);
        var standardsArtifactService = mock(StandardsArtifactService.class);

        when(groundedAnswerService.answer(any())).thenReturn(new GroundedAnswerResponse(
                "ans_1",
                "grounded",
                Instant.parse("2026-07-27T14:00:00Z"),
                "What changes do we need for CMS prior authorization workflows?",
                "Grounded answer",
                List.of(
                        new GroundedAnswerResponse.EvidenceFinding(
                                "finding_1",
                                "The workflow should support submission of prior authorization requests and reviewer-visible follow-up handling.",
                                new GroundedAnswerResponse.Citation(
                                        "passage_1",
                                        "cms-0057-f-final-rule",
                                        "2026-07-24-expanded-web-core-v4",
                                        "governing_regulation",
                                        "CMS Final Rule",
                                        "https://www.cms.gov/files/document/cms-0057-f.pdf",
                                        "page-1",
                                        "Supports prior authorization workflow planning."
                                )
                        ),
                        new GroundedAnswerResponse.EvidenceFinding(
                                "finding_2",
                                "Coverage and documentation requirements should be surfaced early in the provider workflow.",
                                new GroundedAnswerResponse.Citation(
                                        "passage_2",
                                        "cms-prior-auth-api-workflow",
                                        "2026-07-24-expanded-web-core-v4",
                                        "candidate_technical_guidance",
                                        "CMS Prior Auth Workflow",
                                        "https://example.org/workflow",
                                        "page-2",
                                        "Supports discovery and documentation mapping."
                                )
                        )
                ),
                List.of(),
                "Human review required"
        ));

        when(standardsArtifactService.list(eq(null), eq("claim"))).thenReturn(new StandardsArtifactResponse(List.of(
                new StandardsArtifactResponse.Artifact("fhir-r4-claim", "http://hl7.org/fhir/StructureDefinition/Claim", "FHIR R4 Claim", "4.0.1", "resource", "hl7.fhir.r4.core", "hl7-fhir-r4", "4.0.1", "review", List.of(), List.of("claim"))
        )));
        when(standardsArtifactService.list(eq(null), eq("pas"))).thenReturn(new StandardsArtifactResponse(List.of(
                new StandardsArtifactResponse.Artifact("pas-submit-operation", "https://hl7.org/fhir/us/davinci-pas/STU2.1/", "PAS $submit workflow touchpoint", "2.1.0", "operation", "hl7.fhir.us.davinci-pas", "davinci-pas-stu-2-1", "STU2.1", "review", List.of(), List.of("pas"))
        )));
        when(standardsArtifactService.list(eq(null), eq("claimresponse"))).thenReturn(new StandardsArtifactResponse(List.of(
                new StandardsArtifactResponse.Artifact("fhir-r4-claimresponse", "http://hl7.org/fhir/StructureDefinition/ClaimResponse", "FHIR R4 ClaimResponse", "4.0.1", "resource", "hl7.fhir.r4.core", "hl7-fhir-r4", "4.0.1", "review", List.of(), List.of("claimresponse"))
        )));
        when(standardsArtifactService.list(eq(null), eq("crd"))).thenReturn(new StandardsArtifactResponse(List.of(
                new StandardsArtifactResponse.Artifact("crd-implementation-guide", "https://hl7.org/fhir/us/davinci-crd/STU2.1/", "Da Vinci Coverage Requirements Discovery (CRD) STU 2.1", "2.1.0", "implementation_guide", "hl7.fhir.us.davinci-crd", "davinci-crd-stu-2-1", "STU2.1", "review", List.of(), List.of("crd"))
        )));
        when(standardsArtifactService.list(eq(null), eq("coverage"))).thenReturn(new StandardsArtifactResponse(List.of(
                new StandardsArtifactResponse.Artifact("crd-discovery-workflow", "https://hl7.org/fhir/us/davinci-crd/STU2.1/", "CRD discovery workflow touchpoint", "2.1.0", "operation", "hl7.fhir.us.davinci-crd", "davinci-crd-stu-2-1", "STU2.1", "review", List.of(), List.of("coverage"))
        )));
        when(standardsArtifactService.list(eq(null), eq("dtr"))).thenReturn(new StandardsArtifactResponse(List.of()));
        when(standardsArtifactService.list(eq(null), eq("questionnaire"))).thenReturn(new StandardsArtifactResponse(List.of()));

        var service = new PriorAuthStandardsCrosswalkService(groundedAnswerService, standardsArtifactService);
        var response = service.build(new PriorAuthStandardsCrosswalkRequest(
                "mvp-regulatory-corpus",
                "2026-07-24-expanded-web-core-v4",
                "What changes do we need for CMS prior authorization workflows?",
                "Synthetic provider EHR planning scenario for prior authorization APIs.",
                null
        ));

        assertThat(response.status()).isEqualTo("grounded");
        assertThat(response.journeyType()).isEqualTo("PAS");
        assertThat(response.requirementCrosswalks()).hasSize(2);
        assertThat(response.requirementCrosswalks()).extracting(PriorAuthStandardsCrosswalkResponse.RequirementCrosswalk::workflowStage)
                .contains("decision_and_follow_up", "documentation_capture");
        assertThat(response.requirementCrosswalks().get(0).artifacts()).isNotEmpty();
        assertThat(response.artifactSummaries()).isNotEmpty();
    }

    @Test
    void scaffoldsCrosswalkWhenEvidenceIsInsufficient() {
        var groundedAnswerService = mock(GroundedAnswerService.class);
        var standardsArtifactService = mock(StandardsArtifactService.class);

        when(groundedAnswerService.answer(any())).thenReturn(new GroundedAnswerResponse(
                "ans_2",
                "insufficient_evidence",
                Instant.parse("2026-07-27T14:00:00Z"),
                "How should we map DTR requirements?",
                null,
                List.of(),
                List.of("Insufficient evidence"),
                "Human review required"
        ));
        when(standardsArtifactService.list(eq(null), eq("dtr"))).thenReturn(new StandardsArtifactResponse(List.of(
                new StandardsArtifactResponse.Artifact("dtr-implementation-guide", "https://hl7.org/fhir/us/davinci-dtr/", "Da Vinci Documentation Templates and Rules (DTR)", "2.1.0", "implementation_guide", "hl7.fhir.us.davinci-dtr", "davinci-dtr-stu-2-1", "STU2.1", "review", List.of(), List.of("dtr"))
        )));

        var service = new PriorAuthStandardsCrosswalkService(groundedAnswerService, standardsArtifactService);
        var response = service.build(new PriorAuthStandardsCrosswalkRequest(
                "mvp-regulatory-corpus",
                "2026-07-24-expanded-web-core-v4",
                "How should we map DTR requirements?",
                "Synthetic documentation scenario",
                null
        ));

        assertThat(response.status()).isEqualTo("insufficient_evidence");
        assertThat(response.journeyType()).isEqualTo("DTR");
        assertThat(response.requirementCrosswalks()).hasSize(1);
        assertThat(response.reviewerWarnings()).isNotEmpty();
    }
}
