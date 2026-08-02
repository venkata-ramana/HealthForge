package dev.healthforge.platform.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthenticatedActorRegistry {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public AuthenticatedActorRegistry(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordResolvedActor(AuthenticatedActor actor) {
        var now = Timestamp.from(Instant.now(clock));

        jdbcTemplate.update("""
                insert into actor_user (actor_user_id, display_name, auth_subject, identity_mode, created_at, last_seen_at)
                values (?, ?, ?, ?, ?, ?)
                on conflict (actor_user_id) do update
                set auth_subject = excluded.auth_subject,
                    identity_mode = excluded.identity_mode,
                    last_seen_at = excluded.last_seen_at
                """,
                actor.actorId(),
                displayName(actor.actorId()),
                actor.actorId(),
                actor.identityMode(),
                now,
                now
        );

        jdbcTemplate.update("""
                insert into actor_organization (organization_id, display_name, status, created_at, last_seen_at)
                values (?, ?, ?, ?, ?)
                on conflict (organization_id) do update
                set display_name = excluded.display_name,
                    last_seen_at = excluded.last_seen_at
                """,
                actor.organizationId(),
                displayName(actor.organizationId()),
                "active",
                now,
                now
        );

        jdbcTemplate.update("""
                insert into actor_organization_membership (membership_id, actor_user_id, organization_id, status, joined_at, last_seen_at)
                values (?, ?, ?, ?, ?, ?)
                on conflict (actor_user_id, organization_id) do update
                set status = excluded.status,
                    last_seen_at = excluded.last_seen_at
                """,
                "membership_" + UUID.randomUUID(),
                actor.actorId(),
                actor.organizationId(),
                "active",
                now,
                now
        );

        jdbcTemplate.update("""
                insert into actor_role_assignment (role_assignment_id, actor_user_id, organization_id, actor_role, granted_by, granted_at, last_seen_at)
                values (?, ?, ?, ?, ?, ?, ?)
                on conflict (actor_user_id, organization_id, actor_role) do update
                set granted_by = excluded.granted_by,
                    last_seen_at = excluded.last_seen_at
                """,
                "role_" + UUID.randomUUID(),
                actor.actorId(),
                actor.organizationId(),
                actor.role().name().toLowerCase(),
                actor.actorId(),
                now,
                now
        );
    }

    public boolean hasActiveMembership(AuthenticatedActor actor) {
        return jdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from actor_organization_membership m
                    join actor_role_assignment r
                      on r.actor_user_id = m.actor_user_id
                     and r.organization_id = m.organization_id
                     and lower(r.actor_role) = ?
                    where m.actor_user_id = ?
                      and m.organization_id = ?
                      and lower(m.status) = 'active'
                )
                """, Boolean.class,
                actor.role().name().toLowerCase(), actor.actorId(), actor.organizationId());
    }

    private String displayName(String value) {
        return value;
    }
}
