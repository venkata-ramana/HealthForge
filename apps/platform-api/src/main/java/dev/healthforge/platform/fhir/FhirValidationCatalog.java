package dev.healthforge.platform.fhir;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Component
public class FhirValidationCatalog {

    private final FhirValidationCatalogProperties properties;

    public FhirValidationCatalog(FhirValidationCatalogProperties properties) {
        this.properties = properties;
    }

    public FhirValidationCatalogResponse catalog() {
        var packages = properties.getPackages().stream()
                .sorted(Comparator.comparing(FhirValidationCatalogProperties.FhirPackage::getPackageId)
                        .thenComparing(FhirValidationCatalogProperties.FhirPackage::getPackageVersion))
                .map(pkg -> new FhirValidationCatalogResponse.FhirPackage(
                        pkg.getPackageId(),
                        pkg.getPackageVersion(),
                        pkg.getPackageTitle(),
                        pkg.getPackageKind(),
                        pkg.getSupportStatus(),
                        pkg.getValidationBoundary(),
                        pkg.getPackageEvidenceLink(),
                        pkg.getProfiles().stream()
                                .map(profile -> new FhirValidationCatalogResponse.FhirProfile(
                                        profile.getProfileUrl(),
                                        profile.getProfileTitle(),
                                        profile.getSupportStatus(),
                                        profile.getValidationScope(),
                                        profile.getProfileEvidenceLink()
                                ))
                                .toList()
                ))
                .toList();
        return new FhirValidationCatalogResponse(packages);
    }

    public PinnedProfile resolve(String packageId, String packageVersion, String profileUrl) {
        var selectedPackage = properties.getPackages().stream()
                .filter(pkg -> packageId.equals(pkg.getPackageId()) && packageVersion.equals(pkg.getPackageVersion()))
                .findFirst()
                .orElseThrow(() -> unsupported("The requested FHIR package is not in the pinned validation catalog"));

        var selectedProfile = selectedPackage.getProfiles().stream()
                .filter(profile -> profileUrl.equals(profile.getProfileUrl()))
                .findFirst()
                .orElseThrow(() -> unsupported("The requested FHIR profile is not in the pinned validation catalog"));

        if (!"supported".equalsIgnoreCase(selectedPackage.getSupportStatus())
                || !"supported".equalsIgnoreCase(selectedProfile.getSupportStatus())) {
            throw unsupported("The requested FHIR package/profile is cataloged but not yet supported for deterministic validation");
        }

        return new PinnedProfile(
                selectedPackage.getPackageId(),
                selectedPackage.getPackageVersion(),
                selectedPackage.getPackageTitle(),
                selectedPackage.getPackageKind(),
                selectedPackage.getValidationBoundary(),
                selectedPackage.getPackageEvidenceLink(),
                selectedProfile.getProfileUrl(),
                selectedProfile.getProfileTitle(),
                selectedProfile.getValidationScope(),
                selectedProfile.getProfileEvidenceLink()
        );
    }

    private ResponseStatusException unsupported(String detail) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, detail);
    }

    public record PinnedProfile(
            String packageId,
            String packageVersion,
            String packageTitle,
            String packageKind,
            String validationBoundary,
            String packageEvidenceLink,
            String profileUrl,
            String profileTitle,
            String validationScope,
            String profileEvidenceLink
    ) {
    }
}
