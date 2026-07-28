package dev.healthforge.platform.integration;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/integrations")
public class IntegrationOperationsController {

    private final IntegrationOperationsService service;
    private final AuthenticatedActorResolver actorResolver;

    public IntegrationOperationsController(IntegrationOperationsService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/status")
    public IntegrationStatusResponse status(HttpServletRequest request) {
        return service.status(actorResolver.requireAdministrator(request));
    }

    @PostMapping("/recoveries")
    public IntegrationStatusResponse.RecoveryAction recover(
            @Valid @RequestBody IntegrationRecoveryRequest recoveryRequest,
            HttpServletRequest request
    ) {
        return service.recover(recoveryRequest, actorResolver.requireAdministrator(request));
    }
}
