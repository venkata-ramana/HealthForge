package dev.healthforge.platform.brief;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/briefs")
public class BriefController {
    private final BriefService briefService;
    private final AuthenticatedActorResolver actorResolver;
    public BriefController(BriefService briefService, AuthenticatedActorResolver actorResolver) {
        this.briefService = briefService;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    public BriefResponse create(@Valid @RequestBody BriefRequest request, HttpServletRequest httpRequest) {
        return briefService.create(request, actorResolver.requireReviewerOrAdministrator(httpRequest));
    }

    @GetMapping
    public List<BriefSummary> list(HttpServletRequest httpRequest) {
        return briefService.list(actorResolver.requireWriteActor(httpRequest));
    }

    @GetMapping("/{briefId}")
    public BriefResponse get(@PathVariable String briefId, HttpServletRequest httpRequest) {
        return briefService.get(briefId, actorResolver.requireWriteActor(httpRequest));
    }

    @PostMapping("/{briefId}/review-decisions")
    public BriefResponse decide(@PathVariable String briefId, @Valid @RequestBody ReviewDecisionRequest request, HttpServletRequest httpRequest) {
        return briefService.recordDecision(briefId, request, actorResolver.requireReviewerOrAdministrator(httpRequest));
    }

    @PostMapping("/{briefId}/approvals")
    public BriefResponse approve(@PathVariable String briefId, @Valid @RequestBody ApprovalRequest request, HttpServletRequest httpRequest) {
        return briefService.approve(briefId, request, actorResolver.requireApproverOrAdministrator(httpRequest));
    }

    @GetMapping("/{briefId}/audit-export")
    public BriefAuditExportResponse exportAudit(@PathVariable String briefId, HttpServletRequest httpRequest) {
        return briefService.exportAudit(briefId, actorResolver.requireAuditorOrAdministrator(httpRequest));
    }

    @GetMapping("/{briefId}/work-item-export")
    public BriefWorkItemExportResponse exportWorkItems(@PathVariable String briefId, HttpServletRequest httpRequest) {
        return briefService.exportWorkItems(briefId, actorResolver.requireApproverOrAdministrator(httpRequest));
    }
}
