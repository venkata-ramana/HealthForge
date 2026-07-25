package dev.healthforge.platform.standards;

import java.util.List;

public record StandardsArtifactResponse(
        List<Artifact> artifacts
) {
    public record Artifact(
            String artifactId,
            String canonicalUrl,
            String title,
            String version,
            String artifactType,
            String packageId,
            String sourceId,
            String sourceVersion,
            String supportBoundary,
            List<String> evidenceLinks,
            List<String> keywords
    ) {
    }
}
