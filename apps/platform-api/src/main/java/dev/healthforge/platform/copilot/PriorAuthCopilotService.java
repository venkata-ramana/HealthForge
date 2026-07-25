package dev.healthforge.platform.copilot;

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
public class PriorAuthCopilotService {

    private final GroundedAnswerService groundedAnswerService;
    private final StandardsArtifactService standardsArtifactService;
    private final Clock clock = Clock.systemUTC();

    public PriorAuthCopilotService(
            GroundedAnswerService groundedAnswerService,
            StandardsArtifactService standardsArtifactService
    ) {
        this.groundedAnswerService = groundedAnswerService;
        this.standardsArtifactService = standardsArtifactService;
    }

    public PriorAuthCopilotResponse analyze(PriorAuthCopilotRequest request) {
        var answer = groundedAnswerService.answer(new GroundedAnswerRequest(
                request.corpusId(),
                request.corpusVersion(),
                request.question(),
                request.projectContext(),
                List.of()
        ));

        var text = (request.question() + " " + (request.projectContext() == null ? "" : request.projectContext())
                + " " + (request.scenarioHint() == null ? "" : request.scenarioHint())).toLowerCase(Locale.ROOT);
        var scenarioType = scenarioType(text);
        var workflowStage = workflowStage(text, scenarioType);

        return new PriorAuthCopilotResponse(
                "copilot_" + UUID.randomUUID(),
                answer.status(),
                Instant.now(clock),
                new PriorAuthCopilotResponse.Input(
                        request.corpusId(),
                        request.corpusVersion(),
                        request.question(),
                        request.projectContext(),
                        request.scenarioHint()
                ),
                scenarioType,
                workflowStage,
                personas(text),
                summarize(answer.status(), scenarioType, workflowStage),
                touchpoints(text, scenarioType),
                reviewerWarnings(answer.status(), text),
                assumptions(text),
                nextActions(answer.status(), scenarioType),
                answer.findings(),
                true,
                "This prior-authorization copilot is workflow-aware but non-authoritative. Human review is required before treating PAS, CRD, or DTR guidance as implementation direction."
        );
    }

    private String summarize(String answerStatus, String scenarioType, String workflowStage) {
        if (!"grounded".equals(answerStatus)) {
            return "The copilot accepted the scenario but the current corpus did not provide enough cited evidence for a grounded PAS/CRD/DTR workflow answer. Review corpus coverage before making architecture or implementation decisions.";
        }
        return "The copilot identified a likely " + scenarioType + " scenario in the " + workflowStage + " stage and paired the grounded evidence with likely standards touchpoints for reviewer inspection.";
    }

    private String scenarioType(String text) {
        if (text.contains("dtr") || text.contains("documentation") || text.contains("questionnaire")) return "DTR";
        if (text.contains("crd") || text.contains("coverage") || text.contains("discovery")) return "CRD";
        if (text.contains("pas") || text.contains("claim") || text.contains("prior authorization")) return "PAS";
        return "prior_authorization_general";
    }

    private String workflowStage(String text, String scenarioType) {
        if (text.contains("coverage") || text.contains("discover") || "CRD".equals(scenarioType)) return "requirements_discovery";
        if (text.contains("documentation") || text.contains("questionnaire") || "DTR".equals(scenarioType)) return "documentation_capture";
        if (text.contains("status") || text.contains("decision") || text.contains("response")) return "decision_and_follow_up";
        return "submission_preparation";
    }

    private List<String> personas(String text) {
        var personas = new LinkedHashSet<String>();
        if (text.contains("provider") || text.contains("ehr")) personas.add("provider_workflow_owner");
        if (text.contains("payer")) personas.add("payer_or_utilization_management_partner");
        if (text.contains("review") || text.contains("approve")) personas.add("human_reviewer");
        if (personas.isEmpty()) personas.add("integration_engineer");
        personas.add("healthforge_reviewer");
        return personas.stream().toList();
    }

    private List<PriorAuthCopilotResponse.Touchpoint> touchpoints(String text, String scenarioType) {
        var matches = new ArrayList<PriorAuthCopilotResponse.Touchpoint>();
        if ("PAS".equals(scenarioType) || text.contains("claim")) {
            addArtifacts(matches, standardsArtifactService.list(null, "claim"), "Likely request/response touchpoint for prior-authorization submission and status handling.");
            addArtifacts(matches, standardsArtifactService.list(null, "pas"), "Candidate PAS guide artifact for prior-authorization implementation review.");
        }
        if ("CRD".equals(scenarioType)) {
            addArtifacts(matches, standardsArtifactService.list(null, "crd"), "Candidate discovery guidance for coverage and documentation requirement checks.");
        }
        if ("DTR".equals(scenarioType)) {
            addArtifacts(matches, standardsArtifactService.list(null, "dtr"), "Candidate documentation workflow guidance for questionnaire or rules-driven capture.");
        }
        if (matches.isEmpty()) {
            addArtifacts(matches, standardsArtifactService.list(null, "claim"), "General prior-authorization resource touchpoint surfaced from the curated standards registry.");
        }
        return matches.stream().limit(6).toList();
    }

    private void addArtifacts(
            List<PriorAuthCopilotResponse.Touchpoint> matches,
            dev.healthforge.platform.standards.StandardsArtifactResponse response,
            String reason
    ) {
        response.artifacts().forEach(artifact -> {
            var duplicate = matches.stream().anyMatch(existing -> existing.artifactId().equals(artifact.artifactId()));
            if (!duplicate) {
                matches.add(new PriorAuthCopilotResponse.Touchpoint(
                        artifact.artifactId(),
                        artifact.title(),
                        artifact.artifactType(),
                        artifact.canonicalUrl(),
                        reason
                ));
            }
        });
    }

    private List<String> reviewerWarnings(String answerStatus, String text) {
        var warnings = new ArrayList<String>();
        if (!"grounded".equals(answerStatus)) {
            warnings.add("The current corpus did not yield a grounded answer. Expand evidence coverage before relying on the copilot output.");
        }
        warnings.add("Do not treat PAS, CRD, or DTR artifact presence as proof of counterparty support.");
        warnings.add("Do not convert this workflow guidance into payer-specific conformance claims without human review.");
        if (text.contains("payer-specific") || text.contains("specific payer")) {
            warnings.add("Payer-specific behavior remains out of scope for the bounded local copilot unless separately reviewed and sourced.");
        }
        return warnings;
    }

    private List<String> assumptions(String text) {
        var assumptions = new ArrayList<String>();
        assumptions.add("The scenario remains within the current non-sensitive MVP boundary and does not require live patient data.");
        assumptions.add("A human reviewer will validate standards applicability, guide versioning, and counterparty behavior before implementation.");
        if (text.contains("status") || text.contains("decision")) {
            assumptions.add("The workflow may involve asynchronous status or follow-up handling and should not be treated as a single synchronous exchange.");
        }
        return assumptions;
    }

    private List<String> nextActions(String answerStatus, String scenarioType) {
        var nextActions = new ArrayList<String>();
        if ("grounded".equals(answerStatus)) {
            nextActions.add("Create or update a reviewable Brief for the scenario.");
            nextActions.add("Run architecture review for the identified workflow stage.");
            if ("PAS".equals(scenarioType)) {
                nextActions.add("Export approved work items for PAS-oriented request/response handling after review.");
            }
        } else {
            nextActions.add("Refine the workflow question or expand corpus coverage before architecture or implementation planning.");
        }
        nextActions.add("Validate standards applicability and deployment assumptions with a qualified reviewer.");
        return nextActions;
    }
}
