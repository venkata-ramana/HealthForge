package dev.healthforge.platform.answer;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/answers")
public class GroundedAnswerController {

    private final GroundedAnswerService groundedAnswerService;

    public GroundedAnswerController(GroundedAnswerService groundedAnswerService) {
        this.groundedAnswerService = groundedAnswerService;
    }

    @PostMapping
    public GroundedAnswerResponse answer(@Valid @RequestBody GroundedAnswerRequest request) {
        return groundedAnswerService.answer(request);
    }
}
