package dev.healthforge.platform.standards;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class StandardsArtifactService {

    private final StandardsArtifactProperties properties;

    public StandardsArtifactService(StandardsArtifactProperties properties) {
        this.properties = properties;
    }

    public StandardsArtifactResponse list(String canonicalUrl, String artifactName) {
        var canonicalFilter = canonicalUrl == null ? null : canonicalUrl.trim().toLowerCase(Locale.ROOT);
        var nameFilter = artifactName == null ? null : artifactName.trim().toLowerCase(Locale.ROOT);

        var artifacts = properties.getArtifacts().stream()
                .filter(artifact -> canonicalFilter == null || artifact.getCanonicalUrl().toLowerCase(Locale.ROOT).contains(canonicalFilter))
                .filter(artifact -> nameFilter == null || matchesName(artifact, nameFilter))
                .sorted(Comparator.comparing(StandardsArtifactProperties.Artifact::getArtifactType)
                        .thenComparing(StandardsArtifactProperties.Artifact::getTitle))
                .map(artifact -> new StandardsArtifactResponse.Artifact(
                        artifact.getArtifactId(),
                        artifact.getCanonicalUrl(),
                        artifact.getTitle(),
                        artifact.getVersion(),
                        artifact.getArtifactType(),
                        artifact.getPackageId(),
                        artifact.getSourceId(),
                        artifact.getSourceVersion(),
                        artifact.getSupportBoundary(),
                        artifact.getEvidenceLinks(),
                        artifact.getKeywords()
                ))
                .toList();

        return new StandardsArtifactResponse(artifacts);
    }

    private boolean matchesName(StandardsArtifactProperties.Artifact artifact, String nameFilter) {
        if (artifact.getTitle().toLowerCase(Locale.ROOT).contains(nameFilter)) return true;
        if (artifact.getArtifactId().toLowerCase(Locale.ROOT).contains(nameFilter)) return true;
        return artifact.getKeywords().stream().anyMatch(keyword -> keyword.toLowerCase(Locale.ROOT).contains(nameFilter));
    }
}
