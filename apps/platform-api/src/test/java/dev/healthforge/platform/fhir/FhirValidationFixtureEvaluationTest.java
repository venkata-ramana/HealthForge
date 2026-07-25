package dev.healthforge.platform.fhir;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FhirValidationFixtureEvaluationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final FhirValidationService service = new FhirValidationService(catalog(), objectMapper);

    @Test
    void evaluatesSyntheticPriorAuthorizationFixtures() throws Exception {
        var datasetPath = repoRoot().resolve("evals/datasets/fhir-validation/prior-authorization-scenarios.json");
        var dataset = objectMapper.readValue(Files.readString(datasetPath), ScenarioDataset.class);

        assertThat(dataset.scenarios()).isNotEmpty();

        for (var scenario : dataset.scenarios()) {
            var fixture = repoRoot().resolve(scenario.fixturePath());
            var request = objectMapper.readValue(Files.readString(fixture), FhirValidationRequest.class);
            var response = service.validate(request);

            assertThat(response.status())
                    .as("scenario %s", scenario.scenarioId())
                    .isEqualTo(scenario.expectedStatus());

            if ("invalid".equals(scenario.expectedStatus())) {
                assertThat(response.findings())
                        .as("scenario %s should produce machine-readable findings", scenario.scenarioId())
                        .isNotEmpty();

                for (var expectedSubstring : scenario.expectedFindingSubstrings()) {
                    assertThat(response.findings())
                            .extracting(FhirValidationResponse.ValidationFinding::message)
                            .anySatisfy(message -> assertThat(message).contains(expectedSubstring));
                }
            }
        }
    }

    private FhirValidationCatalog catalog() {
        var properties = new FhirValidationCatalogProperties();

        var corePackage = new FhirValidationCatalogProperties.FhirPackage();
        corePackage.setPackageId("hl7.fhir.r4.core");
        corePackage.setPackageVersion("4.0.1");
        corePackage.setPackageTitle("HL7 FHIR R4 Core");
        corePackage.setPackageKind("base_specification");
        corePackage.setSupportStatus("supported");
        corePackage.setValidationBoundary("Deterministic base-profile validation against pinned HL7 FHIR R4 core artifacts only.");
        corePackage.setPackageEvidenceLink("https://hl7.org/fhir/R4/downloads.html");

        corePackage.setProfiles(List.of(
                profile("http://hl7.org/fhir/StructureDefinition/Claim", "FHIR R4 Claim", "https://hl7.org/fhir/R4/claim.html"),
                profile("http://hl7.org/fhir/StructureDefinition/Coverage", "FHIR R4 Coverage", "https://hl7.org/fhir/R4/coverage.html"),
                profile("http://hl7.org/fhir/StructureDefinition/Bundle", "FHIR R4 Bundle", "https://hl7.org/fhir/R4/bundle.html")
        ));

        properties.setPackages(List.of(corePackage));
        return new FhirValidationCatalog(properties);
    }

    private FhirValidationCatalogProperties.FhirProfile profile(String url, String title, String evidenceLink) {
        var profile = new FhirValidationCatalogProperties.FhirProfile();
        profile.setProfileUrl(url);
        profile.setProfileTitle(title);
        profile.setSupportStatus("supported");
        profile.setValidationScope("base_profile_validation");
        profile.setProfileEvidenceLink(evidenceLink);
        return profile;
    }

    private Path repoRoot() {
        return Path.of(System.getProperty("user.dir")).resolve("../..").normalize().toAbsolutePath();
    }

    record ScenarioDataset(List<Scenario> scenarios) {}

    record Scenario(
            @JsonProperty("scenario_id") String scenarioId,
            @JsonProperty("title") String title,
            @JsonProperty("fixture_path") String fixturePath,
            @JsonProperty("expected_status") String expectedStatus,
            @JsonProperty("expected_finding_substrings") List<String> expectedFindingSubstrings,
            @JsonProperty("reviewer_note") String reviewerNote
    ) {
        public List<String> expectedFindingSubstrings() {
            return expectedFindingSubstrings == null ? List.of() : expectedFindingSubstrings;
        }
    }
}
