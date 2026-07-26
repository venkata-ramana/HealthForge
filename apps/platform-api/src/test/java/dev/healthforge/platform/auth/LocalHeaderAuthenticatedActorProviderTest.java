package dev.healthforge.platform.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalHeaderAuthenticatedActorProviderTest {

    private final AuthProperties properties = properties("local_header", "local.default");
    private final LocalHeaderAuthenticatedActorProvider provider = new LocalHeaderAuthenticatedActorProvider(properties);

    @Test
    void resolvesAuthenticatedActorFromHeaders() {
        var request = new MockHttpServletRequest();
        request.addHeader(LocalHeaderAuthenticatedActorProvider.ACTOR_ID_HEADER, "local.reviewer");
        request.addHeader(LocalHeaderAuthenticatedActorProvider.ACTOR_ROLE_HEADER, "reviewer");

        var actor = provider.resolveRequiredActor(request);

        assertThat(actor.actorId()).isEqualTo("local.reviewer");
        assertThat(actor.role()).isEqualTo(ActorRole.REVIEWER);
        assertThat(actor.organizationId()).isEqualTo("local.default");
        assertThat(actor.identityMode()).isEqualTo("local_header");
    }

    @Test
    void resolvesOptionalActorWhenHeadersPresent() {
        var request = new MockHttpServletRequest();
        request.addHeader(LocalHeaderAuthenticatedActorProvider.ACTOR_ID_HEADER, "auditor.one");
        request.addHeader(LocalHeaderAuthenticatedActorProvider.ACTOR_ROLE_HEADER, "auditor");
        request.addHeader(LocalHeaderAuthenticatedActorProvider.ACTOR_ORG_HEADER, "tenant.alpha");
        request.addHeader(LocalHeaderAuthenticatedActorProvider.ACTOR_IDENTITY_MODE_HEADER, "demo_sso");

        var actor = provider.resolveOptionalActor(request);

        assertThat(actor.actorId()).isEqualTo("auditor.one");
        assertThat(actor.role()).isEqualTo(ActorRole.AUDITOR);
        assertThat(actor.organizationId()).isEqualTo("tenant.alpha");
        assertThat(actor.identityMode()).isEqualTo("demo_sso");
    }

    @Test
    void resolvesOptionalActorAsNullWhenHeadersAbsent() {
        assertThat(provider.resolveOptionalActor(new MockHttpServletRequest())).isNull();
    }

    @Test
    void rejectsMissingHeaders() {
        assertThatThrownBy(() -> provider.resolveRequiredActor(new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Authenticated write actions require");
    }

    @Test
    void usesConfiguredDefaultOrganizationId() {
        var customProperties = properties("local_header", "tenant.default");
        var customProvider = new LocalHeaderAuthenticatedActorProvider(customProperties);
        var request = new MockHttpServletRequest();
        request.addHeader(LocalHeaderAuthenticatedActorProvider.ACTOR_ID_HEADER, "local.reviewer");
        request.addHeader(LocalHeaderAuthenticatedActorProvider.ACTOR_ROLE_HEADER, "reviewer");

        var actor = customProvider.resolveRequiredActor(request);

        assertThat(actor.organizationId()).isEqualTo("tenant.default");
    }

    private AuthProperties properties(String mode, String defaultOrganizationId) {
        var properties = new AuthProperties();
        properties.setMode(mode);
        properties.setDefaultOrganizationId(defaultOrganizationId);
        return properties;
    }
}
