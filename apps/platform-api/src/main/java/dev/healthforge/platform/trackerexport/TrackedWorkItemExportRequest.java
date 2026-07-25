package dev.healthforge.platform.trackerexport;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record TrackedWorkItemExportRequest(
        @NotBlank String briefId,
        @NotBlank String targetSystem,
        List<String> workItemIds,
        boolean approvalAcknowledgement,
        boolean writebackRequested,
        String exportReason
) {
}
