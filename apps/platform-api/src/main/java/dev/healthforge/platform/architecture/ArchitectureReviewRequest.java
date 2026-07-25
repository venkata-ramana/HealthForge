package dev.healthforge.platform.architecture;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ArchitectureReviewRequest(
        @NotBlank String corpusId,
        @NotBlank String corpusVersion,
        @NotBlank String question,
        @NotBlank String projectContext,
        List<String> sourceTypes
) {
}
