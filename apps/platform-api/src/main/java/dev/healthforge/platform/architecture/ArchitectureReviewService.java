package dev.healthforge.platform.architecture;

import dev.healthforge.platform.answer.GroundedAnswerRequest;
import dev.healthforge.platform.answer.GroundedAnswerResponse;
import dev.healthforge.platform.answer.GroundedAnswerService;
import dev.healthforge.platform.standards.StandardsArtifactService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ArchitectureReviewService {

    private final GroundedAnswerService groundedAnswerService;
    private final StandardsArtifactService standardsArtifactService;
    private final Clock clock = Clock.systemUTC();

    public ArchitectureReviewService(
            GroundedAnswerService groundedAnswerService,
            StandardsArtifactService standardsArtifactService
    ) {
        this.groundedAnswerService = groundedAnswerService;
        this.standardsArtifactService = standardsArtifactService;
    }

    public ArchitectureReviewResponse review(ArchitectureReviewRequest request) {
        var answer = groundedAnswerService.answer(new GroundedAnswerRequest(
                request.corpusId(),
                request.corpusVersion(),
                request.question(),
                request.projectContext(),
                request.sourceTypes() == null ? List.of() : request.sourceTypes()
        ));

        var reviewId = "arch_" + UUID.randomUUID();
        var createdAt = Instant.now(clock);
        var text = (request.question() + " " + request.projectContext()).toLowerCase(Locale.ROOT);
        var standardsTouchpoints = standardsTouchpoints(text);

        return new ArchitectureReviewResponse(
                reviewId,
                answer.status(),
                createdAt,
                new ArchitectureReviewResponse.Input(
                        request.corpusId(),
                        request.corpusVersion(),
                        request.question(),
                        request.projectContext()
                ),
                summarize(answer, standardsTouchpoints),
                components(text),
                integrations(text),
                standardsTouchpoints,
                assumptions(text),
                risks(text),
                checkpoints(answer.status()),
                answer.findings(),
                true,
                "This architecture review is evidence-aware but non-authoritative. Human review is mandatory before implementation, provisioning, or production use."
        );
    }

    private String summarize(GroundedAnswerResponse answer, List<String> standardsTouchpoints) {
        if (!"grounded".equals(answer.status())) {
            return "The scenario was accepted, but the current corpus did not yield enough cited evidence for a grounded architecture recommendation. Review assumptions and source coverage before implementation planning.";
        }
        if (standardsTouchpoints.isEmpty()) {
            return "The scenario produced grounded evidence and a bounded architecture review artifact. No strong standards touchpoint match was detected, so treat the output as workflow guidance rather than conformance direction.";
        }
        return "The scenario produced grounded evidence and a bounded architecture review artifact that highlights likely service boundaries, integration surfaces, and review checkpoints for prior-authorization design work.";
    }

    private List<ArchitectureReviewResponse.ComponentRecommendation> components(String text) {
        var components = new ArrayList<ArchitectureReviewResponse.ComponentRecommendation>();
        components.add(new ArchitectureReviewResponse.ComponentRecommendation(
                "Brief and review workflow",
                "Capture cited findings, reviewer decisions, and approvals before architecture conclusions become implementation inputs.",
                "The existing HealthForge control boundary requires human-reviewed artifacts before downstream engineering work."
        ));
        components.add(new ArchitectureReviewResponse.ComponentRecommendation(
                "Knowledge and standards layer",
                "Keep regulatory evidence retrieval separate from standards artifact lookup and FHIR validation.",
                "HealthForge already distinguishes retrieval, standards mapping, and deterministic validation so architecture decisions remain traceable."
        ));
        if (text.contains("ehr") || text.contains("provider")) {
            components.add(new ArchitectureReviewResponse.ComponentRecommendation(
                    "Provider workflow adapter",
                    "Translate EHR or practice-management events into reviewable discovery, documentation, and request actions.",
                    "The prior-authorization workflow starts from provider-system context and should remain decoupled from payer-specific rules."
            ));
        }
        if (text.contains("pas") || text.contains("claim") || text.contains("prior authorization")) {
            components.add(new ArchitectureReviewResponse.ComponentRecommendation(
                    "Prior-authorization submission boundary",
                    "Isolate request assembly, package selection, and status handling behind a dedicated integration boundary.",
                    "Claim, ClaimResponse, PAS, and related workflow semantics vary by counterparty and version."
            ));
        }
        if (text.contains("dtr") || text.contains("questionnaire") || text.contains("documentation")) {
            components.add(new ArchitectureReviewResponse.ComponentRecommendation(
                    "Documentation workflow module",
                    "Treat structured documentation capture as a separate user workflow with draft persistence and reviewer checkpoints.",
                    "Documentation collection often changes independently from submission transport and should not be hard-coded into request handling."
            ));
        }
        return components;
    }

    private List<ArchitectureReviewResponse.IntegrationRecommendation> integrations(String text) {
        var integrations = new ArrayList<ArchitectureReviewResponse.IntegrationRecommendation>();
        integrations.add(new ArchitectureReviewResponse.IntegrationRecommendation(
                "Curated corpus retrieval",
                "Read-only evidence lookup",
                "Architecture suggestions should remain tied to cited corpus passages instead of uncited model output."
        ));
        if (text.contains("ehr") || text.contains("provider")) {
            integrations.add(new ArchitectureReviewResponse.IntegrationRecommendation(
                    "Provider EHR or practice-management system",
                    "Inbound workflow context and outbound reviewer-facing guidance",
                    "The provider system is the primary initiating surface in the current prior-authorization model."
            ));
        }
        if (text.contains("crd")) {
            integrations.add(new ArchitectureReviewResponse.IntegrationRecommendation(
                    "Coverage requirements discovery service",
                    "Discovery request and response exchange",
                    "CRD is a candidate guide and should stay optional per deployment."
            ));
        }
        if (text.contains("dtr") || text.contains("documentation")) {
            integrations.add(new ArchitectureReviewResponse.IntegrationRecommendation(
                    "Documentation templates and rules workflow",
                    "Structured documentation launch and response handling",
                    "Documentation capture should preserve user edits and provenance separately from request submission."
            ));
        }
        if (text.contains("pas") || text.contains("claim") || text.contains("prior authorization")) {
            integrations.add(new ArchitectureReviewResponse.IntegrationRecommendation(
                    "Payer or delegated utilization-management endpoint",
                    "Submission, status, and follow-up exchange",
                    "Counterparty support, authentication, and asynchronous status behavior must remain explicit review items."
            ));
        }
        return integrations;
    }

    private List<String> standardsTouchpoints(String text) {
        var matches = new LinkedHashSet<String>();
        addArtifactTitles(matches, standardsArtifactService.list(null, "claim"));
        if (text.contains("pas")) addArtifactTitles(matches, standardsArtifactService.list(null, "pas"));
        if (text.contains("crd") || text.contains("coverage")) addArtifactTitles(matches, standardsArtifactService.list(null, "crd"));
        if (text.contains("dtr") || text.contains("documentation") || text.contains("questionnaire")) addArtifactTitles(matches, standardsArtifactService.list(null, "dtr"));
        if (text.contains("claimresponse") || text.contains("status") || text.contains("decision")) addArtifactTitles(matches, standardsArtifactService.list(null, "claimresponse"));
        return matches.stream().limit(6).toList();
    }

    private void addArtifactTitles(LinkedHashSet<String> matches, dev.healthforge.platform.standards.StandardsArtifactResponse response) {
        response.artifacts().forEach(artifact -> matches.add(artifact.title() + " [" + artifact.artifactType() + "]"));
    }

    private List<ArchitectureReviewResponse.Assumption> assumptions(String text) {
        var assumptions = new ArrayList<ArchitectureReviewResponse.Assumption>();
        assumptions.add(new ArchitectureReviewResponse.Assumption(
                "The scenario remains non-sensitive and does not require live patient or payer data in HealthForge.",
                "Confirm the design discussion can stay within the current no-PHI MVP boundary."
        ));
        assumptions.add(new ArchitectureReviewResponse.Assumption(
                "Human reviewers will validate guide applicability before any PAS, CRD, or DTR implementation claim is made.",
                "Select supported guide versions and counterparties explicitly for the target deployment."
        ));
        if (text.contains("async") || text.contains("status") || text.contains("decision")) {
            assumptions.add(new ArchitectureReviewResponse.Assumption(
                    "The downstream decision flow may be asynchronous.",
                    "Define retry, idempotency, notification ownership, and audit retention before implementation."
            ));
        }
        return assumptions;
    }

    private List<ArchitectureReviewResponse.Risk> risks(String text) {
        var risks = new ArrayList<ArchitectureReviewResponse.Risk>();
        risks.add(new ArchitectureReviewResponse.Risk(
                "Applicability mismatch",
                "A suggested standards path may not apply to the target payer, product, or jurisdiction.",
                "Gate implementation on explicit deployment-scope review and pinned package selection."
        ));
        risks.add(new ArchitectureReviewResponse.Risk(
                "Version mismatch",
                "FHIR, implementation-guide, authentication, or endpoint expectations can diverge across counterparties.",
                "Keep integration boundaries version-aware and validate artifacts deterministically where supported."
        ));
        if (text.contains("claim") || text.contains("pas") || text.contains("prior authorization")) {
            risks.add(new ArchitectureReviewResponse.Risk(
                    "Asynchronous request handling gap",
                    "Prior-authorization responses may require follow-up rather than a one-step synchronous outcome.",
                    "Design explicit status normalization, retry rules, and human follow-up ownership."
            ));
        }
        if (text.contains("documentation") || text.contains("dtr")) {
            risks.add(new ArchitectureReviewResponse.Risk(
                    "Documentation workflow drift",
                    "Structured documentation logic can evolve separately from submission behavior.",
                    "Separate documentation collection from transport and preserve reviewer checkpoints for edits."
            ));
        }
        return risks;
    }

    private List<ArchitectureReviewResponse.ReviewCheckpoint> checkpoints(String answerStatus) {
        var checkpoints = new ArrayList<ArchitectureReviewResponse.ReviewCheckpoint>();
        checkpoints.add(new ArchitectureReviewResponse.ReviewCheckpoint(
                "Evidence coverage review",
                "Confirm the cited corpus actually covers the proposed design boundary before implementation planning."
        ));
        checkpoints.add(new ArchitectureReviewResponse.ReviewCheckpoint(
                "Standards applicability review",
                "Validate whether the referenced FHIR or implementation-guide artifacts apply to the chosen counterparty and deployment."
        ));
        checkpoints.add(new ArchitectureReviewResponse.ReviewCheckpoint(
                "Integration and risk review",
                "Review async behavior, identity resolution, and audit retention before any system changes are approved."
        ));
        if (!"grounded".equals(answerStatus)) {
            checkpoints.add(new ArchitectureReviewResponse.ReviewCheckpoint(
                    "Corpus gap review",
                    "Expand or correct the source corpus before relying on the architecture suggestion."
            ));
        }
        return checkpoints;
    }
}
