package dev.healthforge.platform.auth;

public record AuthenticatedActor(
        String actorId,
        ActorRole role,
        String organizationId,
        String identityMode
) {
    public AuthenticatedActor(String actorId, ActorRole role) {
        this(actorId, role, "local.default", "local_header");
    }
}
