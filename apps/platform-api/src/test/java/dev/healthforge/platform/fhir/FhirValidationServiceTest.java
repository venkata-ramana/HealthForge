package dev.healthforge.platform.fhir;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FhirValidationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FhirValidationCatalogProperties properties = new FhirValidationCatalogProperties();
    private final FhirValidationService service;

    FhirValidationServiceTest() {
        var corePackage = new FhirValidationCatalogProperties.FhirPackage();
        corePackage.setPackageId("hl7.fhir.r4.core");
        corePackage.setPackageVersion("4.0.1");
        corePackage.setPackageTitle("HL7 FHIR R4 Core");
        corePackage.setPackageKind("base_specification");
        corePackage.setSupportStatus("supported");
        corePackage.setValidationBoundary("Deterministic base-profile validation against pinned HL7 FHIR R4 core artifacts only.");
        corePackage.setPackageEvidenceLink("https://hl7.org/fhir/R4/downloads.html");

        var observation = new FhirValidationCatalogProperties.FhirProfile();
        observation.setProfileUrl("http://hl7.org/fhir/StructureDefinition/Observation");
        observation.setProfileTitle("FHIR R4 Observation");
        observation.setSupportStatus("supported");
        observation.setValidationScope("base_profile_validation");
        observation.setProfileEvidenceLink("https://hl7.org/fhir/R4/observation.html");

        var patient = new FhirValidationCatalogProperties.FhirProfile();
        patient.setProfileUrl("http://hl7.org/fhir/StructureDefinition/Patient");
        patient.setProfileTitle("FHIR R4 Patient");
        patient.setSupportStatus("supported");
        patient.setValidationScope("base_profile_validation");
        patient.setProfileEvidenceLink("https://hl7.org/fhir/R4/patient.html");

        corePackage.setProfiles(java.util.List.of(observation, patient));

        var pasPackage = new FhirValidationCatalogProperties.FhirPackage();
        pasPackage.setPackageId("hl7.fhir.us.davinci-pas");
        pasPackage.setPackageVersion("2.1.0");
        pasPackage.setPackageTitle("Da Vinci Prior Authorization Support (PAS) STU 2.1");
        pasPackage.setPackageKind("implementation_guide");
        pasPackage.setSupportStatus("planned");
        pasPackage.setValidationBoundary("Implementation-guide validation requires pinned IG package artifacts and explicit review before deterministic support is enabled.");
        pasPackage.setPackageEvidenceLink("https://hl7.org/fhir/us/davinci-pas/STU2.1/");

        var pasClaim = new FhirValidationCatalogProperties.FhirProfile();
        pasClaim.setProfileUrl("http://hl7.org/fhir/us/davinci-pas/StructureDefinition/profile-claim");
        pasClaim.setProfileTitle("PAS Claim Profile");
        pasClaim.setSupportStatus("planned");
        pasClaim.setValidationScope("implementation_guide_validation");
        pasClaim.setProfileEvidenceLink("https://hl7.org/fhir/us/davinci-pas/STU2.1/StructureDefinition-profile-claim.html");
        pasPackage.setProfiles(java.util.List.of(pasClaim));

        properties.setPackages(java.util.List.of(corePackage, pasPackage));
        service = new FhirValidationService(new FhirValidationCatalog(properties), objectMapper);
    }

    @Test
    void exposesMachineReadableCatalog() {
        var catalog = service.catalog();

        assertThat(catalog.packages()).hasSize(2);
        assertThat(catalog.packages())
                .anySatisfy(pkg -> {
                    assertThat(pkg.packageId()).isEqualTo("hl7.fhir.r4.core");
                    assertThat(pkg.supportStatus()).isEqualTo("supported");
                })
                .anySatisfy(pkg -> {
                    assertThat(pkg.packageId()).isEqualTo("hl7.fhir.us.davinci-pas");
                    assertThat(pkg.packageKind()).isEqualTo("implementation_guide");
                    assertThat(pkg.supportStatus()).isEqualTo("planned");
                });
    }

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
    void rejectsCatalogedPackagesThatAreNotYetSupported() {
        assertThatThrownBy(() -> service.validate(new FhirValidationRequest(
                "hl7.fhir.us.davinci-pas",
                "2.1.0",
                "http://hl7.org/fhir/us/davinci-pas/StructureDefinition/profile-claim",
                "synthetic",
                objectMapper.createObjectNode().put("resourceType", "Claim")
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not yet supported");
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
        assertThat(response.selectedPackage().packageKind()).isEqualTo("base_specification");
        assertThat(response.selectedPackage().validationBoundary()).contains("base-profile validation");
        assertThat(response.selectedPackage().validationScope()).isEqualTo("base_profile_validation");
        assertThat(response.findings())
                .anySatisfy(finding -> {
                    assertThat(finding.message()).contains("Observation.status");
                    assertThat(finding.message()).contains("minimum required");
                });
        assertThat(response.evidenceLinks()).contains("https://hl7.org/fhir/R4/downloads.html");
    }
}
