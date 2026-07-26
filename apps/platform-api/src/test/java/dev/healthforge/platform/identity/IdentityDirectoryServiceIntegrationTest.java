package dev.healthforge.platform.identity;

import dev.healthforge.platform.auth.ActorRole;
import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.auth.AuthenticatedActorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IdentityDirectoryServiceIntegrationTest {

    @Autowired
    private IdentityDirectoryService service;

    @Autowired
    private AuthenticatedActorRegistry registry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from actor_role_assignment where actor_user_id like 'directory.%'");
        jdbcTemplate.update("delete from actor_organization_membership where actor_user_id like 'directory.%'");
        jdbcTemplate.update("delete from actor_user where actor_user_id like 'directory.%'");
        jdbcTemplate.update("delete from actor_organization where organization_id like 'tenant.directory.%'");
    }

    @Test
    void filtersDirectoryByOrganization() {
        registry.recordResolvedActor(new AuthenticatedActor("directory.reviewer", ActorRole.REVIEWER, "tenant.directory.alpha", "local_header"));
        registry.recordResolvedActor(new AuthenticatedActor("directory.approver", ActorRole.APPROVER, "tenant.directory.beta", "local_header"));

        var response = service.directory("tenant.directory.alpha");

        assertThat(response.organizations()).singleElement().satisfies(org ->
                assertThat(org.organizationId()).isEqualTo("tenant.directory.alpha"));
        assertThat(response.users()).singleElement().satisfies(user ->
                assertThat(user.actorUserId()).isEqualTo("directory.reviewer"));
        assertThat(response.memberships()).singleElement().satisfies(membership ->
                assertThat(membership.organizationId()).isEqualTo("tenant.directory.alpha"));
        assertThat(response.roleAssignments()).singleElement().satisfies(role ->
                assertThat(role.actorRole()).isEqualTo("reviewer"));
    }
}
