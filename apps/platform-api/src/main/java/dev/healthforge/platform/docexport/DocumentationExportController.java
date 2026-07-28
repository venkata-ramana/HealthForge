package dev.healthforge.platform.docexport;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/documentation-exports")
public class DocumentationExportController {

    private final DocumentationExportService service;
    private final AuthenticatedActorResolver actorResolver;

    public DocumentationExportController(
            DocumentationExportService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    public DocumentationExportResponse export(
            @Valid @RequestBody DocumentationExportRequest request,
            HttpServletRequest httpRequest
    ) {
        return service.export(request, actorResolver.requireApproverOrAdministrator(httpRequest));
    }
}
