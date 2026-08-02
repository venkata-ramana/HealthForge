package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/enterprise/ga-readiness")
public class GeneralAvailabilityReadinessController {

    private final GeneralAvailabilityReadinessService service;
    private final AuthenticatedActorResolver actorResolver;

    public GeneralAvailabilityReadinessController(
            GeneralAvailabilityReadinessService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public GeneralAvailabilityReadinessResponse readiness(HttpServletRequest request) {
        return service.readiness(actorResolver.requireAuditorOrAdministrator(request));
    }
}
