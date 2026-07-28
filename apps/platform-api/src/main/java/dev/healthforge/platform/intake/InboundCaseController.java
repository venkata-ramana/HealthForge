package dev.healthforge.platform.intake;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/intake/cases")
public class InboundCaseController {

    private final InboundCaseService service;
    private final AuthenticatedActorResolver actorResolver;

    public InboundCaseController(InboundCaseService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    public InboundCaseResponse intake(@Valid @RequestBody InboundCaseRequest request, HttpServletRequest httpRequest) {
        return service.intake(request, actorResolver.requireReviewerOrAdministrator(httpRequest));
    }

    @GetMapping
    public List<InboundCaseResponse> list(HttpServletRequest httpRequest) {
        return service.list(actorResolver.requireReviewerOrAdministrator(httpRequest));
    }
}
