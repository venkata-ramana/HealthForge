package dev.healthforge.platform.bundlereview;

import com.fasterxml.jackson.databind.JsonNode;
import dev.healthforge.platform.answer.GroundedAnswerRequest;
import dev.healthforge.platform.answer.GroundedAnswerService;
import dev.healthforge.platform.fhir.FhirValidationRequest;
import dev.healthforge.platform.fhir.FhirValidationResponse;
import dev.healthforge.platform.fhir.FhirValidationService;
import dev.healthforge.platform.fhirsynthetic.SyntheticFhirGenerateRequest;
import dev.healthforge.platform.fhirsynthetic.SyntheticFhirGenerateResponse;
import dev.healthforge.platform.fhirsynthetic.SyntheticFhirService;
import dev.healthforge.platform.journey.PriorAuthJourneyResponse;
import dev.healthforge.platform.standards.StandardsArtifactResponse;
import dev.healthforge.platform.standards.StandardsArtifactService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PriorAuthBundleReviewService {

    private static final String DEFAULT_SCENARIO_ID = "prior_auth_bundle_valid";

    private final SyntheticFhirService syntheticFhirService;
    private final FhirValidationService fhirValidationService;
    private final GroundedAnswerService groundedAnswerService;
    private final StandardsArtifactService standardsArtifactService;
    private final Clock clock = Clock.systemUTC();

    public PriorAuthBundleReviewService(
            SyntheticFhirService syntheticFhirService,
            FhirValidationService fhirValidationService,
            GroundedAnswerService groundedAnswerService,
            StandardsArtifactService standardsArtifactService
    ) {
        this.syntheticFhirService = syntheticFhirService;
        this.fhirValidationService = fhirValidationService;
        this.groundedAnswerService = groundedAnswerService;
        this.standardsArtifactService = standardsArtifactService;
    }

    public PriorAuthBundleReviewResponse review(PriorAuthBundleReviewRequest request) {
        var scenarioId = request.scenarioId() == null || request.scenarioId().isBlank()
                ? DEFAULT_SCENARIO_ID
                : request.scenarioId();

        var scenario = syntheticFhirService.generate(new SyntheticFhirGenerateRequest(scenarioId));
        var resource = scenario.resource();

        if (!"Bundle".equals(resource.path("resourceType").asText())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Scenario must resolve to a Bundle resource for bundle review");
        }

        var validation = validateScenario(scenario);
        var answer = groundedAnswerService.answer(new GroundedAnswerRequest(
                request.corpusId(),
                request.corpusVersion(),
                request.question(),
                request.projectContext(),
                List.of()
        ));

        var workflowContext = workflowContext(request);
        var bundleInventory = inventory(resource);

        return new PriorAuthBundleReviewResponse(
                "bundle_review_" + UUID.randomUUID(),
                answer.status(),
                Instant.now(clock),
                new PriorAuthBundleReviewResponse.Input(
                        request.corpusId(),
                        request.corpusVersion(),
                        request.question(),
                        request.projectContext()
                ),
                new PriorAuthBundleReviewResponse.SyntheticScenario(
                        scenario.scenarioId(),
                        scenario.title(),
                        scenario.description(),
                        scenario.expectedValidationStatus()
                ),
                summarize(answer.status(), validation, bundleInventory, workflowContext),
                bundleInventory,
                workflowContext,
                scenarioFindings(resource, validation, workflowContext),
                validation,
                answer.findings(),
                reviewerWarnings(answer.status(), validation.status()),
                nextActions(answer.status(), validation.status(), workflowContext.journeyType()),
                true,
                "This bundle review is a synthetic prior-authorization planning artifact only. It highlights workflow structure and standards touchpoints, but human review is required before any implementation, compliance, or counterparty-support conclusion."
        );
    }

    private FhirValidationResponse validateScenario(SyntheticFhirGenerateResponse scenario) {
        return fhirValidationService.validate(new FhirValidationRequest(
                scenario.validationRecommendation().packageId(),
                scenario.validationRecommendation().packageVersion(),
                scenario.validationRecommendation().profileUrl(),
                scenario.dataClassification(),
                scenario.resource()
        ));
    }

    private PriorAuthBundleReviewResponse.BundleInventory inventory(JsonNode bundle) {
        var entryNodes = bundle.path("entry");
        var resourceTypes = new ArrayList<String>();
        var references = new LinkedHashSet<String>();
        entryNodes.forEach(entry -> {
            var resource = entry.path("resource");
            var type = resource.path("resourceType").asText();
            if (!type.isBlank()) {
                resourceTypes.add(type);
            }
            collectReferences(resource, references);
        });
        return new PriorAuthBundleReviewResponse.BundleInventory(
                bundle.path("id").asText("synthetic-bundle"),
                bundle.path("type").asText("missing"),
                entryNodes.size(),
                resourceTypes,
                references.stream().limit(12).toList()
        );
    }

    private void collectReferences(JsonNode node, LinkedHashSet<String> references) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            var referenceNode = node.get("reference");
            if (referenceNode != null && referenceNode.isTextual()) {
                references.add(referenceNode.asText());
            }
            node.elements().forEachRemaining(child -> collectReferences(child, references));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectReferences(child, references));
        }
    }

    private PriorAuthBundleReviewResponse.WorkflowContext workflowContext(PriorAuthBundleReviewRequest request) {
        var text = ((request.question() == null ? "" : request.question()) + " "
                + (request.projectContext() == null ? "" : request.projectContext())).toLowerCase(Locale.ROOT);
        var journeyType = journeyType(text);
        return new PriorAuthBundleReviewResponse.WorkflowContext(
                journeyType,
                primaryPersona(text),
                workflowStages(journeyType),
                stateTransitions(journeyType),
                responsibilities(journeyType),
                standardsTouchpoints(journeyType)
        );
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

    private List<PriorAuthJourneyResponse.WorkflowStage> workflowStages(String journeyType) {
        return switch (journeyType) {
            case "CRD" -> List.of(
                    new PriorAuthJourneyResponse.WorkflowStage("requirements_discovery", "Requirements discovery", "Determine whether prior authorization or documentation guidance should be surfaced during ordering.", List.of("CRD", "CDS Hooks", "Coverage", "ServiceRequest"), List.of("Coverage requirements guidance", "Reviewer-visible discovery result")),
                    new PriorAuthJourneyResponse.WorkflowStage("requirements_review", "Requirements review", "Confirm which returned guidance is actionable in the local provider workflow.", List.of("CRD", "Questionnaire", "QuestionnaireResponse"), List.of("Accepted discovery inputs", "Escalation or manual follow-up decision")),
                    new PriorAuthJourneyResponse.WorkflowStage("handoff_to_documentation_or_submission", "Documentation or submission handoff", "Route the workflow to structured documentation capture or direct submission preparation.", List.of("DTR", "PAS"), List.of("Next workflow stage selection"))
            );
            case "DTR" -> List.of(
                    new PriorAuthJourneyResponse.WorkflowStage("documentation_launch", "Documentation launch", "Initiate structured documentation capture with workflow context and reviewer checkpoints.", List.of("DTR", "Questionnaire", "QuestionnaireResponse"), List.of("Documentation session context")),
                    new PriorAuthJourneyResponse.WorkflowStage("documentation_capture", "Documentation capture", "Collect or pre-populate structured answers while preserving user edits and provenance.", List.of("DTR", "QuestionnaireResponse", "DocumentReference"), List.of("Completed documentation packet", "Reviewer-visible assumptions")),
                    new PriorAuthJourneyResponse.WorkflowStage("documentation_return", "Documentation return", "Return structured documentation back to the provider workflow for final submission decisions.", List.of("DTR", "Bundle", "DocumentReference"), List.of("Submission-ready documentation output"))
            );
            default -> List.of(
                    new PriorAuthJourneyResponse.WorkflowStage("submission_preparation", "Submission preparation", "Assemble the request, supporting context, and candidate PAS/FHIR artifacts before outbound exchange.", List.of("PAS", "Claim", "Coverage", "Organization"), List.of("Submission-ready request draft")),
                    new PriorAuthJourneyResponse.WorkflowStage("request_submission", "Request submission", "Transmit the prior-authorization request to the payer or delegated endpoint through the selected integration boundary.", List.of("PAS", "Claim", "Bundle"), List.of("Submitted request event", "Transport/audit metadata")),
                    new PriorAuthJourneyResponse.WorkflowStage("decision_and_follow_up", "Decision and follow-up", "Handle responses, pending states, additional-information requests, and downstream reviewer ownership.", List.of("PAS", "ClaimResponse", "Bundle"), List.of("Normalized status", "Reviewer follow-up action"))
            );
        };
    }

    private List<PriorAuthJourneyResponse.StateTransition> stateTransitions(String journeyType) {
        return switch (journeyType) {
            case "CRD" -> List.of(
                    new PriorAuthJourneyResponse.StateTransition("requirements_discovery", "requirements_review", "coverage requirements are returned", "Human review determines whether the discovery output is actionable or informational."),
                    new PriorAuthJourneyResponse.StateTransition("requirements_review", "handoff_to_documentation_or_submission", "documentation or submission path is selected", "The next step depends on what the returned requirements actually demand.")
            );
            case "DTR" -> List.of(
                    new PriorAuthJourneyResponse.StateTransition("documentation_launch", "documentation_capture", "documentation workflow is opened", "Structured questions or rules-driven prompts are presented to the user."),
                    new PriorAuthJourneyResponse.StateTransition("documentation_capture", "documentation_return", "documentation is completed or reviewed", "Returned data should remain traceable to the workflow session and reviewer context.")
            );
            default -> List.of(
                    new PriorAuthJourneyResponse.StateTransition("submission_preparation", "request_submission", "request package is approved for exchange", "Guide version, payload shape, and endpoint assumptions remain reviewer-owned decisions."),
                    new PriorAuthJourneyResponse.StateTransition("request_submission", "decision_and_follow_up", "payer or intermediary returns status", "Treat the response as potentially asynchronous and reviewer-visible rather than universally final.")
            );
        };
    }

    private List<PriorAuthJourneyResponse.Responsibility> responsibilities(String journeyType) {
        return switch (journeyType) {
            case "CRD" -> List.of(
                    new PriorAuthJourneyResponse.Responsibility("provider_staff", "provider_workflow_owner", "Initiates discovery from the provider workflow and reviews surfaced requirements.", "requirements_discovery"),
                    new PriorAuthJourneyResponse.Responsibility("provider_system", "clinical_workflow_client", "Captures order context and launches discovery requests.", "requirements_discovery"),
                    new PriorAuthJourneyResponse.Responsibility("reviewer", "human_reviewer", "Confirms whether returned discovery guidance should influence documentation or submission planning.", "requirements_review")
            );
            case "DTR" -> List.of(
                    new PriorAuthJourneyResponse.Responsibility("provider_staff", "provider_workflow_owner", "Launches and completes the documentation workflow with contextual oversight.", "documentation_launch"),
                    new PriorAuthJourneyResponse.Responsibility("dtr_application", "documentation_workflow_aid", "Collects or pre-populates structured documentation responses.", "documentation_capture"),
                    new PriorAuthJourneyResponse.Responsibility("reviewer", "human_reviewer", "Validates that returned documentation is sufficient for downstream planning but not a conformance claim.", "documentation_return")
            );
            default -> List.of(
                    new PriorAuthJourneyResponse.Responsibility("provider_staff", "provider_workflow_owner", "Prepares the request and confirms the request context is reviewable.", "submission_preparation"),
                    new PriorAuthJourneyResponse.Responsibility("provider_system", "clinical_workflow_client", "Packages the request and supporting information for outbound submission.", "request_submission"),
                    new PriorAuthJourneyResponse.Responsibility("payer_or_um_system", "decision_maker", "Returns approval, denial, pending, or additional-information status.", "decision_and_follow_up"),
                    new PriorAuthJourneyResponse.Responsibility("reviewer", "human_reviewer", "Normalizes outcome handling, follow-up ownership, and evidence retention.", "decision_and_follow_up")
            );
        };
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

    private String summarize(
            String answerStatus,
            FhirValidationResponse validation,
            PriorAuthBundleReviewResponse.BundleInventory inventory,
            PriorAuthBundleReviewResponse.WorkflowContext workflowContext
    ) {
        if (!"grounded".equals(answerStatus)) {
            return "The synthetic bundle could be inspected structurally, but the current corpus did not provide enough cited evidence for a grounded scenario explanation. Treat the artifact as a planning scaffold only.";
        }
        if ("valid".equals(validation.status())) {
            return "The synthetic " + workflowContext.journeyType() + " bundle review found a structurally valid multi-resource bundle with "
                    + inventory.entryCount() + " entries and workflow-aware touchpoints for reviewer inspection.";
        }
        return "The synthetic " + workflowContext.journeyType() + " bundle review surfaced bundle-level validation findings together with workflow context so reviewers can distinguish structural gaps from process guidance.";
    }

    private List<PriorAuthBundleReviewResponse.ScenarioFinding> scenarioFindings(
            JsonNode bundle,
            FhirValidationResponse validation,
            PriorAuthBundleReviewResponse.WorkflowContext workflowContext
    ) {
        var findings = new ArrayList<PriorAuthBundleReviewResponse.ScenarioFinding>();
        var entryCount = bundle.path("entry").size();
        findings.add(new PriorAuthBundleReviewResponse.ScenarioFinding(
                "bundle_composition",
                entryCount >= 2 ? "info" : "warning",
                "Bundle composition review",
                "The scenario contains " + entryCount + " bundle entr" + (entryCount == 1 ? "y" : "ies")
                        + " for " + workflowContext.journeyType() + " workflow inspection.",
                validation.evidenceLinks()
        ));
        if (bundle.path("type").isMissingNode() || bundle.path("type").asText().isBlank()) {
            findings.add(new PriorAuthBundleReviewResponse.ScenarioFinding(
                    "bundle_structure",
                    "high",
                    "Bundle.type is missing",
                    "The bundle cannot be treated as structurally complete until Bundle.type is supplied.",
                    validation.evidenceLinks()
            ));
        } else {
            findings.add(new PriorAuthBundleReviewResponse.ScenarioFinding(
                    "bundle_structure",
                    "info",
                    "Bundle.type is present",
                    "The synthetic bundle declares type '" + bundle.path("type").asText() + "', which supports clearer workflow packaging review.",
                    validation.evidenceLinks()
            ));
        }
        if (resourceTypes(bundle).contains("Claim") && resourceTypes(bundle).contains("Coverage")) {
            findings.add(new PriorAuthBundleReviewResponse.ScenarioFinding(
                    "workflow_context",
                    "info",
                    "Request and coverage context are both represented",
                    "The scenario includes Claim and Coverage resources, which is more realistic for prior-authorization planning than single-resource review alone.",
                    validation.evidenceLinks()
            ));
        }
        findings.addAll(validation.findings().stream()
                .limit(4)
                .map(finding -> new PriorAuthBundleReviewResponse.ScenarioFinding(
                        "validation_finding",
                        finding.severity(),
                        "FHIR validation finding",
                        finding.location() + ": " + finding.message(),
                        finding.evidenceLinks()
                ))
                .toList());
        return findings;
    }

    private List<String> resourceTypes(JsonNode bundle) {
        var values = new ArrayList<String>();
        bundle.path("entry").forEach(entry -> {
            var type = entry.path("resource").path("resourceType").asText();
            if (!type.isBlank()) {
                values.add(type);
            }
        });
        return values;
    }

    private List<String> reviewerWarnings(String answerStatus, String validationStatus) {
        var warnings = new ArrayList<String>();
        if (!"grounded".equals(answerStatus)) {
            warnings.add("The current corpus did not provide enough cited evidence for a grounded scenario explanation.");
        }
        if (!"valid".equals(validationStatus)) {
            warnings.add("Bundle-level validation findings indicate structural issues that must be resolved before treating the scenario as a clean exchange example.");
        }
        warnings.add("This review does not prove PAS, CRD, DTR, or payer-specific production interoperability.");
        warnings.add("Human review remains required before using bundle review output for implementation, conformance, or operational claims.");
        return warnings;
    }

    private List<String> nextActions(String answerStatus, String validationStatus, String journeyType) {
        var actions = new ArrayList<String>();
        actions.add("Review the scenario findings alongside the rendered workflow stages with provider and payer stakeholders.");
        if (!"valid".equals(validationStatus)) {
            actions.add("Fix bundle-level structural findings before using this scenario as a demo of successful exchange packaging.");
        } else {
            actions.add("Use the valid multi-resource bundle to demo how request, coverage, and organization context travel together.");
        }
        if ("grounded".equals(answerStatus)) {
            actions.add("Use this review as input to the next standards crosswalk work in Phase 7.");
        } else {
            actions.add("Refine the question or expand the corpus before relying on the scenario summary as implementation guidance.");
        }
        if ("PAS".equals(journeyType)) {
            actions.add("Compare the bundle stages with PAS request-submission and follow-up handling before counterparty-specific planning.");
        }
        return actions;
    }
}
