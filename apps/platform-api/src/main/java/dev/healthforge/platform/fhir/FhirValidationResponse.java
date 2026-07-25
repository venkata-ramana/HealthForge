package dev.healthforge.platform.fhir;

import java.util.List;

public record FhirValidationResponse(
        String status,
        PackageSelection selectedPackage,
        String dataClassification,
        boolean humanReviewRequired,
        String implementationGuidanceStatus,
        String summary,
        List<ValidationFinding> findings,
        List<String> evidenceLinks
) {
    public record PackageSelection(
            String packageId,
            String packageVersion,
            String profileUrl,
            String profileTitle
    ) {
    }

    public record ValidationFinding(
            String severity,
            String location,
            String message,
            String messageId,
            List<String> evidenceLinks
    ) {
    }
}
