package dev.healthforge.platform.crosswalk;

import dev.healthforge.platform.answer.GroundedAnswerRequest;
import dev.healthforge.platform.answer.GroundedAnswerResponse;
import dev.healthforge.platform.answer.GroundedAnswerService;
import dev.healthforge.platform.standards.StandardsArtifactResponse;
import dev.healthforge.platform.standards.StandardsArtifactService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PriorAuthStandardsCrosswalkService {

    private final GroundedAnswerService groundedAnswerService;
    private final StandardsArtifactService standardsArtifactService;
    private final Clock clock = Clock.systemUTC();

    public PriorAuthStandardsCrosswalkService(
            GroundedAnswerService groundedAnswerService,
            StandardsArtifactService standardsArtifactService
    ) {
        this.groundedAnswerService = groundedAnswerService;
        this.standardsArtifactService = standardsArtifactService;
    }

    public PriorAuthStandardsCrosswalkResponse build(PriorAuthStandardsCrosswalkRequest request) {
        var answer = groundedAnswerService.answer(new GroundedAnswerRequest(
                request.corpusId(),
                request.corpusVersion(),
                request.question(),
                request.projectContext(),
                List.of()
        ));

        var text = normalizedText(request);
        var journeyType = journeyType(text);
        var primaryPersona = primaryPersona(text);
        var requirementCrosswalks = requirementCrosswalks(answer, text, journeyType);

        return new PriorAuthStandardsCrosswalkResponse(
                "crosswalk_" + UUID.randomUUID(),
                answer.status(),
                Instant.now(clock),
                new PriorAuthStandardsCrosswalkResponse.Input(
                        request.corpusId(),
                        request.corpusVersion(),
                        request.question(),
                        request.projectContext(),
                        request.scenarioHint()
                ),
                journeyType,
                primaryPersona,
                summary(answer.status(), journeyType, requirementCrosswalks.size()),
                requirementCrosswalks,
                artifactSummaries(requirementCrosswalks),
                reviewerWarnings(answer.status(), journeyType),
                nextActions(answer.status(), journeyType),
                answer.findings(),
                true,
                "This standards crosswalk is a planning and reviewer-inspection artifact only. It maps cited policy statements to likely workflow stages and technical touchpoints, but human review is required before any implementation, compliance, or conformance conclusion."
        );
    }

    private String normalizedText(PriorAuthStandardsCrosswalkRequest request) {
        return (request.question() + " "
                + (request.projectContext() == null ? "" : request.projectContext()) + " "
                + (request.scenarioHint() == null ? "" : request.scenarioHint()))
                .toLowerCase(Locale.ROOT);
    }

    private String journeyType(String text) {
        if (text.contains("dtr") || text.contains("documentation") || text.contains("questionnaire")) return "DTR";
        if (text.contains("crd") || text.contains("coverage") || text.contains("discover")) return "CRD";
        return "PAS";
    }

    private String primaryPersona(String text) {
        if (text.contains("payer")) return "payer_or_utilization_management_partner";
        if (text.contains("provider") || text.contains("ehr") || text.contains("clinician")) return "provider_workflow_owner";
        return "integration_engineer";
    }

    private String summary(String answerStatus, String journeyType, int count) {
        if (!"grounded".equals(answerStatus)) {
            return "The platform identified a likely " + journeyType + " context, but the current corpus did not provide enough cited evidence for a grounded policy-to-standards crosswalk. Treat the output as a review scaffold only.";
        }
        return "The platform mapped cited policy statements into " + count + " inspectable crosswalk entr" + (count == 1 ? "y" : "ies")
                + " spanning workflow stages, FHIR resources, operations, and candidate implementation guides for " + journeyType + " planning.";
    }

    private List<PriorAuthStandardsCrosswalkResponse.RequirementCrosswalk> requirementCrosswalks(
            GroundedAnswerResponse answer,
            String text,
            String journeyType
    ) {
        var findings = answer.findings().isEmpty()
                ? List.of(new GroundedAnswerResponse.EvidenceFinding(
                "finding_scaffold",
                "Insufficient cited policy findings were available, so this crosswalk entry is a scaffold that still requires reviewer confirmation.",
                null
        ))
                : answer.findings();

        var results = new ArrayList<PriorAuthStandardsCrosswalkResponse.RequirementCrosswalk>();
        for (int i = 0; i < findings.size(); i++) {
            var finding = findings.get(i);
            var focus = policyFocus(finding.statement(), text, journeyType);
            var stage = workflowStage(focus, journeyType);
            results.add(new PriorAuthStandardsCrosswalkResponse.RequirementCrosswalk(
                    "requirement_" + (i + 1),
                    finding.statement(),
                    stage,
                    focus,
                    fhirResources(focus, journeyType),
                    operations(focus, journeyType),
                    guides(focus, journeyType),
                    artifacts(focus, journeyType),
                    technicalImplications(focus, stage, journeyType)
            ));
        }
        return results;
    }

    private String policyFocus(String statement, String text, String journeyType) {
        var normalized = (statement + " " + text).toLowerCase(Locale.ROOT);
        if (normalized.contains("questionnaire") || normalized.contains("documentation") || normalized.contains("template")) {
            return "documentation_requirements";
        }
        if (normalized.contains("coverage") || normalized.contains("discovery") || normalized.contains("eligibility")) {
            return "coverage_requirements";
        }
        if (normalized.contains("decision") || normalized.contains("response") || normalized.contains("status") || normalized.contains("follow-up")) {
            return "decision_handling";
        }
        if ("CRD".equals(journeyType)) return "coverage_requirements";
        if ("DTR".equals(journeyType)) return "documentation_requirements";
        return "submission_requirements";
    }

    private String workflowStage(String focus, String journeyType) {
        return switch (focus) {
            case "coverage_requirements" -> "CRD".equals(journeyType) ? "requirements_discovery" : "submission_preparation";
            case "documentation_requirements" -> "documentation_capture";
            case "decision_handling" -> "decision_and_follow_up";
            default -> "submission_preparation";
        };
    }

    private List<String> fhirResources(String focus, String journeyType) {
        var results = new LinkedHashSet<String>();
        switch (focus) {
            case "coverage_requirements" -> {
                results.add("Coverage");
                results.add("Organization");
                if ("CRD".equals(journeyType)) results.add("ServiceRequest");
            }
            case "documentation_requirements" -> {
                results.add("Questionnaire");
                results.add("QuestionnaireResponse");
                results.add("DocumentReference");
            }
            case "decision_handling" -> {
                results.add("ClaimResponse");
                results.add("Bundle");
            }
            default -> {
                results.add("Claim");
                results.add("Coverage");
                results.add("Bundle");
            }
        }
        return results.stream().toList();
    }

    private List<String> operations(String focus, String journeyType) {
        var results = new LinkedHashSet<String>();
        switch (focus) {
            case "coverage_requirements" -> {
                if ("CRD".equals(journeyType)) results.add("coverage requirements discovery workflow");
                results.add("requirements review");
            }
            case "documentation_requirements" -> {
                results.add("documentation workflow");
                results.add("questionnaire capture");
            }
            case "decision_handling" -> {
                results.add("status normalization");
                results.add("response review");
            }
            default -> {
                results.add("$submit");
                results.add("request packaging");
            }
        }
        return results.stream().toList();
    }

    private List<String> guides(String focus, String journeyType) {
        var results = new LinkedHashSet<String>();
        results.add("HL7 FHIR R4 Core");
        switch (focus) {
            case "coverage_requirements" -> results.add("Da Vinci CRD");
            case "documentation_requirements" -> results.add("Da Vinci DTR");
            case "decision_handling", "submission_requirements" -> results.add("Da Vinci PAS");
            default -> {
                if ("CRD".equals(journeyType)) results.add("Da Vinci CRD");
                if ("DTR".equals(journeyType)) results.add("Da Vinci DTR");
                if ("PAS".equals(journeyType)) results.add("Da Vinci PAS");
            }
        }
        return results.stream().toList();
    }

    private List<PriorAuthStandardsCrosswalkResponse.ArtifactLink> artifacts(String focus, String journeyType) {
        var keywords = switch (focus) {
            case "coverage_requirements" -> List.of("crd", "coverage");
            case "documentation_requirements" -> List.of("dtr", "questionnaire");
            case "decision_handling" -> List.of("claimresponse", "pas");
            default -> List.of("claim", "pas");
        };
        var results = new ArrayList<PriorAuthStandardsCrosswalkResponse.ArtifactLink>();
        var seen = new LinkedHashSet<String>();
        for (var keyword : keywords) {
            addArtifacts(results, seen, standardsArtifactService.list(null, keyword), reason(focus, keyword));
        }
        if (results.isEmpty()) {
            var fallback = switch (journeyType) {
                case "CRD" -> "crd";
                case "DTR" -> "dtr";
                default -> "claim";
            };
            addArtifacts(results, seen, standardsArtifactService.list(null, fallback), "General fallback artifact surfaced for reviewer inspection.");
        }
        return results.stream().limit(6).toList();
    }

    private String reason(String focus, String keyword) {
        return switch (focus) {
            case "coverage_requirements" -> "Candidate CRD-oriented landing point for coverage and documentation requirement discovery.";
            case "documentation_requirements" -> "Candidate DTR-oriented landing point for documentation and questionnaire workflow mapping.";
            case "decision_handling" -> "Candidate PAS-oriented landing point for response, status, and follow-up handling.";
            default -> keyword.contains("pas")
                    ? "Candidate PAS-oriented landing point for request submission planning."
                    : "Base resource touchpoint for prior-authorization request assembly.";
        };
    }

    private void addArtifacts(
            List<PriorAuthStandardsCrosswalkResponse.ArtifactLink> results,
            LinkedHashSet<String> seen,
            StandardsArtifactResponse response,
            String reason
    ) {
        if (response == null || response.artifacts() == null) {
            return;
        }
        response.artifacts().forEach(artifact -> {
            if (seen.add(artifact.artifactId())) {
                results.add(new PriorAuthStandardsCrosswalkResponse.ArtifactLink(
                        artifact.artifactId(),
                        artifact.title(),
                        artifact.artifactType(),
                        artifact.canonicalUrl(),
                        reason
                ));
            }
        });
    }

    private List<String> technicalImplications(String focus, String stage, String journeyType) {
        var results = new ArrayList<String>();
        results.add("Treat the workflow stage '" + stage + "' as the first technical landing zone for this requirement.");
        switch (focus) {
            case "coverage_requirements" ->
                    results.add("Keep coverage-rule discovery separate from final submission so deployment-specific payer behavior remains reviewable.");
            case "documentation_requirements" ->
                    results.add("Preserve questionnaire/documentation provenance and allow reviewer checkpoints before treating responses as submission-ready.");
            case "decision_handling" ->
                    results.add("Design explicit status normalization and follow-up ownership instead of assuming a single synchronous outcome.");
            default ->
                    results.add("Separate request assembly, packaging, and submission boundaries so PAS applicability can be reviewed independently.");
        }
        results.add("Do not treat " + journeyType + "-oriented touchpoints as proof of counterparty support without human review.");
        return results;
    }

    private List<PriorAuthStandardsCrosswalkResponse.ArtifactSummary> artifactSummaries(
            List<PriorAuthStandardsCrosswalkResponse.RequirementCrosswalk> requirementCrosswalks
    ) {
        var index = new LinkedHashMap<String, PriorAuthStandardsCrosswalkResponse.ArtifactSummary>();
        for (var crosswalk : requirementCrosswalks) {
            for (var artifact : crosswalk.artifacts()) {
                index.compute(artifact.artifactId(), (artifactId, existing) -> {
                    var usedFor = new LinkedHashSet<String>();
                    if (existing != null) {
                        usedFor.addAll(existing.usedFor());
                    }
                    usedFor.add(crosswalk.policyFocus() + " -> " + crosswalk.workflowStage());
                    return new PriorAuthStandardsCrosswalkResponse.ArtifactSummary(
                            artifact.artifactId(),
                            artifact.title(),
                            artifact.artifactType(),
                            artifact.canonicalUrl(),
                            usedFor.stream().toList()
                    );
                });
            }
        }
        return new ArrayList<>(index.values());
    }

    private List<String> reviewerWarnings(String answerStatus, String journeyType) {
        var warnings = new ArrayList<String>();
        if (!"grounded".equals(answerStatus)) {
            warnings.add("The current corpus did not provide enough cited evidence for a grounded crosswalk. Reviewers must confirm the policy landing zones manually.");
        }
        warnings.add("This crosswalk explains likely technical landing zones for " + journeyType + " planning, not definitive implementation requirements.");
        warnings.add("Guide and artifact presence does not prove PAS, CRD, DTR, or payer-specific deployment support.");
        warnings.add("Human review remains required before using the crosswalk as a compliance, architecture, or build decision artifact.");
        return warnings;
    }

    private List<String> nextActions(String answerStatus, String journeyType) {
        var actions = new ArrayList<String>();
        actions.add("Review the crosswalk with domain, product, and integration stakeholders.");
        actions.add("Compare each policy focus with the explicit workflow stages already modeled in the prior-auth journey endpoint.");
        if ("grounded".equals(answerStatus)) {
            actions.add("Reuse the crosswalk in bundle review, architecture review, and approved work-item planning.");
            if ("PAS".equals(journeyType)) {
                actions.add("Prioritize request-submission and response-handling mappings for PAS-oriented engineering work.");
            }
        } else {
            actions.add("Refine the policy question or expand the corpus before relying on this crosswalk for planning.");
        }
        return actions;
    }
}
