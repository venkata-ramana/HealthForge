package dev.healthforge.platform.ingestion;

import java.net.URI;

public record SourceArtifact(byte[] bytes, String contentType, URI finalUri) {
}
