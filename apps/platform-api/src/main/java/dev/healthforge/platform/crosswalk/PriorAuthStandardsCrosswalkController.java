package dev.healthforge.platform.crosswalk;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/prior-auth/standards-crosswalks")
public class PriorAuthStandardsCrosswalkController {

    private final PriorAuthStandardsCrosswalkService service;

    public PriorAuthStandardsCrosswalkController(PriorAuthStandardsCrosswalkService service) {
        this.service = service;
    }

    @PostMapping
    public PriorAuthStandardsCrosswalkResponse build(@Valid @RequestBody PriorAuthStandardsCrosswalkRequest request) {
        return service.build(request);
    }
}
