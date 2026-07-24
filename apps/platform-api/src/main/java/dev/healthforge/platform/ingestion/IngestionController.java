package dev.healthforge.platform.ingestion;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ingestions")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestionJob request(@Valid @RequestBody IngestionRequest request) {
        return ingestionService.request(request);
    }

    @GetMapping("/{ingestionId}")
    public IngestionJob get(@PathVariable String ingestionId) {
        return ingestionService.get(ingestionId);
    }
}
