package dev.healthforge.platform.compliance;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/compliance")
public class ComplianceDashboardController {

    private final ComplianceDashboardService service;
    private final AuthenticatedActorResolver actorResolver;

    public ComplianceDashboardController(
            ComplianceDashboardService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/dashboard")
    public ComplianceDashboardResponse dashboard(HttpServletRequest httpRequest) {
        return service.dashboard(actorResolver.requireAuditorOrAdministrator(httpRequest));
    }
}
