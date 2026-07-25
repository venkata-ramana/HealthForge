package dev.healthforge.platform.brief;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalRequest(
        @NotBlank @Size(max = 2000) String rationale
) {
}
