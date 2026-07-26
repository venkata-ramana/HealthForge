package dev.healthforge.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(name = "healthforge.auth.mode", havingValue = "local_header", matchIfMissing = true)
public class LocalHeaderAuthenticatedActorProvider implements AuthenticatedActorProvider {

    public static final String ACTOR_ID_HEADER = "X-HealthForge-Actor";
    public static final String ACTOR_ROLE_HEADER = "X-HealthForge-Role";
    public static final String ACTOR_ORG_HEADER = "X-HealthForge-Organization";
    public static final String ACTOR_IDENTITY_MODE_HEADER = "X-HealthForge-Identity-Mode";

    private final AuthProperties properties;

    public LocalHeaderAuthenticatedActorProvider(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public AuthenticatedActor resolveRequiredActor(HttpServletRequest request) {
        var actorId = header(request, ACTOR_ID_HEADER);
        var roleHeader = header(request, ACTOR_ROLE_HEADER);
        var role = parseRole(roleHeader);
        var organizationId = optionalHeader(request, ACTOR_ORG_HEADER);
        var identityMode = optionalHeader(request, ACTOR_IDENTITY_MODE_HEADER);
        return new AuthenticatedActor(
                actorId,
                role,
                organizationId == null ? properties.getDefaultOrganizationId() : organizationId,
                identityMode == null ? authenticationMode() : identityMode
        );
    }

    @Override
    public AuthenticatedActor resolveOptionalActor(HttpServletRequest request) {
        var actorId = optionalHeader(request, ACTOR_ID_HEADER);
        var roleHeader = optionalHeader(request, ACTOR_ROLE_HEADER);
        if (actorId == null && roleHeader == null) {
            return null;
        }
        if (actorId == null || roleHeader == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authenticated write actions require " + ACTOR_ID_HEADER + " and " + ACTOR_ROLE_HEADER + " headers.");
        }
        return resolveRequiredActor(request);
    }

    @Override
    public String authenticationMode() {
        return "local_header";
    }

    private ActorRole parseRole(String roleHeader) {
        try {
            return ActorRole.parse(roleHeader);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Unsupported actor role. Allowed roles are reviewer, approver, auditor, and administrator.");
        }
    }

    private String header(HttpServletRequest request, String name) {
        var value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authenticated write actions require " + ACTOR_ID_HEADER + " and " + ACTOR_ROLE_HEADER + " headers.");
        }
        return value.trim();
    }

    private String optionalHeader(HttpServletRequest request, String name) {
        var value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
