package dev.healthforge.platform.regulatory;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/regulated-readiness")
public class RegulatedReadinessController {

    private final RegulatedReadinessService service;
    private final AuthenticatedActorResolver actorResolver;

    public RegulatedReadinessController(
            RegulatedReadinessService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public RegulatedReadinessResponse overview(HttpServletRequest request) {
        return service.overview(actorResolver.requireAuditorOrAdministrator(request));
    }
}
