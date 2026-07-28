package dev.healthforge.platform.docexport;

import jakarta.validation.constraints.NotBlank;

public record DocumentationExportRequest(
        @NotBlank String briefId,
        @NotBlank String targetSystem,
        @NotBlank String packageFormat,
        boolean approvalAcknowledgement,
        boolean publishRequested,
        String approvalId,
        String targetLocator,
        String exportReason
) {
}
