package dev.healthforge.platform.codegen;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/codegen/starter-artifacts")
public class StarterCodeGenerationController {

    private final StarterCodeGenerationService service;
    private final AuthenticatedActorResolver actorResolver;

    public StarterCodeGenerationController(StarterCodeGenerationService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    public StarterCodeGenerationResponse generate(@Valid @RequestBody StarterCodeGenerationRequest request, HttpServletRequest httpRequest) {
        return service.generate(request, actorResolver.requireApproverOrAdministrator(httpRequest));
    }
}
