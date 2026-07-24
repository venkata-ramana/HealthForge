package dev.healthforge.platform.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "healthforge.storage")
public record ArtifactStorageProperties(Path artifactDirectory) {
}
