package dev.healthforge.platform.developer;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/developer")
public class DeveloperWorkflowController {

    private final DeveloperWorkflowService service;
    private final AuthenticatedActorResolver actorResolver;

    public DeveloperWorkflowController(
            DeveloperWorkflowService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/overview")
    public DeveloperOverviewResponse overview(HttpServletRequest request) {
        return service.overview(actorResolver.requireReviewerOrAdministrator(request));
    }

    @PostMapping("/repo-guidance")
    public DeveloperRepoGuidanceResponse repoGuidance(
            @Valid @RequestBody DeveloperRepoGuidanceRequest request,
            HttpServletRequest httpRequest
    ) {
        return service.repoGuidance(request, actorResolver.requireApproverOrAdministrator(httpRequest));
    }
}
