package dev.healthforge.platform.answer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GroundedAnswerRequest(
        @NotBlank String corpusId,
        @NotBlank String corpusVersion,
        @NotBlank @Size(max = 2000) String question,
        @Size(max = 2000) String projectContext,
        List<String> sourceTypes
) {
}
