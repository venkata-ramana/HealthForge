package dev.healthforge.platform.intelligence;

import jakarta.validation.constraints.NotBlank;

public record RetrievalFeedbackRequest(
        @NotBlank String briefId,
        @NotBlank String findingId,
        @NotBlank String feedbackType,
        String sourceId,
        String note
) {
}
