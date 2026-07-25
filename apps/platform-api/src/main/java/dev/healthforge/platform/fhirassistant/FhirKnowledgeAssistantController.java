package dev.healthforge.platform.fhirassistant;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/fhir-assistant")
public class FhirKnowledgeAssistantController {

    private final FhirKnowledgeAssistantService service;

    public FhirKnowledgeAssistantController(FhirKnowledgeAssistantService service) {
        this.service = service;
    }

    @PostMapping("/query")
    public FhirKnowledgeAssistantResponse assist(@Valid @RequestBody FhirKnowledgeAssistantRequest request) {
        return service.assist(request);
    }
}
