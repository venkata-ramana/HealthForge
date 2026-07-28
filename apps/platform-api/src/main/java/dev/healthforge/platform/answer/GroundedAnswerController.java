package dev.healthforge.platform.answer;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/answers")
public class GroundedAnswerController {

    private final GroundedAnswerService groundedAnswerService;
    private final AuthenticatedActorResolver actorResolver;

    public GroundedAnswerController(
            GroundedAnswerService groundedAnswerService,
            AuthenticatedActorResolver actorResolver
    ) {
        this.groundedAnswerService = groundedAnswerService;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    public GroundedAnswerResponse answer(@Valid @RequestBody GroundedAnswerRequest request, HttpServletRequest httpRequest) {
        return groundedAnswerService.answer(request, actorResolver.resolveOptionalActor(httpRequest));
    }
}
