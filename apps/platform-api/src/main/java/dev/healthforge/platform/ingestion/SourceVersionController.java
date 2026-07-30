package dev.healthforge.platform.ingestion;

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
@RequestMapping("/v1/source-versions")
public class SourceVersionController {

    private final SourceVersionService sourceVersionService;
    private final AuthenticatedActorResolver actorResolver;

    public SourceVersionController(SourceVersionService sourceVersionService, AuthenticatedActorResolver actorResolver) {
        this.sourceVersionService = sourceVersionService;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/{sourceVersionId}")
    public SourceVersionResponse get(@PathVariable String sourceVersionId) {
        return sourceVersionService.get(sourceVersionId);
    }

    @GetMapping("/operations")
    public SourceOperationsOverviewResponse operations(HttpServletRequest httpRequest) {
        return sourceVersionService.operations(actorResolver.requireReviewerOrAdministrator(httpRequest));
    }

    @PostMapping("/watchlists")
    public SourceOperationsOverviewResponse.WatchlistItem createWatchlist(
            @Valid @RequestBody SourceWatchlistRequest request,
            HttpServletRequest httpRequest
    ) {
        return sourceVersionService.createWatchlist(request, actorResolver.requireReviewerOrAdministrator(httpRequest));
    }

    @PostMapping("/{sourceVersionId}/lifecycle")
    public SourceVersionResponse updateLifecycle(@PathVariable String sourceVersionId, @Valid @RequestBody SourceLifecycleUpdateRequest request, HttpServletRequest httpRequest) {
        actorResolver.requireAdministrator(httpRequest);
        return sourceVersionService.updateLifecycle(sourceVersionId, request);
    }
}
