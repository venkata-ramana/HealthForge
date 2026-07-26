package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/enterprise")
public class EnterprisePostureController {

    private final EnterprisePostureService service;
    private final AuthenticatedActorResolver actorResolver;

    public EnterprisePostureController(
            EnterprisePostureService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/posture")
    public EnterprisePostureResponse posture(HttpServletRequest httpRequest) {
        return service.posture(actorResolver.requireAuditorOrAdministrator(httpRequest));
    }
}
