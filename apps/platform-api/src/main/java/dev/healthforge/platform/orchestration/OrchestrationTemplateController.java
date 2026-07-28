package dev.healthforge.platform.orchestration;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/orchestration/templates")
public class OrchestrationTemplateController {

    private final OrchestrationTemplateService service;
    private final AuthenticatedActorResolver actorResolver;

    public OrchestrationTemplateController(OrchestrationTemplateService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public List<OrchestrationTemplateResponse> list(HttpServletRequest request) {
        return service.list(actorResolver.requireReviewerOrAdministrator(request));
    }
}
