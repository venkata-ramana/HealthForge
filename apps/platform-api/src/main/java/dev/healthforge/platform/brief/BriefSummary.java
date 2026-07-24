package dev.healthforge.platform.brief;

import java.time.Instant;

public record BriefSummary(String briefId, String status, Instant createdAt, String question) {
}
