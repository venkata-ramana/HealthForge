package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/pilot/analytics")
public class PilotAnalyticsController {

    private final PilotAnalyticsService service;
    private final AuthenticatedActorResolver actorResolver;

    public PilotAnalyticsController(PilotAnalyticsService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public PilotAnalyticsResponse analytics(HttpServletRequest request) {
        return service.analytics(actorResolver.requireAuditorOrAdministrator(request));
    }

    @PostMapping("/feedback")
    public PilotAnalyticsResponse feedback(
            @Valid @RequestBody PilotFeedbackRequest feedbackRequest,
            HttpServletRequest request
    ) {
        return service.recordFeedback(actorResolver.requireReviewerOrAdministrator(request), feedbackRequest);
    }
}
