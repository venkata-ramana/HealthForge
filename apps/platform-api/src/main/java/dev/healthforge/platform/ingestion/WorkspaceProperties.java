package dev.healthforge.platform.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "healthforge.workspace")
public record WorkspaceProperties(Path rootDirectory) {
}
