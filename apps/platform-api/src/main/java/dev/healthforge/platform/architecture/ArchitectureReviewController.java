package dev.healthforge.platform.architecture;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/architecture-reviews")
public class ArchitectureReviewController {

    private final ArchitectureReviewService service;

    public ArchitectureReviewController(ArchitectureReviewService service) {
        this.service = service;
    }

    @PostMapping
    public ArchitectureReviewResponse review(@Valid @RequestBody ArchitectureReviewRequest request) {
        return service.review(request);
    }
}
