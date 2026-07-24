package dev.healthforge.platform.answer;

import dev.healthforge.platform.retrieval.RetrievalResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class GroundedAnswerAssembler {

    public GroundedAnswerResponse assemble(
            GroundedAnswerRequest request,
            RetrievalResponse retrieval,
            Instant createdAt
    ) {
        var answerId = "answer_" + UUID.randomUUID();
        if (retrieval.results().isEmpty()) {
            return new GroundedAnswerResponse(
                    answerId,
                    "insufficient_evidence",
                    createdAt,
                    request.question(),
                    null,
                    List.of(),
                    List.of("No citeable passage was retrieved for this question from the selected local corpus."),
                    "Do not infer a regulatory, clinical, or implementation conclusion without supporting evidence and human review."
            );
        }

        var findings = retrieval.results().stream()
                .map(result -> new GroundedAnswerResponse.EvidenceFinding(
                        "find_" + result.passageId(),
                        result.excerpt(),
                        new GroundedAnswerResponse.Citation(
                                result.passageId(),
                                result.source().sourceId(),
                                result.source().sourceVersion(),
                                result.source().sourceType(),
                                result.source().title(),
                                result.source().canonicalUrl(),
                                result.source().locator(),
                                "This is the retrieved passage excerpt supporting the finding."
                        )
                ))
                .toList();

        return new GroundedAnswerResponse(
                answerId,
                "grounded",
                createdAt,
                request.question(),
                "The evidence findings below are retrieved source excerpts relevant to the question.",
                findings,
                List.of("This local MVP does not make a legal, regulatory, clinical, or compliance determination."),
                "A qualified human reviewer must assess applicability, context, and any proposed implementation action."
        );
    }
}
