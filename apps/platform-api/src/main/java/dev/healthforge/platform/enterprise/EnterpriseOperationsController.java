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
@RequestMapping("/v1/admin/operations")
public class EnterpriseOperationsController {

    private final EnterpriseOperationsService service;
    private final AuthenticatedActorResolver actorResolver;

    public EnterpriseOperationsController(
            EnterpriseOperationsService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/configuration")
    public OperationsConfigurationResponse configuration(HttpServletRequest httpRequest) {
        return service.configuration(actorResolver.requireAdministrator(httpRequest));
    }

    @GetMapping("/observability")
    public OperationsObservabilityResponse observability(HttpServletRequest httpRequest) {
        return service.observability(actorResolver.requireAuditorOrAdministrator(httpRequest));
    }

    @GetMapping("/continuity")
    public OperationsContinuityResponse continuity(HttpServletRequest httpRequest) {
        return service.continuity(actorResolver.requireAdministrator(httpRequest));
    }

    @GetMapping("/usage")
    public OperationsUsageResponse usage(HttpServletRequest httpRequest) {
        return service.usage(actorResolver.requireAuditorOrAdministrator(httpRequest));
    }

    @GetMapping("/attestations")
    public OperationsAttestationResponse attestations(HttpServletRequest httpRequest) {
        return service.attestations(actorResolver.requireAdministrator(httpRequest));
    }

    @PostMapping("/attestations")
    public OperationsAttestationResponse recordAttestation(
            @Valid @RequestBody OperationsAttestationRequest request,
            HttpServletRequest httpRequest
    ) {
        return service.recordAttestation(actorResolver.requireAdministrator(httpRequest), request);
    }
}
