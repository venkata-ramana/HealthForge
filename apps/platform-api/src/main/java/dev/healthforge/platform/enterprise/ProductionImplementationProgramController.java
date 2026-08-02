package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/enterprise/production-program")
public class ProductionImplementationProgramController {

    private final ProductionImplementationProgramService service;
    private final AuthenticatedActorResolver actorResolver;

    public ProductionImplementationProgramController(
            ProductionImplementationProgramService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public ProductionImplementationProgramResponse program(HttpServletRequest request) {
        return service.program(actorResolver.requireAuditorOrAdministrator(request));
    }
}
