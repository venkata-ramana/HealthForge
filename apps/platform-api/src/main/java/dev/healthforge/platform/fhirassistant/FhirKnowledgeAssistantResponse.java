package dev.healthforge.platform.fhirassistant;

import java.time.Instant;
import java.util.List;

public record FhirKnowledgeAssistantResponse(
        String assistantId,
        String status,
        Instant createdAt,
        String query,
        String summary,
        List<ArtifactInsight> artifactMatches,
        List<PackageInsight> packageMatches,
        List<String> assumptions,
        List<String> unsupportedRequests,
        String reviewNotice
) {
    public record ArtifactInsight(
            String artifactId,
            String title,
            String artifactType,
            String canonicalUrl,
            String version,
            String packageId,
            String sourceId,
            String sourceVersion,
            String supportStatus,
            String explanation,
            String supportBoundary,
            List<String> evidenceLinks,
            List<String> matchedKeywords
    ) {
    }

    public record PackageInsight(
            String packageId,
            String packageVersion,
            String packageTitle,
            String packageKind,
            String supportStatus,
            String validationBoundary,
            String packageEvidenceLink,
            List<ProfileInsight> profiles
    ) {
    }

    public record ProfileInsight(
            String profileUrl,
            String profileTitle,
            String supportStatus,
            String validationScope,
            String profileEvidenceLink
    ) {
    }
}
