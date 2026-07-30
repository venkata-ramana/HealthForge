package dev.healthforge.platform.workspace;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/workspace")
public class WorkspaceController {

    private final WorkspaceService service;
    private final AuthenticatedActorResolver actorResolver;

    public WorkspaceController(WorkspaceService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/overview")
    public WorkspaceOverviewResponse overview(HttpServletRequest request) {
        return service.overview(actorResolver.requireReviewerOrAdministrator(request));
    }

    @PostMapping("/projects")
    public WorkspaceOverviewResponse.ProjectSummary createProject(
            @Valid @RequestBody WorkspaceProjectRequest projectRequest,
            HttpServletRequest request
    ) {
        return service.createProject(projectRequest, actorResolver.requireReviewerOrAdministrator(request));
    }

    @PostMapping("/projects/{projectId}/briefs")
    public WorkspaceOverviewResponse.ProjectSummary linkBrief(
            @PathVariable String projectId,
            @Valid @RequestBody WorkspaceProjectLinkRequest linkRequest,
            HttpServletRequest request
    ) {
        return service.linkBrief(projectId, linkRequest, actorResolver.requireReviewerOrAdministrator(request));
    }

    @PostMapping("/assignments")
    public WorkspaceOverviewResponse.AssignmentSummary createAssignment(
            @Valid @RequestBody WorkspaceAssignmentRequest assignmentRequest,
            HttpServletRequest request
    ) {
        return service.createAssignment(assignmentRequest, actorResolver.requireReviewerOrAdministrator(request));
    }

    @PostMapping("/views")
    public WorkspaceOverviewResponse.SavedViewSummary createSavedView(
            @Valid @RequestBody WorkspaceSavedViewRequest savedViewRequest,
            HttpServletRequest request
    ) {
        return service.createSavedView(savedViewRequest, actorResolver.requireReviewerOrAdministrator(request));
    }

    @PostMapping("/research-packs")
    public WorkspaceOverviewResponse.ResearchPackSummary createResearchPack(
            @Valid @RequestBody WorkspaceResearchPackRequest researchPackRequest,
            HttpServletRequest request
    ) {
        return service.createResearchPack(researchPackRequest, actorResolver.requireReviewerOrAdministrator(request));
    }
}
