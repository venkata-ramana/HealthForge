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
    }

    @Test
    void rejectsMissingHeaders() {
        assertThatThrownBy(() -> resolver.requireWriteActor(new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Authenticated write actions require");
    }
}
