package dev.healthforge.platform.identity;

import java.time.Instant;
import java.util.List;

public record IdentityDirectoryResponse(
        String requestedOrganizationId,
        Instant generatedAt,
        List<OrganizationSummary> organizations,
        List<UserSummary> users,
        List<MembershipSummary> memberships,
        List<RoleAssignmentSummary> roleAssignments
) {
    public record OrganizationSummary(
            String organizationId,
            String displayName,
            String status,
            Instant createdAt,
            Instant lastSeenAt
    ) {
    }

    public record UserSummary(
            String actorUserId,
            String displayName,
            String authSubject,
            String identityMode,
            Instant createdAt,
            Instant lastSeenAt
    ) {
    }

    public record MembershipSummary(
            String membershipId,
            String actorUserId,
            String organizationId,
            String status,
            Instant joinedAt,
            Instant lastSeenAt
    ) {
    }

    public record RoleAssignmentSummary(
            String roleAssignmentId,
            String actorUserId,
            String organizationId,
            String actorRole,
            String grantedBy,
            Instant grantedAt,
            Instant lastSeenAt
    ) {
    }
}
