package dev.healthforge.platform.brief;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BriefRequest(
        @NotBlank String corpusId,
        @NotBlank String corpusVersion,
        @NotBlank @Size(max = 2000) String question,
        @NotBlank @Size(max = 2000) String projectContext,
        List<String> sourceTypes
) {
}
