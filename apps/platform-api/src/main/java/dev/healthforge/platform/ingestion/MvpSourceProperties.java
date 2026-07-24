package dev.healthforge.platform.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "healthforge.mvp")
public record MvpSourceProperties(Map<String, SourcePolicy> sources) {

    public record SourcePolicy(String canonicalUrl, Set<String> contentTypes) {
    }
}
