package dev.healthforge.platform.bundlereview;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/prior-auth/bundle-reviews")
public class PriorAuthBundleReviewController {

    private final PriorAuthBundleReviewService service;

    public PriorAuthBundleReviewController(PriorAuthBundleReviewService service) {
        this.service = service;
    }

    @PostMapping
    public PriorAuthBundleReviewResponse review(@Valid @RequestBody PriorAuthBundleReviewRequest request) {
        return service.review(request);
    }
}
