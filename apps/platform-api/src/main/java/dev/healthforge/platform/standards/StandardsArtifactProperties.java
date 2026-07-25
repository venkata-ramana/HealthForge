package dev.healthforge.platform.standards;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "healthforge.standards")
public class StandardsArtifactProperties {

    private List<Artifact> artifacts = new ArrayList<>();

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    public static class Artifact {
        private String artifactId;
        private String canonicalUrl;
        private String title;
        private String version;
        private String artifactType;
        private String packageId;
        private String sourceId;
        private String sourceVersion;
        private String supportBoundary;
        private List<String> evidenceLinks = new ArrayList<>();
        private List<String> keywords = new ArrayList<>();

        public String getArtifactId() { return artifactId; }
        public void setArtifactId(String artifactId) { this.artifactId = artifactId; }
        public String getCanonicalUrl() { return canonicalUrl; }
        public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getArtifactType() { return artifactType; }
        public void setArtifactType(String artifactType) { this.artifactType = artifactType; }
        public String getPackageId() { return packageId; }
        public void setPackageId(String packageId) { this.packageId = packageId; }
        public String getSourceId() { return sourceId; }
        public void setSourceId(String sourceId) { this.sourceId = sourceId; }
        public String getSourceVersion() { return sourceVersion; }
        public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
        public String getSupportBoundary() { return supportBoundary; }
        public void setSupportBoundary(String supportBoundary) { this.supportBoundary = supportBoundary; }
        public List<String> getEvidenceLinks() { return evidenceLinks; }
        public void setEvidenceLinks(List<String> evidenceLinks) { this.evidenceLinks = evidenceLinks; }
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    }
}
