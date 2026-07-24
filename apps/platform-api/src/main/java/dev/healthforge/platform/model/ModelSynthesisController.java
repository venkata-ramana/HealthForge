package dev.healthforge.platform.model;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/model-synthesis")
public class ModelSynthesisController {
    private final ModelSynthesisService service;
    public ModelSynthesisController(ModelSynthesisService service) { this.service = service; }
    @PostMapping public ModelSynthesisResponse synthesize(@Valid @RequestBody ModelSynthesisRequest request) { return service.synthesize(request); }
}
