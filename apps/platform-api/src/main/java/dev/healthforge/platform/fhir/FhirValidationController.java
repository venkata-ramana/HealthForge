package dev.healthforge.platform.fhir;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/fhir-validation")
public class FhirValidationController {

    private final FhirValidationService service;

    public FhirValidationController(FhirValidationService service) {
        this.service = service;
    }

    @PostMapping("/validate")
    public FhirValidationResponse validate(@Valid @RequestBody FhirValidationRequest request) {
        return service.validate(request);
    }
}
