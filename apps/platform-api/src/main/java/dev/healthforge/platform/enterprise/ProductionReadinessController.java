package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/enterprise/production-readiness")
public class ProductionReadinessController {

    private final ProductionReadinessService service;
    private final AuthenticatedActorResolver actorResolver;

    public ProductionReadinessController(ProductionReadinessService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public ProductionReadinessResponse assess(HttpServletRequest request) {
        return service.assess(actorResolver.requireAuditorOrAdministrator(request));
    }
}
