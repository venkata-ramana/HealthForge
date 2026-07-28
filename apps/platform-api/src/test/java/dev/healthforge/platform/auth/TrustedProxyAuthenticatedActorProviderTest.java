package dev.healthforge.platform.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrustedProxyAuthenticatedActorProviderTest {

    private final AuthProperties properties = properties();
    private final TrustedProxyAuthenticatedActorProvider provider = new TrustedProxyAuthenticatedActorProvider(properties);

    @Test
    void resolvesActorFromTrustedHeadersAndGroupMappings() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-HealthForge-Subject", "alice@example.org");
        request.addHeader("X-HealthForge-Groups", "team-reviewers,team-approvers");
        request.addHeader("X-HealthForge-Organization", "tenant.alpha");

        var actor = provider.resolveRequiredActor(request);

        assertThat(actor.actorId()).isEqualTo("alice@example.org");
        assertThat(actor.role()).isEqualTo(ActorRole.APPROVER);
        assertThat(actor.organizationId()).isEqualTo("tenant.alpha");
        assertThat(actor.identityMode()).isEqualTo("trusted_proxy");
    }

    @Test
    void rejectsWhenGroupsDoNotMapToRoles() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-HealthForge-Subject", "alice@example.org");
        request.addHeader("X-HealthForge-Groups", "unmapped-group");

        assertThatThrownBy(() -> provider.resolveRequiredActor(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("did not include any mapped groups");
    }

    private AuthProperties properties() {
        var properties = new AuthProperties();
        properties.setMode("trusted_proxy");
        properties.setDefaultOrganizationId("local.default");
        var reviewer = new AuthProperties.GroupRoleMapping();
        reviewer.setGroup("team-reviewers");
        reviewer.setRole("reviewer");
        var approver = new AuthProperties.GroupRoleMapping();
        approver.setGroup("team-approvers");
        approver.setRole("approver");
        properties.setGroupRoleMappings(List.of(reviewer, approver));
        return properties;
    }
}
