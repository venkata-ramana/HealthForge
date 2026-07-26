package dev.healthforge.platform.identity;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/identity-directory")
public class IdentityDirectoryController {

    private final IdentityDirectoryService service;
    private final AuthenticatedActorResolver actorResolver;

    public IdentityDirectoryController(
            IdentityDirectoryService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public IdentityDirectoryResponse directory(
            @RequestParam(name = "organization_id", required = false) String organizationId,
            HttpServletRequest httpRequest
    ) {
        actorResolver.requireAdministrator(httpRequest);
        return service.directory(organizationId);
    }
}
