package dev.healthforge.platform.model;

public record ModelSynthesisResponse(String status, String provider, String promptTemplateVersion, int citedFindingCount) {}
