package dev.healthforge.platform.retrieval;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/retrieval")
public class RetrievalController {

    @PostMapping("/search")
    public RetrievalResponse search(@Valid @RequestBody RetrievalRequest request) {
        return new RetrievalResponse(
                request.corpusId(),
                request.corpusVersion(),
                "local-placeholder-v1",
                List.of()
        );
    }
}
