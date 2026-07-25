package dev.healthforge.platform.standards;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/standards-artifacts")
public class StandardsArtifactController {

    private final StandardsArtifactService service;

    public StandardsArtifactController(StandardsArtifactService service) {
        this.service = service;
    }

    @GetMapping
    public StandardsArtifactResponse list(
            @RequestParam(name = "canonical_url", required = false) String canonicalUrl,
            @RequestParam(name = "artifact_name", required = false) String artifactName
    ) {
        return service.list(canonicalUrl, artifactName);
    }
}
