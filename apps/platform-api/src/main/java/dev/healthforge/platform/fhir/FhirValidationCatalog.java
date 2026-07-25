package dev.healthforge.platform.fhir;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
public class FhirValidationCatalog {

    private final Map<String, PinnedProfile> profiles = Map.of(
            key("hl7.fhir.r4.core", "4.0.1", "http://hl7.org/fhir/StructureDefinition/Patient"),
            new PinnedProfile("hl7.fhir.r4.core", "4.0.1", "http://hl7.org/fhir/StructureDefinition/Patient", "FHIR R4 Patient", "https://hl7.org/fhir/R4/patient.html"),
            key("hl7.fhir.r4.core", "4.0.1", "http://hl7.org/fhir/StructureDefinition/Observation"),
            new PinnedProfile("hl7.fhir.r4.core", "4.0.1", "http://hl7.org/fhir/StructureDefinition/Observation", "FHIR R4 Observation", "https://hl7.org/fhir/R4/observation.html"),
            key("hl7.fhir.r4.core", "4.0.1", "http://hl7.org/fhir/StructureDefinition/Claim"),
            new PinnedProfile("hl7.fhir.r4.core", "4.0.1", "http://hl7.org/fhir/StructureDefinition/Claim", "FHIR R4 Claim", "https://hl7.org/fhir/R4/claim.html"),
            key("hl7.fhir.r4.core", "4.0.1", "http://hl7.org/fhir/StructureDefinition/ClaimResponse"),
            new PinnedProfile("hl7.fhir.r4.core", "4.0.1", "http://hl7.org/fhir/StructureDefinition/ClaimResponse", "FHIR R4 ClaimResponse", "https://hl7.org/fhir/R4/claimresponse.html"),
            key("hl7.fhir.r4.core", "4.0.1", "http://hl7.org/fhir/StructureDefinition/Coverage"),
            new PinnedProfile("hl7.fhir.r4.core", "4.0.1", "http://hl7.org/fhir/StructureDefinition/Coverage", "FHIR R4 Coverage", "https://hl7.org/fhir/R4/coverage.html"),
            key("hl7.fhir.r4.core", "4.0.1", "http://hl7.org/fhir/StructureDefinition/Bundle"),
            new PinnedProfile("hl7.fhir.r4.core", "4.0.1", "http://hl7.org/fhir/StructureDefinition/Bundle", "FHIR R4 Bundle", "https://hl7.org/fhir/R4/bundle.html")
    );

    public PinnedProfile resolve(String packageId, String packageVersion, String profileUrl) {
        var profile = profiles.get(key(packageId, packageVersion, profileUrl));
        if (profile == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "The requested FHIR package/profile is not in the pinned validation catalog"
            );
        }
        return profile;
    }

    private static String key(String packageId, String packageVersion, String profileUrl) {
        return packageId + "|" + packageVersion + "|" + profileUrl;
    }

    public record PinnedProfile(
            String packageId,
            String packageVersion,
            String profileUrl,
            String profileTitle,
            String profileEvidenceLink
    ) {
        public String packageEvidenceLink() {
            return "https://hl7.org/fhir/R4/downloads.html";
        }
    }
}
