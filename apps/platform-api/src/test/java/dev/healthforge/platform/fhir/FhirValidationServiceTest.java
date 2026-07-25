package dev.healthforge.platform.fhir;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FhirValidationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FhirValidationService service = new FhirValidationService(new FhirValidationCatalog(), objectMapper);

    @Test
    void rejectsProfilesOutsideThePinnedCatalog() {
        assertThatThrownBy(() -> service.validate(new FhirValidationRequest(
                "example.package",
                "1.0.0",
                "http://example.org/StructureDefinition/Example",
                "synthetic",
                objectMapper.createObjectNode().put("resourceType", "Patient")
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("pinned validation catalog");
    }

    @Test
    void returnsStructuredFindingsForInvalidObservation() {
        var response = service.validate(new FhirValidationRequest(
                "hl7.fhir.r4.core",
                "4.0.1",
                "http://hl7.org/fhir/StructureDefinition/Observation",
                "synthetic",
                objectMapper.createObjectNode()
                        .put("resourceType", "Observation")
                        .set("code", objectMapper.createObjectNode()
                                .set("coding", objectMapper.createArrayNode()
                                        .add(objectMapper.createObjectNode()
                                                .put("system", "http://loinc.org")
                                                .put("code", "12345-6"))))
        ));

        assertThat(response.status()).isEqualTo("invalid");
        assertThat(response.humanReviewRequired()).isTrue();
        assertThat(response.implementationGuidanceStatus()).isEqualTo("human_review_required");
        assertThat(response.findings())
                .anySatisfy(finding -> {
                    assertThat(finding.message()).contains("Observation.status");
                    assertThat(finding.message()).contains("minimum required");
                });
        assertThat(response.evidenceLinks()).contains("https://hl7.org/fhir/R4/downloads.html");
    }
}
