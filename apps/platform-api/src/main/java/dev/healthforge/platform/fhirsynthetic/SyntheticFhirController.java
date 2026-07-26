package dev.healthforge.platform.fhirsynthetic;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/fhir-synthetic")
public class SyntheticFhirController {

    private final SyntheticFhirService service;

    public SyntheticFhirController(SyntheticFhirService service) {
        this.service = service;
    }

    @GetMapping("/catalog")
    public SyntheticFhirCatalogResponse catalog() {
        return service.catalog();
    }

    @PostMapping("/generate")
    public SyntheticFhirGenerateResponse generate(@Valid @RequestBody SyntheticFhirGenerateRequest request) {
        return service.generate(request);
    }
}
