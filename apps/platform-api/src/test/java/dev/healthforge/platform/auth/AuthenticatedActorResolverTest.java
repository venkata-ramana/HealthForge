package dev.healthforge.platform.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedActorResolverTest {

    private final AuthenticatedActorResolver resolver = new AuthenticatedActorResolver();

    @Test
    void resolvesAuthenticatedActorFromHeaders() {
        var request = new MockHttpServletRequest();
        request.addHeader(AuthenticatedActorResolver.ACTOR_ID_HEADER, "local.reviewer");
        request.addHeader(AuthenticatedActorResolver.ACTOR_ROLE_HEADER, "reviewer");

        var actor = resolver.requireWriteActor(request);

        assertThat(actor.actorId()).isEqualTo("local.reviewer");
        assertThat(actor.role()).isEqualTo(ActorRole.REVIEWER);
        assertThat(actor.organizationId()).isEqualTo("local.default");
        assertThat(actor.identityMode()).isEqualTo("local_header");
    }

    @Test
    void rejectsMissingHeaders() {
        assertThatThrownBy(() -> resolver.requireWriteActor(new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Authenticated write actions require");
    }

    @Test
    void rejectsAdministratorOnlyActionForReviewer() {
        var request = new MockHttpServletRequest();
        request.addHeader(AuthenticatedActorResolver.ACTOR_ID_HEADER, "local.reviewer");
        request.addHeader(AuthenticatedActorResolver.ACTOR_ROLE_HEADER, "reviewer");

        assertThatThrownBy(() -> resolver.requireAdministrator(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("administrator role");
    }

    @Test
    void resolvesOptionalActorAsNullWhenHeadersAbsent() {
        assertThat(resolver.resolveOptionalActor(new MockHttpServletRequest())).isNull();
    }

    @Test
    void resolvesOptionalActorWhenHeadersPresent() {
        var request = new MockHttpServletRequest();
        request.addHeader(AuthenticatedActorResolver.ACTOR_ID_HEADER, "auditor.one");
        request.addHeader(AuthenticatedActorResolver.ACTOR_ROLE_HEADER, "auditor");
        request.addHeader(AuthenticatedActorResolver.ACTOR_ORG_HEADER, "tenant.alpha");
        request.addHeader(AuthenticatedActorResolver.ACTOR_IDENTITY_MODE_HEADER, "demo_sso");

        var actor = resolver.resolveOptionalActor(request);

        assertThat(actor.actorId()).isEqualTo("auditor.one");
        assertThat(actor.role()).isEqualTo(ActorRole.AUDITOR);
        assertThat(actor.organizationId()).isEqualTo("tenant.alpha");
        assertThat(actor.identityMode()).isEqualTo("demo_sso");
    }
}
