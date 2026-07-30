package dev.healthforge.platform.answer;

import dev.healthforge.platform.retrieval.RetrievalResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
                    "Do not infer a regulatory, clinical, or implementation conclusion without supporting evidence and human review.",
                    diagnosticsForInsufficientEvidence(request, 0, List.of("The selected snapshot did not return any citeable passages for this question."))
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
                                "This is the retrieved passage excerpt supporting the finding.",
                                result.source().freshnessStatus(),
                                result.source().sourceAgeDays(),
                                result.source().changeSummary()
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
                "A qualified human reviewer must assess applicability, context, and any proposed implementation action.",
                new GroundedAnswerResponse.Diagnostics(
                        "supported",
                        retrieval.results().size(),
                        List.of("The selected snapshot returned citeable passages for the current question."),
                        groundedRefinements(request),
                        contextHints(request),
                        "Create a Brief if the cited evidence is relevant enough to review and approve."
                )
        );
    }

    public GroundedAnswerResponse assembleUnsupported(
            GroundedAnswerRequest request,
            UnsupportedQuestionPolicy.UnsupportedDecision decision,
            Instant createdAt
    ) {
        return new GroundedAnswerResponse(
                "answer_" + UUID.randomUUID(),
                "insufficient_evidence",
                createdAt,
                request.question(),
                null,
                List.of(),
                List.of(decision.limitation()),
                decision.safeNextStep(),
                diagnosticsForInsufficientEvidence(request, 0, List.of(decision.limitation()))
        );
    }

    private GroundedAnswerResponse.Diagnostics diagnosticsForInsufficientEvidence(
            GroundedAnswerRequest request,
            int retrievalResultCount,
            List<String> reasons
    ) {
        return new GroundedAnswerResponse.Diagnostics(
                "insufficient_evidence",
                retrievalResultCount,
                reasons,
                insufficientEvidenceRefinements(request),
                contextHints(request),
                "Refine the question toward one workflow, actor, document, or standard touchpoint and try again."
        );
    }

    private List<String> insufficientEvidenceRefinements(GroundedAnswerRequest request) {
        var refinements = new ArrayList<String>();
        refinements.add("Name the exact workflow stage, actor, or transaction you want to inspect.");
        refinements.add("Mention the specific regulation, implementation guide, or program if you know it.");
        refinements.add("Reduce the question to one planning decision instead of a broad topic summary.");
        var lowered = request.question().toLowerCase(Locale.ROOT);
        if (!lowered.contains("prior authorization") && !lowered.contains("pas") && !lowered.contains("crd") && !lowered.contains("dtr")) {
            refinements.add("If this is a prior-authorization scenario, include PAS, CRD, DTR, payer, or provider context explicitly.");
        }
        if (request.question().split("\\s+").length > 12) {
            refinements.add("Shorten the question so retrieval focuses on one standards or policy concept.");
        }
        return refinements.stream().distinct().limit(5).toList();
    }

    private List<String> groundedRefinements(GroundedAnswerRequest request) {
        var refinements = new ArrayList<String>();
        refinements.add("If you need a narrower answer, constrain the question to one actor or one API interaction.");
        if (request.projectContext() != null && request.projectContext().length() > 120) {
            refinements.add("If the next step is a Brief, keep the context focused on the implementation scenario rather than broad program history.");
        }
        return refinements;
    }

    private List<String> contextHints(GroundedAnswerRequest request) {
        var hints = new ArrayList<String>();
        var context = request.projectContext() == null ? "" : request.projectContext().trim();
        if (context.length() < 30) {
            hints.add("Add the system, actor, or planning scenario so the evidence can be interpreted more precisely.");
        } else {
            hints.add("Keep the project context specific to the workflow, user type, and planning objective you care about.");
        }
        hints.add("State whether the question is for provider, payer, reviewer, auditor, or implementation planning use.");
        return hints;
    }
}
