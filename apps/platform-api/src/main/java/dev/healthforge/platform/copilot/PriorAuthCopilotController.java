package dev.healthforge.platform.copilot;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/prior-auth/copilot")
public class PriorAuthCopilotController {

    private final PriorAuthCopilotService service;

    public PriorAuthCopilotController(PriorAuthCopilotService service) {
        this.service = service;
    }

    @PostMapping
    public PriorAuthCopilotResponse analyze(@Valid @RequestBody PriorAuthCopilotRequest request) {
        return service.analyze(request);
    }
}
