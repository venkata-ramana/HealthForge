package dev.healthforge.platform.brief;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(
        @NotBlank String findingId,
        @NotBlank @Pattern(regexp = "accept|reject|correct|needs_information") String decision,
        @Size(max = 255) String reviewer,
        @NotBlank @Size(max = 2000) String rationale,
        @Size(max = 4000) String correctedStatement
) {
}
