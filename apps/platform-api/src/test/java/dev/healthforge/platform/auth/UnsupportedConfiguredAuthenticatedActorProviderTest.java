package dev.healthforge.platform.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnsupportedConfiguredAuthenticatedActorProviderTest {

    @Test
    void returnsNullForOptionalResolutionWithoutAuthSignals() {
        var provider = new UnsupportedConfiguredAuthenticatedActorProvider(properties("oidc"));

        assertThat(provider.resolveOptionalActor(new MockHttpServletRequest())).isNull();
    }

    @Test
    void rejectsRequiredResolutionWhenModeIsNotImplemented() {
        var provider = new UnsupportedConfiguredAuthenticatedActorProvider(properties("oidc"));

        assertThatThrownBy(() -> provider.resolveRequiredActor(new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Configured authentication mode 'oidc' is not implemented");
    }

    @Test
    void rejectsOptionalResolutionWhenAuthSignalsExist() {
        var provider = new UnsupportedConfiguredAuthenticatedActorProvider(properties("oidc"));
        var request = new MockHttpServletRequest();
        request.addHeader(LocalHeaderAuthenticatedActorProvider.ACTOR_ID_HEADER, "future.user");

        assertThatThrownBy(() -> provider.resolveOptionalActor(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Configured authentication mode 'oidc' is not implemented");
    }

    private AuthProperties properties(String mode) {
        var properties = new AuthProperties();
        properties.setMode(mode);
        return properties;
    }
}
