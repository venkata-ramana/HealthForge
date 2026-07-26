package dev.healthforge.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticatedActorResolver {

    public static final String ACTOR_ID_HEADER = LocalHeaderAuthenticatedActorProvider.ACTOR_ID_HEADER;
    public static final String ACTOR_ROLE_HEADER = LocalHeaderAuthenticatedActorProvider.ACTOR_ROLE_HEADER;
    public static final String ACTOR_ORG_HEADER = LocalHeaderAuthenticatedActorProvider.ACTOR_ORG_HEADER;
    public static final String ACTOR_IDENTITY_MODE_HEADER = LocalHeaderAuthenticatedActorProvider.ACTOR_IDENTITY_MODE_HEADER;

    private final AuthenticatedActorProvider actorProvider;
    private final AuthenticatedActorRegistry actorRegistry;

    public AuthenticatedActorResolver(AuthenticatedActorProvider actorProvider, AuthenticatedActorRegistry actorRegistry) {
        this.actorProvider = actorProvider;
        this.actorRegistry = actorRegistry;
    }

    public AuthenticatedActor requireWriteActor(HttpServletRequest request) {
        var actor = actorProvider.resolveRequiredActor(request);
        actorRegistry.recordResolvedActor(actor);
        return actor;
    }

    public AuthenticatedActor resolveOptionalActor(HttpServletRequest request) {
        var actor = actorProvider.resolveOptionalActor(request);
        if (actor != null) {
            actorRegistry.recordResolvedActor(actor);
        }
        return actor;
    }

    public AuthenticatedActor requireReviewerOrAdministrator(HttpServletRequest request) {
        var actor = requireWriteActor(request);
        if (actor.role() != ActorRole.REVIEWER
                && actor.role() != ActorRole.APPROVER
                && actor.role() != ActorRole.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This action requires a reviewer, approver, or administrator role.");
        }
        return actor;
    }

    public AuthenticatedActor requireAdministrator(HttpServletRequest request) {
        var actor = requireWriteActor(request);
        if (actor.role() != ActorRole.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This action requires an administrator role.");
        }
        return actor;
    }

    public AuthenticatedActor requireApproverOrAdministrator(HttpServletRequest request) {
        var actor = requireWriteActor(request);
        if (actor.role() != ActorRole.APPROVER && actor.role() != ActorRole.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This action requires an approver or administrator role.");
        }
        return actor;
    }

    public AuthenticatedActor requireAuditorOrAdministrator(HttpServletRequest request) {
        var actor = requireWriteActor(request);
        if (actor.role() != ActorRole.AUDITOR && actor.role() != ActorRole.ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This action requires an auditor or administrator role.");
        }
        return actor;
    }

}
