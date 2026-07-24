package dev.healthforge.platform.retrieval;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record RetrievalRequest(
        @NotBlank String corpusId,
        @NotBlank String corpusVersion,
        @NotBlank String query,
        List<String> sourceTypes,
        @Min(1) @Max(20) Integer limit
) {
}
