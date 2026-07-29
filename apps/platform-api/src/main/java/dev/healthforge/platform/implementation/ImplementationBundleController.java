package dev.healthforge.platform.implementation;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/implementation")
public class ImplementationBundleController {

    private final ImplementationBundleService service;
    private final AuthenticatedActorResolver actorResolver;

    public ImplementationBundleController(
            ImplementationBundleService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/briefs/{briefId}/bundle")
    public ImplementationBundleResponse bundle(@PathVariable String briefId, HttpServletRequest request) {
        return service.generate(briefId, actorResolver.requireApproverOrAdministrator(request));
    }
}
