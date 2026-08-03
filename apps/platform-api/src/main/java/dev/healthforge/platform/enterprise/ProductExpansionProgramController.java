package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/enterprise/product-expansion")
public class ProductExpansionProgramController {

    private final ProductExpansionProgramService service;
    private final AuthenticatedActorResolver actorResolver;

    public ProductExpansionProgramController(ProductExpansionProgramService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public ProductExpansionProgramResponse program(HttpServletRequest request) {
        return service.program(actorResolver.requireAuditorOrAdministrator(request));
    }
}
