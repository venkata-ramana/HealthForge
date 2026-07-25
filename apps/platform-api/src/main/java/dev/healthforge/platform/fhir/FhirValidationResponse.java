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
            String packageTitle,
            String packageKind,
            String profileUrl,
            String profileTitle,
            String validationBoundary,
            String validationScope
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
