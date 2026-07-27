package dev.healthforge.platform.journey;

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

class PriorAuthJourneyServiceTest {

    @Test
    void buildsPasJourneyWithTransitionsAndTouchpoints() {
        var groundedAnswerService = mock(GroundedAnswerService.class);
        var standardsArtifactService = mock(StandardsArtifactService.class);

        when(groundedAnswerService.answer(any())).thenReturn(new GroundedAnswerResponse(
                "answer-1",
                "grounded",
                Instant.parse("2026-07-27T12:00:00Z"),
                "How should PAS claim submission work?",
                "Grounded answer",
                List.of(),
                List.of(),
                "Human review required"
        ));
        when(standardsArtifactService.list(any(), eq("claim"))).thenReturn(new StandardsArtifactResponse(List.of(
                new StandardsArtifactResponse.Artifact(
                        "fhir-r4-claim",
                        "http://hl7.org/fhir/StructureDefinition/Claim",
                        "FHIR R4 Claim",
                        "4.0.1",
                        "resource",
                        "hl7.fhir.r4.core",
                        "hl7-fhir-r4",
                        "4.0.1",
                        "review",
                        List.of("https://hl7.org/fhir/R4/claim.html"),
                        List.of("claim")
                )
        )));
        when(standardsArtifactService.list(any(), eq("pas"))).thenReturn(new StandardsArtifactResponse(List.of(
                new StandardsArtifactResponse.Artifact(
                        "pas-claim-profile",
                        "http://hl7.org/fhir/us/davinci-pas/StructureDefinition/profile-claim",
                        "PAS Claim Profile",
                        "2.1.0",
                        "profile",
                        "hl7.fhir.us.davinci-pas",
                        "davinci-pas-stu-2-1",
                        "STU2.1",
                        "review",
                        List.of("https://hl7.org/fhir/us/davinci-pas/STU2.1/StructureDefinition-profile-claim.html"),
                        List.of("pas", "claim")
                )
        )));
        when(standardsArtifactService.list(any(), eq("claimresponse"))).thenReturn(new StandardsArtifactResponse(List.of()));

        var service = new PriorAuthJourneyService(groundedAnswerService, standardsArtifactService);

        var response = service.build(new PriorAuthJourneyRequest(
                "mvp-regulatory-corpus",
                "2026-07-24-expanded-web-core-v4",
                "How should PAS claim submission work?",
                "Synthetic provider scenario",
                null
        ));

        assertThat(response.status()).isEqualTo("grounded");
        assertThat(response.journeyType()).isEqualTo("PAS");
        assertThat(response.workflowStages()).hasSize(3);
        assertThat(response.stateTransitions()).hasSize(2);
        assertThat(response.standardsTouchpoints()).isNotEmpty();
    }

    @Test
    void infersDtrJourneyFromDocumentationScenario() {
        var groundedAnswerService = mock(GroundedAnswerService.class);
        var standardsArtifactService = mock(StandardsArtifactService.class);

        when(groundedAnswerService.answer(any())).thenReturn(new GroundedAnswerResponse(
                "answer-1",
                "grounded",
                Instant.parse("2026-07-27T12:00:00Z"),
                "How should documentation capture work?",
                "Grounded answer",
                List.of(),
                List.of(),
                "Human review required"
        ));
        when(standardsArtifactService.list(any(), eq("dtr"))).thenReturn(new StandardsArtifactResponse(List.of()));

        var service = new PriorAuthJourneyService(groundedAnswerService, standardsArtifactService);

        var response = service.build(new PriorAuthJourneyRequest(
                "mvp-regulatory-corpus",
                "2026-07-24-expanded-web-core-v4",
                "How should DTR documentation capture work?",
                "Synthetic documentation scenario",
                null
        ));

        assertThat(response.journeyType()).isEqualTo("DTR");
        assertThat(response.workflowStages()).extracting(PriorAuthJourneyResponse.WorkflowStage::stageId)
                .containsExactly("documentation_launch", "documentation_capture", "documentation_return");
    }
}
