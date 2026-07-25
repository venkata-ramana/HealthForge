package dev.healthforge.platform.copilot;

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

class PriorAuthCopilotServiceTest {

    @Test
    void infersPasScenarioAndWorkflowStage() {
        var groundedAnswerService = mock(GroundedAnswerService.class);
        var standardsArtifactService = mock(StandardsArtifactService.class);

        when(groundedAnswerService.answer(any())).thenReturn(new GroundedAnswerResponse(
                "answer-1",
                "grounded",
                Instant.parse("2026-07-25T18:00:00Z"),
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
        when(standardsArtifactService.list(any(), eq("pas"))).thenReturn(new StandardsArtifactResponse(List.of()));

        var service = new PriorAuthCopilotService(groundedAnswerService, standardsArtifactService);

        var response = service.analyze(new PriorAuthCopilotRequest(
                "mvp-regulatory-corpus",
                "2026-07-24-expanded-web-core-v4",
                "How should PAS claim submission work?",
                "Synthetic provider scenario",
                null
        ));

        assertThat(response.status()).isEqualTo("grounded");
        assertThat(response.scenarioType()).isEqualTo("PAS");
        assertThat(response.workflowStage()).isEqualTo("submission_preparation");
        assertThat(response.standardsTouchpoints()).isNotEmpty();
    }
}
