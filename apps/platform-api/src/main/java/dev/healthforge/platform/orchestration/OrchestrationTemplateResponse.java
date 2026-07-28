package dev.healthforge.platform.orchestration;

import java.time.Instant;
import java.util.List;

public record OrchestrationTemplateResponse(
        String templateId,
        String organizationId,
        String name,
        String templateType,
        String summary,
        String defaultQueue,
        String defaultTargetSystem,
        String workflowPhase,
        List<String> guardrails,
        Instant updatedAt
) {
}
