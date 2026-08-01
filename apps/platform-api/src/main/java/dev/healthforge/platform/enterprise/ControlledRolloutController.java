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
@RequestMapping("/v1/enterprise/controlled-rollout")
public class ControlledRolloutController {

    private final ControlledRolloutService service;
    private final AuthenticatedActorResolver actorResolver;

    public ControlledRolloutController(ControlledRolloutService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public ControlledRolloutResponse assess(HttpServletRequest request) {
        return service.assess(actorResolver.requireAuditorOrAdministrator(request));
    }

    @PostMapping("/evidence")
    public ControlledRolloutResponse recordEvidence(
            @Valid @RequestBody ControlledRolloutEvidenceRequest evidenceRequest,
            HttpServletRequest request
    ) {
        return service.recordEvidence(actorResolver.requireAdministrator(request), evidenceRequest);
    }
}
