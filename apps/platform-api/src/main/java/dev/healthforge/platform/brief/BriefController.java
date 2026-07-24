package dev.healthforge.platform.brief;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/briefs")
public class BriefController {
    private final BriefService briefService;
    public BriefController(BriefService briefService) { this.briefService = briefService; }

    @PostMapping
    public BriefResponse create(@Valid @RequestBody BriefRequest request) { return briefService.create(request); }

    @GetMapping
    public List<BriefSummary> list() { return briefService.list(); }

    @GetMapping("/{briefId}")
    public BriefResponse get(@PathVariable String briefId) { return briefService.get(briefId); }

    @PostMapping("/{briefId}/review-decisions")
    public BriefResponse decide(@PathVariable String briefId, @Valid @RequestBody ReviewDecisionRequest request) {
        return briefService.recordDecision(briefId, request);
    }
}
