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

    @PostMapping("/question-packs")
    public WorkspaceOverviewResponse.QuestionPackSummary createQuestionPack(
            @Valid @RequestBody WorkspaceQuestionPackRequest questionPackRequest,
            HttpServletRequest request
    ) {
        return service.createQuestionPack(questionPackRequest, actorResolver.requireReviewerOrAdministrator(request));
    }

    @PostMapping("/research-notebooks")
    public WorkspaceOverviewResponse.ResearchNotebookSummary createResearchNotebook(
            @Valid @RequestBody WorkspaceResearchNotebookRequest researchNotebookRequest,
            HttpServletRequest request
    ) {
        return service.createResearchNotebook(researchNotebookRequest, actorResolver.requireReviewerOrAdministrator(request));
    }

    @PostMapping("/review-escalations")
    public WorkspaceOverviewResponse.EscalationSummary createReviewEscalation(
            @Valid @RequestBody WorkspaceReviewEscalationRequest reviewEscalationRequest,
            HttpServletRequest request
    ) {
        return service.createReviewEscalation(reviewEscalationRequest, actorResolver.requireReviewerOrAdministrator(request));
    }

    @PostMapping("/discovery/search")
    public WorkspaceSearchResponse search(
            @Valid @RequestBody WorkspaceSearchRequest searchRequest,
            HttpServletRequest request
    ) {
        return service.search(searchRequest, actorResolver.requireReviewerOrAdministrator(request));
    }
}
