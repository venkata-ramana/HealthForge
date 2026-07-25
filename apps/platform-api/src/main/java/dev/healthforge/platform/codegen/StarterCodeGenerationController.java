package dev.healthforge.platform.codegen;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/codegen/starter-artifacts")
public class StarterCodeGenerationController {

    private final StarterCodeGenerationService service;

    public StarterCodeGenerationController(StarterCodeGenerationService service) {
        this.service = service;
    }

    @PostMapping
    public StarterCodeGenerationResponse generate(@Valid @RequestBody StarterCodeGenerationRequest request) {
        return service.generate(request);
    }
}
