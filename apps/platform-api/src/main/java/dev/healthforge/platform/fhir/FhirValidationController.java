package dev.healthforge.platform.fhir;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/fhir-validation")
public class FhirValidationController {

    private final FhirValidationService service;
    private final AuthenticatedActorResolver actorResolver;

    public FhirValidationController(FhirValidationService service, AuthenticatedActorResolver actorResolver) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/catalog")
    public FhirValidationCatalogResponse catalog() {
        return service.catalog();
    }

    @PostMapping("/validate")
    public FhirValidationResponse validate(@Valid @RequestBody FhirValidationRequest request, HttpServletRequest httpRequest) {
        return service.validate(request, actorResolver.resolveOptionalActor(httpRequest));
    }
}
