package dev.healthforge.platform.codegen;

import java.time.Instant;
import java.util.List;

public record StarterCodeGenerationResponse(
        String generationId,
        String status,
        Instant createdAt,
        String artifactType,
        String fileName,
        String mediaType,
        String code,
        String disclaimer,
        Traceability traceability
) {
    public record Traceability(
            String briefId,
            String workItemId,
            List<String> relatedFindingIds,
            List<String> standardsTouchpoints,
            String humanReviewStatus,
            List<String> validationNotes
    ) {}
}
