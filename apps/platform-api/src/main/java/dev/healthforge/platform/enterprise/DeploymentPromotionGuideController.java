package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/enterprise")
public class DeploymentPromotionGuideController {

    private final DeploymentPromotionGuideService service;
    private final AuthenticatedActorResolver actorResolver;

    public DeploymentPromotionGuideController(
            DeploymentPromotionGuideService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/deployment-promotion-guide")
    public DeploymentPromotionGuideResponse guide(HttpServletRequest httpRequest) {
        return service.guide(actorResolver.requireAdministrator(httpRequest));
    }
}
