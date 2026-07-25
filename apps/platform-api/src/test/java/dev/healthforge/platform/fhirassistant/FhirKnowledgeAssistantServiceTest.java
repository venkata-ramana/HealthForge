package dev.healthforge.platform.fhirassistant;

import dev.healthforge.platform.fhir.FhirValidationCatalogProperties;
import dev.healthforge.platform.standards.StandardsArtifactProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FhirKnowledgeAssistantServiceTest {

    @Test
    void surfacesCuratedArtifactAndPackageMatches() {
        var standards = new StandardsArtifactProperties();
        var claim = new StandardsArtifactProperties.Artifact();
        claim.setArtifactId("pas-claim-profile");
        claim.setCanonicalUrl("https://hl7.org/fhir/us/davinci-pas/STU2.1/StructureDefinition-profile-claim.html");
        claim.setTitle("PAS Claim Profile");
        claim.setVersion("2.1.0");
        claim.setArtifactType("profile");
        claim.setPackageId("hl7.fhir.us.davinci-pas");
        claim.setSourceId("davinci-pas-stu-2-1");
        claim.setSourceVersion("STU2.1");
        claim.setSupportBoundary("Review before use.");
        claim.setEvidenceLinks(List.of("https://hl7.org/fhir/us/davinci-pas/STU2.1/"));
        claim.setKeywords(List.of("pas", "claim", "prior authorization"));
        standards.setArtifacts(List.of(claim));

        var catalog = new FhirValidationCatalogProperties();
        var pkg = new FhirValidationCatalogProperties.FhirPackage();
        pkg.setPackageId("hl7.fhir.us.davinci-pas");
        pkg.setPackageVersion("2.1.0");
        pkg.setPackageTitle("Da Vinci Prior Authorization Support (PAS) STU 2.1");
        pkg.setPackageKind("implementation_guide");
        pkg.setSupportStatus("planned");
        pkg.setValidationBoundary("Pinned only.");
        pkg.setPackageEvidenceLink("https://hl7.org/fhir/us/davinci-pas/STU2.1/");
        var profile = new FhirValidationCatalogProperties.FhirProfile();
        profile.setProfileUrl("http://hl7.org/fhir/us/davinci-pas/StructureDefinition/profile-claim");
        profile.setProfileTitle("PAS Claim Profile");
        profile.setSupportStatus("planned");
        profile.setValidationScope("implementation_guide_validation");
        profile.setProfileEvidenceLink("https://hl7.org/fhir/us/davinci-pas/STU2.1/StructureDefinition-profile-claim.html");
        pkg.setProfiles(List.of(profile));
        catalog.setPackages(List.of(pkg));

        var service = new FhirKnowledgeAssistantService(standards, catalog);

        var response = service.assist(new FhirKnowledgeAssistantRequest("PAS claim", null, null, 5));

        assertThat(response.status()).isEqualTo("grounded");
        assertThat(response.artifactMatches()).hasSize(1);
        assertThat(response.packageMatches()).hasSize(1);
        assertThat(response.artifactMatches().getFirst().title()).isEqualTo("PAS Claim Profile");
    }
}
