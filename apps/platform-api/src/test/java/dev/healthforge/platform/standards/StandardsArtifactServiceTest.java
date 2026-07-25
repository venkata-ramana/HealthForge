package dev.healthforge.platform.standards;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StandardsArtifactServiceTest {

    private final StandardsArtifactService service;

    StandardsArtifactServiceTest() {
        var properties = new StandardsArtifactProperties();

        var claim = new StandardsArtifactProperties.Artifact();
        claim.setArtifactId("fhir-r4-claim");
        claim.setCanonicalUrl("http://hl7.org/fhir/StructureDefinition/Claim");
        claim.setTitle("FHIR R4 Claim");
        claim.setVersion("4.0.1");
        claim.setArtifactType("resource");
        claim.setPackageId("hl7.fhir.r4.core");
        claim.setSourceId("hl7-fhir-r4");
        claim.setSourceVersion("4.0.1");
        claim.setSupportBoundary("Base FHIR reference for prior-authorization request modeling.");
        claim.setEvidenceLinks(List.of("https://hl7.org/fhir/R4/claim.html"));
        claim.setKeywords(List.of("claim", "prior authorization", "resource"));

        var pasClaim = new StandardsArtifactProperties.Artifact();
        pasClaim.setArtifactId("pas-claim-profile");
        pasClaim.setCanonicalUrl("http://hl7.org/fhir/us/davinci-pas/StructureDefinition/profile-claim");
        pasClaim.setTitle("PAS Claim Profile");
        pasClaim.setVersion("2.1.0");
        pasClaim.setArtifactType("profile");
        pasClaim.setPackageId("hl7.fhir.us.davinci-pas");
        pasClaim.setSourceId("davinci-pas-stu-2-1");
        pasClaim.setSourceVersion("STU2.1");
        pasClaim.setSupportBoundary("Candidate implementation-guide artifact for PAS review; applicability must be confirmed per deployment.");
        pasClaim.setEvidenceLinks(List.of("https://hl7.org/fhir/us/davinci-pas/STU2.1/StructureDefinition-profile-claim.html"));
        pasClaim.setKeywords(List.of("pas", "claim", "profile"));

        properties.setArtifacts(List.of(claim, pasClaim));
        service = new StandardsArtifactService(properties);
    }

    @Test
    void returnsArtifactsByCanonicalUrl() {
        var response = service.list("StructureDefinition/Claim", null);

        assertThat(response.artifacts()).extracting(StandardsArtifactResponse.Artifact::artifactId)
                .containsExactly("fhir-r4-claim");
    }

    @Test
    void returnsArtifactsByArtifactNameOrKeyword() {
        var response = service.list(null, "pas");

        assertThat(response.artifacts()).singleElement().satisfies(artifact -> {
            assertThat(artifact.artifactId()).isEqualTo("pas-claim-profile");
            assertThat(artifact.artifactType()).isEqualTo("profile");
        });
    }
}
