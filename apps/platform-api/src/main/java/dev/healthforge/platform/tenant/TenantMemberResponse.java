package dev.healthforge.platform.tenant;

import java.time.Instant;
import java.util.List;

public record TenantMemberResponse(
        String actorUserId,
        String displayName,
        String authSubject,
        String identityMode,
        String organizationId,
        String membershipStatus,
        List<String> roles,
        Instant joinedAt,
        Instant lastSeenAt
) {
}
