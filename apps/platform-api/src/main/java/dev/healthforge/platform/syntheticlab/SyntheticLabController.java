package dev.healthforge.platform.syntheticlab;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/synthetic-labs")
public class SyntheticLabController {

    private final SyntheticLabService service;

    public SyntheticLabController(SyntheticLabService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public SyntheticLabOverviewResponse overview() {
        return service.overview();
    }

    @PostMapping("/runs")
    public SyntheticLabRunResponse run(@Valid @RequestBody SyntheticLabRunRequest request) {
        return service.run(request);
    }

    @PostMapping("/compare")
    public SyntheticLabCompareResponse compare(@Valid @RequestBody SyntheticLabCompareRequest request) {
        return service.compare(request);
    }
}
