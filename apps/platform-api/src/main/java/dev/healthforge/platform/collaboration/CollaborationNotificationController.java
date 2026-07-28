package dev.healthforge.platform.collaboration;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/collaboration")
public class CollaborationNotificationController {

    private final CollaborationNotificationService service;
    private final AuthenticatedActorResolver actorResolver;

    public CollaborationNotificationController(
            CollaborationNotificationService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @PostMapping("/notifications")
    public CollaborationNotificationResponse notify(
            @Valid @RequestBody CollaborationNotificationRequest request,
            HttpServletRequest httpRequest
    ) {
        return service.notify(request, actorResolver.requireApproverOrAdministrator(httpRequest));
    }
}
