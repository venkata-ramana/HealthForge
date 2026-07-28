package dev.healthforge.platform.evaluation;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/evaluation")
public class EvaluationDashboardController {

    private final EvaluationDashboardService service;
    private final AuthenticatedActorResolver actorResolver;

    public EvaluationDashboardController(
            EvaluationDashboardService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/dashboard")
    public EvaluationDashboardResponse dashboard(HttpServletRequest httpRequest) {
        return service.dashboard(actorResolver.requireAuditorOrAdministrator(httpRequest));
    }

    @GetMapping("/policy-safety-report")
    public PolicySafetyReportResponse policySafetyReport(HttpServletRequest httpRequest) {
        return service.policySafetyReport(actorResolver.requireAuditorOrAdministrator(httpRequest));
    }
}
