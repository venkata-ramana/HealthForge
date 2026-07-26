package dev.healthforge.platform.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthenticatedActorRegistryIntegrationTest {

    @Autowired
    private AuthenticatedActorRegistry registry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from actor_role_assignment where actor_user_id like 'actor.%'");
        jdbcTemplate.update("delete from actor_organization_membership where actor_user_id like 'actor.%'");
        jdbcTemplate.update("delete from actor_user where actor_user_id like 'actor.%'");
        jdbcTemplate.update("delete from actor_organization where organization_id like 'tenant.%'");
    }

    @Test
    void recordsUsersOrganizationsMembershipsAndRoleAssignments() {
        registry.recordResolvedActor(new AuthenticatedActor("actor.reviewer", ActorRole.REVIEWER, "tenant.alpha", "local_header"));

        assertThat(count("select count(*) from actor_user where actor_user_id = 'actor.reviewer'")).isEqualTo(1);
        assertThat(count("select count(*) from actor_organization where organization_id = 'tenant.alpha'")).isEqualTo(1);
        assertThat(count("select count(*) from actor_organization_membership where actor_user_id = 'actor.reviewer' and organization_id = 'tenant.alpha'")).isEqualTo(1);
        assertThat(count("select count(*) from actor_role_assignment where actor_user_id = 'actor.reviewer' and organization_id = 'tenant.alpha' and actor_role = 'reviewer'")).isEqualTo(1);
    }

    @Test
    void updatesExistingMembershipAndRoleAssignmentWithoutDuplication() {
        var actor = new AuthenticatedActor("actor.admin", ActorRole.ADMINISTRATOR, "tenant.beta", "local_header");

        registry.recordResolvedActor(actor);
        registry.recordResolvedActor(actor);

        assertThat(count("select count(*) from actor_organization_membership where actor_user_id = 'actor.admin' and organization_id = 'tenant.beta'")).isEqualTo(1);
        assertThat(count("select count(*) from actor_role_assignment where actor_user_id = 'actor.admin' and organization_id = 'tenant.beta' and actor_role = 'administrator'")).isEqualTo(1);
    }

    private int count(String sql) {
        var result = jdbcTemplate.queryForObject(sql, Integer.class);
        return result == null ? 0 : result;
    }
}
