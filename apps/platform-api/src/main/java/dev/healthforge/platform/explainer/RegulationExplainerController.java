package dev.healthforge.platform.explainer;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/regulation-explainers")
public class RegulationExplainerController {

    private final RegulationExplainerService service;

    public RegulationExplainerController(RegulationExplainerService service) {
        this.service = service;
    }

    @PostMapping
    public RegulationExplainerResponse explain(@Valid @RequestBody RegulationExplainerRequest request) {
        return service.explain(request);
    }
}
