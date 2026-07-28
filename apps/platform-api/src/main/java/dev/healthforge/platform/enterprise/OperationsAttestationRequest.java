package dev.healthforge.platform.enterprise;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record OperationsAttestationRequest(
        @NotBlank String policyArea,
        @NotBlank String environmentName,
        @NotBlank String attestationType,
        @NotBlank String changeSummary,
        List<String> controlIds,
        @NotBlank String acknowledgment
) {
}
