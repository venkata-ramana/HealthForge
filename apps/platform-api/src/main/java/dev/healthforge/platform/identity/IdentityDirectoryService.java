package dev.healthforge.platform.identity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class IdentityDirectoryService {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public IdentityDirectoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public IdentityDirectoryResponse directory(String organizationId) {
        var organizations = organizations(organizationId);
        var users = users(organizationId);
        var memberships = memberships(organizationId);
        var roleAssignments = roleAssignments(organizationId);

        return new IdentityDirectoryResponse(
                organizationId,
                Instant.now(clock),
                organizations,
                users,
                memberships,
                roleAssignments
        );
    }

    private List<IdentityDirectoryResponse.OrganizationSummary> organizations(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return jdbcTemplate.query("""
                    select organization_id, display_name, status, created_at, last_seen_at
                    from actor_organization
                    order by last_seen_at desc
                    """, (rs, row) -> new IdentityDirectoryResponse.OrganizationSummary(
                    rs.getString("organization_id"),
                    rs.getString("display_name"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("last_seen_at").toInstant()
            ));
        }
        return jdbcTemplate.query("""
                select organization_id, display_name, status, created_at, last_seen_at
                from actor_organization
                where organization_id = ?
                order by last_seen_at desc
                """, (rs, row) -> new IdentityDirectoryResponse.OrganizationSummary(
                rs.getString("organization_id"),
                rs.getString("display_name"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_seen_at").toInstant()
        ), organizationId);
    }

    private List<IdentityDirectoryResponse.UserSummary> users(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return jdbcTemplate.query("""
                    select actor_user_id, display_name, auth_subject, identity_mode, created_at, last_seen_at
                    from actor_user
                    order by last_seen_at desc
                    """, (rs, row) -> new IdentityDirectoryResponse.UserSummary(
                    rs.getString("actor_user_id"),
                    rs.getString("display_name"),
                    rs.getString("auth_subject"),
                    rs.getString("identity_mode"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("last_seen_at").toInstant()
            ));
        }
        return jdbcTemplate.query("""
                select distinct u.actor_user_id, u.display_name, u.auth_subject, u.identity_mode, u.created_at, u.last_seen_at
                from actor_user u
                join actor_organization_membership m on m.actor_user_id = u.actor_user_id
                where m.organization_id = ?
                order by u.last_seen_at desc
                """, (rs, row) -> new IdentityDirectoryResponse.UserSummary(
                rs.getString("actor_user_id"),
                rs.getString("display_name"),
                rs.getString("auth_subject"),
                rs.getString("identity_mode"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_seen_at").toInstant()
        ), organizationId);
    }

    private List<IdentityDirectoryResponse.MembershipSummary> memberships(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return jdbcTemplate.query("""
                    select membership_id, actor_user_id, organization_id, status, joined_at, last_seen_at
                    from actor_organization_membership
                    order by last_seen_at desc
                    """, (rs, row) -> new IdentityDirectoryResponse.MembershipSummary(
                    rs.getString("membership_id"),
                    rs.getString("actor_user_id"),
                    rs.getString("organization_id"),
                    rs.getString("status"),
                    rs.getTimestamp("joined_at").toInstant(),
                    rs.getTimestamp("last_seen_at").toInstant()
            ));
        }
        return jdbcTemplate.query("""
                select membership_id, actor_user_id, organization_id, status, joined_at, last_seen_at
                from actor_organization_membership
                where organization_id = ?
                order by last_seen_at desc
                """, (rs, row) -> new IdentityDirectoryResponse.MembershipSummary(
                rs.getString("membership_id"),
                rs.getString("actor_user_id"),
                rs.getString("organization_id"),
                rs.getString("status"),
                rs.getTimestamp("joined_at").toInstant(),
                rs.getTimestamp("last_seen_at").toInstant()
        ), organizationId);
    }

    private List<IdentityDirectoryResponse.RoleAssignmentSummary> roleAssignments(String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return jdbcTemplate.query("""
                    select role_assignment_id, actor_user_id, organization_id, actor_role, granted_by, granted_at, last_seen_at
                    from actor_role_assignment
                    order by last_seen_at desc
                    """, (rs, row) -> new IdentityDirectoryResponse.RoleAssignmentSummary(
                    rs.getString("role_assignment_id"),
                    rs.getString("actor_user_id"),
                    rs.getString("organization_id"),
                    rs.getString("actor_role"),
                    rs.getString("granted_by"),
                    rs.getTimestamp("granted_at").toInstant(),
                    rs.getTimestamp("last_seen_at").toInstant()
            ));
        }
        return jdbcTemplate.query("""
                select role_assignment_id, actor_user_id, organization_id, actor_role, granted_by, granted_at, last_seen_at
                from actor_role_assignment
                where organization_id = ?
                order by last_seen_at desc
                """, (rs, row) -> new IdentityDirectoryResponse.RoleAssignmentSummary(
                rs.getString("role_assignment_id"),
                rs.getString("actor_user_id"),
                rs.getString("organization_id"),
                rs.getString("actor_role"),
                rs.getString("granted_by"),
                rs.getTimestamp("granted_at").toInstant(),
                rs.getTimestamp("last_seen_at").toInstant()
        ), organizationId);
    }
}
