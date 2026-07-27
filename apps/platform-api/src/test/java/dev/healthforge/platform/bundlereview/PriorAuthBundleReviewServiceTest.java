package dev.healthforge.platform.bundlereview;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.healthforge.platform.answer.GroundedAnswerResponse;
import dev.healthforge.platform.answer.GroundedAnswerService;
import dev.healthforge.platform.fhir.FhirValidationResponse;
import dev.healthforge.platform.fhir.FhirValidationService;
import dev.healthforge.platform.fhirsynthetic.SyntheticFhirGenerateResponse;
import dev.healthforge.platform.fhirsynthetic.SyntheticFhirService;
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

class PriorAuthBundleReviewServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsStructuredBundleScenarioReview() {
        var syntheticFhirService = mock(SyntheticFhirService.class);
        var fhirValidationService = mock(FhirValidationService.class);
        var groundedAnswerService = mock(GroundedAnswerService.class);
        var standardsArtifactService = mock(StandardsArtifactService.class);

        var bundle = objectMapper.createObjectNode()
                .put("resourceType", "Bundle")
                .put("id", "synthetic-prior-auth-bundle-valid")
                .put("type", "collection");
        var entries = objectMapper.createArrayNode();
        entries.add(objectMapper.createObjectNode().set("resource", objectMapper.createObjectNode()
                .put("resourceType", "Coverage")
                .put("id", "coverage-1")
                .set("beneficiary", objectMapper.createObjectNode().put("reference", "Patient/example"))));
        entries.add(objectMapper.createObjectNode().set("resource", objectMapper.createObjectNode()
                .put("resourceType", "Claim")
                .put("id", "claim-1")
                .set("insurance", objectMapper.createArrayNode().add(objectMapper.createObjectNode()
                        .set("coverage", objectMapper.createObjectNode().put("reference", "Coverage/coverage-1"))))));
        bundle.set("entry", entries);

        when(syntheticFhirService.generate(any())).thenReturn(new SyntheticFhirGenerateResponse(
                "prior_auth_bundle_valid",
                "Prior authorization bundle (valid)",
                "Synthetic multi-resource prior authorization bundle for workflow demos.",
                "valid",
                "synthetic",
                new SyntheticFhirGenerateResponse.ValidationRecommendation(
                        "hl7.fhir.r4.core",
                        "4.0.1",
                        "http://hl7.org/fhir/StructureDefinition/Bundle",
                        "/v1/fhir-validation/validate"
                ),
                objectMapper.createObjectNode(),
                bundle,
                List.of("synthetic only")
        ));

        when(fhirValidationService.validate(any())).thenReturn(new FhirValidationResponse(
                "valid",
                new FhirValidationResponse.PackageSelection(
                        "hl7.fhir.r4.core",
                        "4.0.1",
                        "HL7 FHIR R4 Core",
                        "base_specification",
                        "http://hl7.org/fhir/StructureDefinition/Bundle",
                        "FHIR R4 Bundle",
                        "boundary",
                        "base_profile_validation"
                ),
                "synthetic",
                true,
                "human_review_required",
                "Structured validation response.",
                List.of(),
                List.of("https://hl7.org/fhir/R4/bundle.html")
        ));

        when(groundedAnswerService.answer(any())).thenReturn(new GroundedAnswerResponse(
                "ans_1",
                "grounded",
                Instant.parse("2026-07-27T12:00:00Z"),
                "How should we review bundle-level PAS scenarios?",
                "Grounded answer",
                List.of(new GroundedAnswerResponse.EvidenceFinding(
                        "finding_1",
                        "Prior authorization exchanges should be reviewed with workflow and standards context.",
                        new GroundedAnswerResponse.Citation(
                                "passage_1",
                                "cms-prior-auth",
                                "2026-07-24-expanded-web-core-v4",
                                "governing_regulation",
                                "CMS prior authorization material",
                                "https://example.org/cms",
                                "section-1",
                                "Supports workflow-aware review."
                        )
                )),
                List.of(),
                "Human review required."
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

        var service = new PriorAuthBundleReviewService(
                syntheticFhirService,
                fhirValidationService,
                groundedAnswerService,
                standardsArtifactService
        );

        var response = service.review(new PriorAuthBundleReviewRequest(
                "mvp-regulatory-corpus",
                "2026-07-24-expanded-web-core-v4",
                "How should we review bundle-level PAS scenarios?",
                "Synthetic provider EHR planning scenario for prior authorization APIs.",
                "prior_auth_bundle_valid"
        ));

        assertThat(response.status()).isEqualTo("grounded");
        assertThat(response.scenario().scenarioId()).isEqualTo("prior_auth_bundle_valid");
        assertThat(response.bundleInventory().entryCount()).isEqualTo(2);
        assertThat(response.bundleInventory().resourceTypes()).contains("Coverage", "Claim");
        assertThat(response.workflowContext().journeyType()).isEqualTo("PAS");
        assertThat(response.workflowContext().standardsTouchpoints()).isNotEmpty();
        assertThat(response.scenarioFindings()).extracting(PriorAuthBundleReviewResponse.ScenarioFinding::findingType)
                .contains("bundle_composition", "workflow_context");
        assertThat(response.validation().status()).isEqualTo("valid");
    }
}
