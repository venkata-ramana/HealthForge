package dev.healthforge.platform.intelligence;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/intelligence")
public class IntelligenceController {

    private final IntelligenceService service;
    private final AuthenticatedActorResolver actorResolver;

    public IntelligenceController(IntelligenceService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/overview")
    public IntelligenceOverviewResponse overview(HttpServletRequest request) {
        return service.overview(actorResolver.requireWriteActor(request));
    }

    @PostMapping("/retrieval-feedback")
    public RetrievalFeedbackResponse feedback(
            @Valid @RequestBody RetrievalFeedbackRequest feedbackRequest,
            HttpServletRequest request
    ) {
        return service.recordFeedback(feedbackRequest, actorResolver.requireReviewerOrAdministrator(request));
    }
}
