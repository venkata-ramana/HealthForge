package dev.healthforge.platform.journey;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/prior-auth/journeys")
public class PriorAuthJourneyController {

    private final PriorAuthJourneyService service;

    public PriorAuthJourneyController(PriorAuthJourneyService service) {
        this.service = service;
    }

    @PostMapping
    public PriorAuthJourneyResponse build(@Valid @RequestBody PriorAuthJourneyRequest request) {
        return service.build(request);
    }
}
