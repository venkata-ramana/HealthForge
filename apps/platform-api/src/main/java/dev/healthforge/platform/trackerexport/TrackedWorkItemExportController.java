package dev.healthforge.platform.trackerexport;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tracker-exports")
public class TrackedWorkItemExportController {

    private final TrackedWorkItemExportService service;
    private final AuthenticatedActorResolver actorResolver;

    public TrackedWorkItemExportController(
            TrackedWorkItemExportService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @PostMapping("/preview")
    public TrackedWorkItemExportResponse preview(
            @Valid @RequestBody TrackedWorkItemExportRequest request,
            HttpServletRequest httpRequest
    ) {
        return service.preview(request, actorResolver.requireApproverOrAdministrator(httpRequest));
    }
}
