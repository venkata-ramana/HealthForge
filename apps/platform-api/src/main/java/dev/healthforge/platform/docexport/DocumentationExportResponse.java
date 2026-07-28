package dev.healthforge.platform.docexport;

import java.time.Instant;
import java.util.List;

public record DocumentationExportResponse(
        String documentationExportId,
        String briefId,
        String targetSystem,
        String exportMode,
        String packageFormat,
        String publishOperation,
        Instant createdAt,
        String deliveryStatus,
        String externalReference,
        List<String> traceability,
        String packageBody,
        boolean simulated,
        String receiptType,
        String reviewNotice
) {
}
