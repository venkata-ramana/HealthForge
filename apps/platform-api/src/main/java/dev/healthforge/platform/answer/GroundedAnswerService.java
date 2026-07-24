package dev.healthforge.platform.answer;

import dev.healthforge.platform.retrieval.RetrievalRequest;
import dev.healthforge.platform.retrieval.RetrievalService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class GroundedAnswerService {

    private final RetrievalService retrievalService;
    private final GroundedAnswerAssembler assembler;
    private final UnsupportedQuestionPolicy unsupportedQuestionPolicy;
    private final Clock clock = Clock.systemUTC();

    public GroundedAnswerService(
            RetrievalService retrievalService,
            GroundedAnswerAssembler assembler,
            UnsupportedQuestionPolicy unsupportedQuestionPolicy
    ) {
        this.retrievalService = retrievalService;
        this.assembler = assembler;
        this.unsupportedQuestionPolicy = unsupportedQuestionPolicy;
    }

    public GroundedAnswerResponse answer(GroundedAnswerRequest request) {
        var unsupportedDecision = unsupportedQuestionPolicy.evaluate(request.question());
        if (unsupportedDecision.isPresent()) {
            return assembler.assembleUnsupported(request, unsupportedDecision.get(), Instant.now(clock));
        }
        var retrieval = retrievalService.search(new RetrievalRequest(
                request.corpusId(), request.corpusVersion(), request.question(), request.sourceTypes(), 5
        ));
        return assembler.assemble(request, retrieval, Instant.now(clock));
    }
}
