package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/enterprise/maturity-program")
public class MaturityProgramController {
    private final MaturityProgramService service;
    private final AuthenticatedActorResolver actorResolver;

    public MaturityProgramController(MaturityProgramService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public MaturityProgramResponse program(HttpServletRequest request) {
        return service.program(actorResolver.requireAuditorOrAdministrator(request));
    }
}
