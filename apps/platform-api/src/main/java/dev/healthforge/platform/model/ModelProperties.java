package dev.healthforge.platform.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "healthforge.model")
public record ModelProperties(boolean enabled, String provider, String promptTemplateVersion) {}
