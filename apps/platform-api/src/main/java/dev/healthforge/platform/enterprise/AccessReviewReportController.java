package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/access-review")
public class AccessReviewReportController {

    private final AccessReviewReportService service;
    private final AuthenticatedActorResolver actorResolver;

    public AccessReviewReportController(
            AccessReviewReportService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public AccessReviewReportResponse report(HttpServletRequest httpRequest) {
        return service.report(actorResolver.requireAdministrator(httpRequest));
    }
}
