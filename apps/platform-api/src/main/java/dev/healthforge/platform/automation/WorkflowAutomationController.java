package dev.healthforge.platform.automation;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/automation")
public class WorkflowAutomationController {

    private final WorkflowAutomationService service;
    private final AuthenticatedActorResolver actorResolver;

    public WorkflowAutomationController(
            WorkflowAutomationService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @PostMapping("/webhook-subscriptions")
    public WorkflowAutomationSubscriptionResponse configureSubscription(
            @Valid @RequestBody WorkflowAutomationSubscriptionRequest request,
            HttpServletRequest httpRequest
    ) {
        return service.configureSubscription(request, actorResolver.requireAdministrator(httpRequest));
    }

    @PostMapping("/events")
    public WorkflowAutomationDispatchResponse dispatchEvent(
            @Valid @RequestBody WorkflowAutomationDispatchRequest request,
            HttpServletRequest httpRequest
    ) {
        return service.dispatch(request, actorResolver.requireAdministrator(httpRequest));
    }

    @GetMapping("/status")
    public WorkflowAutomationStatusResponse status(HttpServletRequest httpRequest) {
        return service.status(actorResolver.requireAdministrator(httpRequest));
    }
}
