package dev.healthforge.platform.fhir;

import java.util.List;

public record FhirValidationCatalogResponse(
        List<FhirPackage> packages
) {
    public record FhirPackage(
            String packageId,
            String packageVersion,
            String packageTitle,
            String packageKind,
            String supportStatus,
            String validationBoundary,
            String packageEvidenceLink,
            List<FhirProfile> profiles
    ) {
    }

    public record FhirProfile(
            String profileUrl,
            String profileTitle,
            String supportStatus,
            String validationScope,
            String profileEvidenceLink
    ) {
    }
}
