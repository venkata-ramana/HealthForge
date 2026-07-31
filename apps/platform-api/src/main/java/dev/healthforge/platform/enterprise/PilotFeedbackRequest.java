package dev.healthforge.platform.enterprise;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PilotFeedbackRequest(
        @NotBlank String feedbackType,
        @Min(1) @Max(5) int rating,
        String briefId,
        String findingId,
        String note
) {
}
