package dev.healthforge.platform.web;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String detail,
        String path,
        String requestId,
        List<String> errors
) {
}
