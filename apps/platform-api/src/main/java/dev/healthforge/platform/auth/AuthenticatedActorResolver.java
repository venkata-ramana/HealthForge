package dev.healthforge.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticatedActorResolver {

    public static final String ACTOR_ID_HEADER = "X-HealthForge-Actor";
    public static final String ACTOR_ROLE_HEADER = "X-HealthForge-Role";

    public AuthenticatedActor requireWriteActor(HttpServletRequest request) {
        var actorId = header(request, ACTOR_ID_HEADER);
        var roleHeader = header(request, ACTOR_ROLE_HEADER);
        final ActorRole role;
        try {
            role = ActorRole.parse(roleHeader);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Unsupported actor role. Allowed roles are reviewer and administrator.");
        }
        return new AuthenticatedActor(actorId, role);
    }

    private String header(HttpServletRequest request, String name) {
        var value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authenticated write actions require " + ACTOR_ID_HEADER + " and " + ACTOR_ROLE_HEADER + " headers.");
        }
        return value.trim();
    }
}
