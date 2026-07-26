package dev.healthforge.platform.auth;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthenticatedActorProvider {

    AuthenticatedActor resolveRequiredActor(HttpServletRequest request);

    AuthenticatedActor resolveOptionalActor(HttpServletRequest request);

    String authenticationMode();
}
