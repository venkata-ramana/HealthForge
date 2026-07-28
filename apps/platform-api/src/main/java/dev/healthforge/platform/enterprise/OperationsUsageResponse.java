package dev.healthforge.platform.enterprise;

import java.time.Instant;
import java.util.List;

public record OperationsUsageResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        UsageSummary usageSummary,
        List<QuotaStatus> quotaStatuses,
        List<CostSignal> costSignals,
        List<String> operatorNotes,
        String summary
) {
    public record UsageSummary(
            int activeOrganizations,
            int briefsLast30Days,
            int answersLast30Days,
            int validationsLast30Days,
            int trackedExportsLast30Days,
            int inboundCasesLast30Days
    ) {
    }

    public record QuotaStatus(
            String metric,
            int observed,
            int softLimit,
            String window,
            String status,
            String rationale
    ) {
    }

    public record CostSignal(
            String title,
            String signal,
            String explanation
    ) {
    }
}
