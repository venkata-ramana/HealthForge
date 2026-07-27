package dev.healthforge.platform.journey;

import dev.healthforge.platform.answer.GroundedAnswerRequest;
import dev.healthforge.platform.answer.GroundedAnswerService;
import dev.healthforge.platform.standards.StandardsArtifactResponse;
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
public class PriorAuthJourneyService {

    private final GroundedAnswerService groundedAnswerService;
    private final StandardsArtifactService standardsArtifactService;
    private final Clock clock = Clock.systemUTC();

    public PriorAuthJourneyService(
            GroundedAnswerService groundedAnswerService,
            StandardsArtifactService standardsArtifactService
    ) {
        this.groundedAnswerService = groundedAnswerService;
        this.standardsArtifactService = standardsArtifactService;
    }

    public PriorAuthJourneyResponse build(PriorAuthJourneyRequest request) {
        var answer = groundedAnswerService.answer(new GroundedAnswerRequest(
                request.corpusId(),
                request.corpusVersion(),
                request.question(),
                request.projectContext(),
                List.of()
        ));

        var text = normalizedText(request);
        var journeyType = journeyType(text);

        return new PriorAuthJourneyResponse(
                "journey_" + UUID.randomUUID(),
                answer.status(),
                Instant.now(clock),
                new PriorAuthJourneyResponse.Input(
                        request.corpusId(),
                        request.corpusVersion(),
                        request.question(),
                        request.projectContext(),
                        request.scenarioHint()
                ),
                journeyType,
                primaryPersona(text),
                summary(answer.status(), journeyType),
                workflowStages(journeyType),
                stateTransitions(journeyType),
                responsibilities(journeyType),
                standardsTouchpoints(journeyType),
                reviewerWarnings(answer.status(), journeyType),
                nextActions(answer.status(), journeyType),
                answer.findings(),
                true,
                "This journey artifact models PAS, CRD, or DTR workflow structure for planning and demo use only. Human review is required before treating it as implementation direction."
        );
    }

    private String normalizedText(PriorAuthJourneyRequest request) {
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

    private String summary(String answerStatus, String journeyType) {
        if (!"grounded".equals(answerStatus)) {
            return "The journey was identified as " + journeyType + ", but the current corpus did not provide enough cited evidence for a grounded workflow explanation. Review the structure as a planning scaffold only.";
        }
        return "The platform mapped the scenario to a " + journeyType + " journey and rendered explicit workflow stages, transitions, responsibilities, and candidate standards touchpoints for reviewer inspection.";
    }

    private List<PriorAuthJourneyResponse.WorkflowStage> workflowStages(String journeyType) {
        return switch (journeyType) {
            case "CRD" -> List.of(
                    stage("requirements_discovery", "Requirements discovery", "Determine whether prior authorization or documentation guidance should be surfaced during ordering.", List.of("CRD", "CDS Hooks", "Coverage", "ServiceRequest"), List.of("Coverage requirements guidance", "Reviewer-visible discovery result")),
                    stage("requirements_review", "Requirements review", "Confirm which returned guidance is actionable in the local provider workflow.", List.of("CRD", "Questionnaire", "QuestionnaireResponse"), List.of("Accepted discovery inputs", "Escalation or manual follow-up decision")),
                    stage("handoff_to_documentation_or_submission", "Documentation or submission handoff", "Route the workflow to structured documentation capture or direct submission preparation.", List.of("DTR", "PAS"), List.of("Next workflow stage selection"))
            );
            case "DTR" -> List.of(
                    stage("documentation_launch", "Documentation launch", "Initiate structured documentation capture with workflow context and reviewer checkpoints.", List.of("DTR", "Questionnaire", "QuestionnaireResponse"), List.of("Documentation session context")),
                    stage("documentation_capture", "Documentation capture", "Collect or pre-populate structured answers while preserving user edits and provenance.", List.of("DTR", "QuestionnaireResponse", "DocumentReference"), List.of("Completed documentation packet", "Reviewer-visible assumptions")),
                    stage("documentation_return", "Documentation return", "Return structured documentation back to the provider workflow for final submission decisions.", List.of("DTR", "Bundle", "DocumentReference"), List.of("Submission-ready documentation output"))
            );
            default -> List.of(
                    stage("submission_preparation", "Submission preparation", "Assemble the request, supporting context, and candidate PAS/FHIR artifacts before outbound exchange.", List.of("PAS", "Claim", "Coverage", "Organization"), List.of("Submission-ready request draft")),
                    stage("request_submission", "Request submission", "Transmit the prior-authorization request to the payer or delegated endpoint through the selected integration boundary.", List.of("PAS", "Claim", "Bundle"), List.of("Submitted request event", "Transport/audit metadata")),
                    stage("decision_and_follow_up", "Decision and follow-up", "Handle responses, pending states, additional-information requests, and downstream reviewer ownership.", List.of("PAS", "ClaimResponse", "Bundle"), List.of("Normalized status", "Reviewer follow-up action"))
            );
        };
    }

    private PriorAuthJourneyResponse.WorkflowStage stage(
            String stageId,
            String title,
            String objective,
            List<String> candidateStandards,
            List<String> expectedOutputs
    ) {
        return new PriorAuthJourneyResponse.WorkflowStage(stageId, title, objective, candidateStandards, expectedOutputs);
    }

    private List<PriorAuthJourneyResponse.StateTransition> stateTransitions(String journeyType) {
        return switch (journeyType) {
            case "CRD" -> List.of(
                    transition("requirements_discovery", "requirements_review", "coverage requirements are returned", "Human review determines whether the discovery output is actionable or informational."),
                    transition("requirements_review", "handoff_to_documentation_or_submission", "documentation or submission path is selected", "The next step depends on what the returned requirements actually demand.")
            );
            case "DTR" -> List.of(
                    transition("documentation_launch", "documentation_capture", "documentation workflow is opened", "Structured questions or rules-driven prompts are presented to the user."),
                    transition("documentation_capture", "documentation_return", "documentation is completed or reviewed", "Returned data should remain traceable to the workflow session and reviewer context.")
            );
            default -> List.of(
                    transition("submission_preparation", "request_submission", "request package is approved for exchange", "Guide version, payload shape, and endpoint assumptions remain reviewer-owned decisions."),
                    transition("request_submission", "decision_and_follow_up", "payer or intermediary returns status", "Treat the response as potentially asynchronous and reviewer-visible rather than universally final.")
            );
        };
    }

    private PriorAuthJourneyResponse.StateTransition transition(
            String fromStage,
            String toStage,
            String trigger,
            String transitionNote
    ) {
        return new PriorAuthJourneyResponse.StateTransition(fromStage, toStage, trigger, transitionNote);
    }

    private List<PriorAuthJourneyResponse.Responsibility> responsibilities(String journeyType) {
        return switch (journeyType) {
            case "CRD" -> List.of(
                    responsibility("provider_staff", "provider_workflow_owner", "Initiates discovery from the provider workflow and reviews surfaced requirements.", "requirements_discovery"),
                    responsibility("provider_system", "clinical_workflow_client", "Captures order context and launches discovery requests.", "requirements_discovery"),
                    responsibility("reviewer", "human_reviewer", "Confirms whether returned discovery guidance should influence documentation or submission planning.", "requirements_review")
            );
            case "DTR" -> List.of(
                    responsibility("provider_staff", "provider_workflow_owner", "Launches and completes the documentation workflow with contextual oversight.", "documentation_launch"),
                    responsibility("dtr_application", "documentation_workflow_aid", "Collects or pre-populates structured documentation responses.", "documentation_capture"),
                    responsibility("reviewer", "human_reviewer", "Validates that returned documentation is sufficient for downstream planning but not a conformance claim.", "documentation_return")
            );
            default -> List.of(
                    responsibility("provider_staff", "provider_workflow_owner", "Prepares the request and confirms the request context is reviewable.", "submission_preparation"),
                    responsibility("provider_system", "clinical_workflow_client", "Packages the request and supporting information for outbound submission.", "request_submission"),
                    responsibility("payer_or_um_system", "decision_maker", "Returns approval, denial, pending, or additional-information status.", "decision_and_follow_up"),
                    responsibility("reviewer", "human_reviewer", "Normalizes outcome handling, follow-up ownership, and evidence retention.", "decision_and_follow_up")
            );
        };
    }

    private PriorAuthJourneyResponse.Responsibility responsibility(
            String actor,
            String systemRole,
            String responsibility,
            String stageId
    ) {
        return new PriorAuthJourneyResponse.Responsibility(actor, systemRole, responsibility, stageId);
    }

    private List<PriorAuthJourneyResponse.StandardsTouchpoint> standardsTouchpoints(String journeyType) {
        var results = new ArrayList<PriorAuthJourneyResponse.StandardsTouchpoint>();
        switch (journeyType) {
            case "CRD" -> addArtifacts(results, "requirements_discovery", standardsArtifactService.list(null, "crd"),
                    "Candidate discovery guidance for coverage and documentation requirement checks.");
            case "DTR" -> addArtifacts(results, "documentation_capture", standardsArtifactService.list(null, "dtr"),
                    "Candidate documentation workflow guidance for questionnaire or rules-driven capture.");
            default -> {
                addArtifacts(results, "submission_preparation", standardsArtifactService.list(null, "claim"),
                        "Likely request structure touchpoint for prior-authorization submission planning.");
                addArtifacts(results, "request_submission", standardsArtifactService.list(null, "pas"),
                        "Candidate PAS guide artifact for prior-authorization exchange review.");
                addArtifacts(results, "decision_and_follow_up", standardsArtifactService.list(null, "claimresponse"),
                        "Likely response/status touchpoint for prior-authorization follow-up handling.");
            }
        }
        return results.stream().limit(8).toList();
    }

    private void addArtifacts(
            List<PriorAuthJourneyResponse.StandardsTouchpoint> results,
            String stageId,
            StandardsArtifactResponse response,
            String reason
    ) {
        var seen = new LinkedHashSet<String>();
        results.forEach(existing -> seen.add(existing.artifactId()));
        response.artifacts().forEach(artifact -> {
            if (seen.add(artifact.artifactId())) {
                results.add(new PriorAuthJourneyResponse.StandardsTouchpoint(
                        stageId,
                        artifact.artifactId(),
                        artifact.title(),
                        artifact.artifactType(),
                        artifact.canonicalUrl(),
                        reason
                ));
            }
        });
    }

    private List<String> reviewerWarnings(String answerStatus, String journeyType) {
        var warnings = new ArrayList<String>();
        if (!"grounded".equals(answerStatus)) {
            warnings.add("The current corpus did not yield enough cited evidence for a grounded " + journeyType + " journey explanation.");
        }
        warnings.add("Do not treat PAS, CRD, or DTR touchpoints as proof of counterparty support.");
        warnings.add("Do not treat workflow-stage visibility as a conformance or compliance determination.");
        warnings.add("Human review remains required before using this journey for architecture, implementation, or trading-partner claims.");
        return warnings;
    }

    private List<String> nextActions(String answerStatus, String journeyType) {
        var actions = new ArrayList<String>();
        actions.add("Review the explicit stage transitions with a provider and payer domain owner.");
        actions.add("Validate candidate standards applicability for the chosen counterparty and deployment.");
        if ("grounded".equals(answerStatus)) {
            actions.add("Use this journey as input to architecture review and later bundle-level scenario review.");
            if ("PAS".equals(journeyType)) {
                actions.add("Compare request-submission and follow-up stages against approved Brief work items.");
            }
        } else {
            actions.add("Refine the scenario question or expand corpus coverage before relying on the journey as implementation guidance.");
        }
        return actions;
    }
}
