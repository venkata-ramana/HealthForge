package dev.healthforge.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnMissingBean(AuthenticatedActorProvider.class)
public class UnsupportedConfiguredAuthenticatedActorProvider implements AuthenticatedActorProvider {

    private final AuthProperties properties;

    public UnsupportedConfiguredAuthenticatedActorProvider(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public AuthenticatedActor resolveRequiredActor(HttpServletRequest request) {
        throw unsupported();
    }

    @Override
    public AuthenticatedActor resolveOptionalActor(HttpServletRequest request) {
        var hasAnyAuthSignal = request.getHeader(LocalHeaderAuthenticatedActorProvider.ACTOR_ID_HEADER) != null
                || request.getHeader(LocalHeaderAuthenticatedActorProvider.ACTOR_ROLE_HEADER) != null
                || request.getHeader(LocalHeaderAuthenticatedActorProvider.ACTOR_ORG_HEADER) != null;
        if (!hasAnyAuthSignal) {
            return null;
        }
        throw unsupported();
    }

    @Override
    public String authenticationMode() {
        return properties.getMode();
    }

    private ResponseStatusException unsupported() {
        return new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "Configured authentication mode '" + properties.getMode() + "' is not implemented in this phase."
        );
    }
}
