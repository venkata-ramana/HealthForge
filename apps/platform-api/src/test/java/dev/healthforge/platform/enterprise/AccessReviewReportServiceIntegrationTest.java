package dev.healthforge.platform.enterprise;

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
class AccessReviewReportServiceIntegrationTest {

    @Autowired
    private AccessReviewReportService service;

    @Autowired
    private AuthenticatedActorRegistry registry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from actor_role_assignment where actor_user_id like 'report.%'");
        jdbcTemplate.update("delete from actor_organization_membership where actor_user_id like 'report.%'");
        jdbcTemplate.update("delete from actor_user where actor_user_id like 'report.%'");
        jdbcTemplate.update("delete from actor_organization where organization_id like 'tenant.report.%'");
    }

    @Test
    void summarizesAssignmentsAndPolicyForOrganization() {
        registry.recordResolvedActor(new AuthenticatedActor("report.admin", ActorRole.ADMINISTRATOR, "tenant.report.alpha", "local_header"));
        registry.recordResolvedActor(new AuthenticatedActor("report.reviewer", ActorRole.REVIEWER, "tenant.report.alpha", "local_header"));
        registry.recordResolvedActor(new AuthenticatedActor("report.auditor", ActorRole.AUDITOR, "tenant.report.alpha", "local_header"));

        var report = service.report(new AuthenticatedActor("report.admin", ActorRole.ADMINISTRATOR, "tenant.report.alpha", "local_header"));

        assertThat(report.organizationId()).isEqualTo("tenant.report.alpha");
        assertThat(report.accessSummary().totalUsers()).isEqualTo(3);
        assertThat(report.accessSummary().reviewerAssignments()).isEqualTo(1);
        assertThat(report.accessSummary().auditorAssignments()).isEqualTo(1);
        assertThat(report.accessSummary().administratorAssignments()).isEqualTo(1);
        assertThat(report.accessAssignments()).hasSize(3);
        assertThat(report.auditPolicy().policyVersion()).isEqualTo("private-demo-v1");
        assertThat(report.auditPolicy().approvalRequiredForExports()).isTrue();
    }
}
