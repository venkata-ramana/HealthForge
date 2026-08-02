package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/enterprise/saas-readiness")
public class SaasReadinessController {

    private final SaasReadinessService service;
    private final AuthenticatedActorResolver actorResolver;

    public SaasReadinessController(SaasReadinessService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public SaasReadinessResponse readiness(HttpServletRequest request) {
        return service.readiness(actorResolver.requireAuditorOrAdministrator(request));
    }
}
