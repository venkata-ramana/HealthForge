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
@RequestMapping("/v1/pilot")
public class PilotReadinessController {

    private final PilotReadinessService service;
    private final AuthenticatedActorResolver actorResolver;

    public PilotReadinessController(
            PilotReadinessService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/readiness")
    public PilotReadinessResponse readiness(HttpServletRequest request) {
        return service.readiness(actorResolver.requireAuditorOrAdministrator(request));
    }

    @GetMapping("/solution-packs")
    public SolutionPacksResponse solutionPacks(HttpServletRequest request) {
        return service.solutionPacks(actorResolver.requireReviewerOrAdministrator(request));
    }

    @GetMapping("/stakeholder-report")
    public StakeholderReportingResponse stakeholderReport(HttpServletRequest request) {
        return service.stakeholderReport(actorResolver.requireAuditorOrAdministrator(request));
    }

    @GetMapping("/future-roadmap")
    public FutureControlRoadmapResponse futureRoadmap(HttpServletRequest request) {
        return service.futureRoadmap(actorResolver.requireAuditorOrAdministrator(request));
    }

    @GetMapping("/success")
    public PilotSuccessResponse success(HttpServletRequest request) {
        return service.success(actorResolver.requireReviewerOrAdministrator(request));
    }

    @PostMapping("/success")
    public PilotSuccessResponse recordCheckpoint(
            @Valid @RequestBody PilotSuccessCheckpointRequest checkpointRequest,
            HttpServletRequest request
    ) {
        return service.recordCheckpoint(actorResolver.requireReviewerOrAdministrator(request), checkpointRequest);
    }
}
