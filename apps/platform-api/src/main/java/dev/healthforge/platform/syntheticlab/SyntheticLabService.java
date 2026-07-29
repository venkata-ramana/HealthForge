package dev.healthforge.platform.syntheticlab;

import dev.healthforge.platform.bundlereview.PriorAuthBundleReviewRequest;
import dev.healthforge.platform.bundlereview.PriorAuthBundleReviewResponse;
import dev.healthforge.platform.bundlereview.PriorAuthBundleReviewService;
import dev.healthforge.platform.journey.PriorAuthJourneyRequest;
import dev.healthforge.platform.journey.PriorAuthJourneyResponse;
import dev.healthforge.platform.journey.PriorAuthJourneyService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SyntheticLabService {

    private static final String DEFAULT_CORPUS_ID = "mvp-regulatory-corpus";
    private static final String DEFAULT_CORPUS_VERSION = "2026-07-24-expanded-web-core-v4";

    private final PriorAuthBundleReviewService bundleReviewService;
    private final PriorAuthJourneyService journeyService;
    private final Clock clock = Clock.systemUTC();
    private final Map<String, Template> templates = new LinkedHashMap<>();

    public SyntheticLabService(
            PriorAuthBundleReviewService bundleReviewService,
            PriorAuthJourneyService journeyService
    ) {
        this.bundleReviewService = bundleReviewService;
        this.journeyService = journeyService;
        templates.put("provider_submission_baseline", new Template(
                "provider_submission_baseline",
                "Provider PAS submission baseline",
                "Rehearse provider-side request preparation and outbound PAS-oriented submission planning.",
                "PAS",
                "provider_intake_claim_valid",
                "valid",
                "How should a provider workflow package and submit a prior authorization request?",
                "Synthetic provider EHR submission rehearsal for a PAS-oriented prior authorization workflow.",
                List.of("provider_staff", "provider_system", "reviewer"),
                List.of("provider", "pas", "request_submission")
        ));
        templates.put("payer_decision_followup", new Template(
                "payer_decision_followup",
                "Payer decision and follow-up bundle",
                "Inspect multi-resource bundle handling for payer response review and follow-up normalization.",
                "PAS",
                "payer_decision_bundle_valid",
                "valid",
                "How should a payer-facing prior authorization workflow handle decision, status, and follow-up review?",
                "Synthetic payer decision rehearsal with bundle-level follow-up handling and reviewer checkpoints.",
                List.of("payer_or_um_system", "reviewer", "provider_staff"),
                List.of("payer", "pas", "decision_and_follow_up", "bundle")
        ));
        templates.put("coverage_discovery_rehearsal", new Template(
                "coverage_discovery_rehearsal",
                "Coverage discovery rehearsal",
                "Model a CRD-style discovery flow using synthetic coverage context and reviewer-visible requirements.",
                "CRD",
                "coverage_discovery_valid",
                "valid",
                "How should a provider workflow discover prior authorization and documentation requirements before submission?",
                "Synthetic CRD-style discovery rehearsal for coverage and documentation requirement checks.",
                List.of("provider_staff", "provider_system", "reviewer"),
                List.of("provider", "crd", "requirements_discovery")
        ));
        templates.put("documentation_capture_rehearsal", new Template(
                "documentation_capture_rehearsal",
                "Documentation capture rehearsal",
                "Walk a DTR-style documentation workflow with explicit launch, capture, and return stages.",
                "DTR",
                "prior_auth_bundle_valid",
                "valid",
                "How should a documentation workflow capture and return structured answers for prior authorization planning?",
                "Synthetic DTR-style documentation rehearsal with questionnaire-oriented handoff expectations.",
                List.of("provider_staff", "dtr_application", "reviewer"),
                List.of("provider", "dtr", "documentation_capture", "bundle")
        ));
        templates.put("negative_bundle_structure", new Template(
                "negative_bundle_structure",
                "Negative bundle structure drill",
                "Run a negative scenario that highlights invalid bundle structure and expected debug behavior.",
                "PAS",
                "documentation_followup_bundle_invalid",
                "invalid",
                "What should reviewers and engineers inspect when a prior authorization bundle is structurally invalid?",
                "Synthetic negative drill for bundle validation, replay comparison, and debug handoff.",
                List.of("reviewer", "integration_engineer"),
                List.of("negative", "bundle", "validation_debug")
        ));
    }

    public SyntheticLabOverviewResponse overview() {
        return new SyntheticLabOverviewResponse(
                Instant.now(clock),
                "The synthetic lab expands HealthForge from isolated fixtures into repeatable scenario-based workflow rehearsals with coverage, validation, and comparison views.",
                new SyntheticLabOverviewResponse.CoverageSummary(
                        templates.size(),
                        (int) templates.values().stream().filter(template -> "valid".equals(template.expectedValidationStatus())).count(),
                        (int) templates.values().stream().filter(template -> "invalid".equals(template.expectedValidationStatus())).count(),
                        templates.values().stream().map(Template::journeyType).distinct().toList()
                ),
                templates.values().stream().map(template -> new SyntheticLabOverviewResponse.TemplateSummary(
                        template.templateId(),
                        template.title(),
                        template.description(),
                        template.journeyType(),
                        template.syntheticScenarioId(),
                        template.expectedValidationStatus(),
                        template.primaryActors(),
                        template.coverageTags()
                )).toList(),
                List.of(
                        new SyntheticLabOverviewResponse.SupportMatrixEntry("PAS request + follow-up", "covered", List.of("provider_submission_baseline", "payer_decision_followup"), "Covers request assembly, submission, and decision handling with valid multi-step scenarios."),
                        new SyntheticLabOverviewResponse.SupportMatrixEntry("CRD discovery", "covered", List.of("coverage_discovery_rehearsal"), "Supports requirements discovery and handoff to downstream workflow planning."),
                        new SyntheticLabOverviewResponse.SupportMatrixEntry("DTR documentation", "covered", List.of("documentation_capture_rehearsal"), "Supports documentation launch, capture, and return planning."),
                        new SyntheticLabOverviewResponse.SupportMatrixEntry("Negative structural debugging", "partial", List.of("negative_bundle_structure"), "Covers invalid bundle structure but not every invalid counterparty or auth edge case.")
                ),
                List.of(
                        new SyntheticLabOverviewResponse.ValidationGap("Counterparty variation", "medium", "No synthetic lab currently distinguishes multiple payer counterparty behavior models.", "payer_decision_followup"),
                        new SyntheticLabOverviewResponse.ValidationGap("Async retry patterns", "medium", "Retry and repeated pending-state scenarios are not yet modeled separately.", "payer_decision_followup"),
                        new SyntheticLabOverviewResponse.ValidationGap("Documentation negative paths", "low", "There is no dedicated invalid DTR questionnaire payload scenario yet.", "negative_bundle_structure")
                ),
                List.of(
                        "Start with provider_submission_baseline for the fastest happy-path demonstration.",
                        "Use negative_bundle_structure to explain regression assertions and debug workflows.",
                        "Compare coverage_discovery_rehearsal with documentation_capture_rehearsal to show CRD vs DTR differences."
                )
        );
    }

    public SyntheticLabRunResponse run(SyntheticLabRunRequest request) {
        var template = requireTemplate(request.templateId());
        return runTemplate(template);
    }

    public SyntheticLabCompareResponse compare(SyntheticLabCompareRequest request) {
        var primary = runTemplate(requireTemplate(request.primaryTemplateId()));
        var comparison = runTemplate(requireTemplate(request.comparisonTemplateId()));
        var primaryTypes = String.join(", ", primary.replayMetadata().resourceTypes());
        var comparisonTypes = String.join(", ", comparison.replayMetadata().resourceTypes());

        return new SyntheticLabCompareResponse(
                Instant.now(clock),
                request.primaryTemplateId(),
                request.comparisonTemplateId(),
                "This comparison highlights structural, workflow, and validation differences between two synthetic lab runs so reviewers can inspect replay and regression behavior more clearly.",
                List.of(
                        new SyntheticLabCompareResponse.Difference(
                                "journey_type",
                                primary.journey().journeyType(),
                                comparison.journey().journeyType(),
                                primary.journey().journeyType().equals(comparison.journey().journeyType())
                                        ? "Both runs exercise the same workflow family."
                                        : "These runs exercise different workflow families and should be interpreted with different expected stages."
                        ),
                        new SyntheticLabCompareResponse.Difference(
                                "validation_status",
                                primary.bundleReview().validation().status(),
                                comparison.bundleReview().validation().status(),
                                "Use this difference to compare expected positive-path vs negative-path behavior."
                        ),
                        new SyntheticLabCompareResponse.Difference(
                                "resource_types",
                                primaryTypes,
                                comparisonTypes,
                                primaryTypes.equals(comparisonTypes)
                                        ? "Resource composition is similar across runs."
                                        : "Resource composition differs, which changes what validation and workflow assertions should be expected."
                        ),
                        new SyntheticLabCompareResponse.Difference(
                                "assertion_pass_count",
                                String.valueOf(primary.assertions().stream().filter(assertion -> "pass".equals(assertion.status())).count()),
                                String.valueOf(comparison.assertions().stream().filter(assertion -> "pass".equals(assertion.status())).count()),
                                "Assertion count helps explain regression behavior between scenarios."
                        )
                ),
                timelineComparisons(primary.journey(), comparison.journey()),
                List.of(
                        "Use this view to compare a happy path against the negative bundle structure drill.",
                        "Compare CRD and DTR templates to show how workflow responsibilities and stages change.",
                        "Use replay differences as a structured debug conversation instead of relying on raw bundle inspection alone."
                )
        );
    }

    private SyntheticLabRunResponse runTemplate(Template template) {
        var bundleReview = bundleReviewService.review(new PriorAuthBundleReviewRequest(
                DEFAULT_CORPUS_ID,
                DEFAULT_CORPUS_VERSION,
                template.question(),
                template.projectContext(),
                template.syntheticScenarioId()
        ));
        var journey = journeyService.build(new PriorAuthJourneyRequest(
                DEFAULT_CORPUS_ID,
                DEFAULT_CORPUS_VERSION,
                template.question(),
                template.projectContext(),
                template.journeyType()
        ));
        return new SyntheticLabRunResponse(
                "synthetic_lab_run_" + UUID.randomUUID(),
                template.templateId(),
                template.title(),
                Instant.now(clock),
                summarize(template, bundleReview, journey),
                bundleReview,
                journey,
                assertions(template, bundleReview, journey),
                expectedOutcomes(template),
                timeline(template, journey),
                new SyntheticLabRunResponse.ReplayMetadata(
                        bundleReview.bundleInventory().bundleType(),
                        bundleReview.bundleInventory().resourceTypes(),
                        "Compare this run with another template to inspect bundle structure, validation status, and workflow-stage differences."
                )
        );
    }

    private String summarize(Template template, PriorAuthBundleReviewResponse bundleReview, PriorAuthJourneyResponse journey) {
        return "The " + template.title() + " run packages a " + journey.journeyType() + " workflow rehearsal with bundle validation, scenario assertions, and stage-by-stage expectations for synthetic-safe debugging and regression review.";
    }

    private List<SyntheticLabRunResponse.Assertion> assertions(
            Template template,
            PriorAuthBundleReviewResponse bundleReview,
            PriorAuthJourneyResponse journey
    ) {
        var resourceTypes = new LinkedHashSet<>(bundleReview.bundleInventory().resourceTypes());
        return List.of(
                new SyntheticLabRunResponse.Assertion(
                        "expected_validation_status",
                        "Expected validation status matches the template",
                        template.expectedValidationStatus().equals(bundleReview.validation().status()) ? "pass" : "fail",
                        "Template expects " + template.expectedValidationStatus() + " and the run produced " + bundleReview.validation().status() + "."
                ),
                new SyntheticLabRunResponse.Assertion(
                        "journey_type_alignment",
                        "Journey type aligns with the template workflow family",
                        template.journeyType().equals(journey.journeyType()) ? "pass" : "fail",
                        "Template expects " + template.journeyType() + " and the run produced " + journey.journeyType() + "."
                ),
                new SyntheticLabRunResponse.Assertion(
                        "bundle_resource_presence",
                        "Bundle includes the expected core resource types",
                        resourceTypes.isEmpty() ? "fail" : "pass",
                        "Observed resource types: " + String.join(", ", resourceTypes)
                ),
                new SyntheticLabRunResponse.Assertion(
                        "reviewer_visible_timeline",
                        "Workflow run exposes explicit stage transitions",
                        journey.workflowStages().isEmpty() ? "fail" : "pass",
                        "Observed " + journey.workflowStages().size() + " workflow stages and " + journey.stateTransitions().size() + " transitions."
                )
        );
    }

    private List<String> expectedOutcomes(Template template) {
        return List.of(
                "Reviewers can inspect a " + template.journeyType() + " workflow path without sensitive data.",
                "Validation outcomes stay explicit and suitable for regression comparison.",
                "Bundle composition and workflow stages can be replayed and compared across runs.",
                "The scenario remains bounded as synthetic planning and testing support rather than a conformance claim."
        );
    }

    private List<SyntheticLabRunResponse.TimelineEvent> timeline(Template template, PriorAuthJourneyResponse journey) {
        return journey.workflowStages().stream().map(stage -> new SyntheticLabRunResponse.TimelineEvent(
                stage.stageId(),
                stage.title(),
                ownerForStage(template, stage.stageId()),
                stage.expectedOutputs().isEmpty() ? "reviewer_visible_output" : stage.expectedOutputs().getFirst()
        )).toList();
    }

    private String ownerForStage(Template template, String stageId) {
        if (stageId.contains("decision") || stageId.contains("follow_up")) return "payer_or_um_system";
        if (stageId.contains("documentation")) return "dtr_application";
        return template.primaryActors().getFirst();
    }

    private List<SyntheticLabCompareResponse.TimelineComparison> timelineComparisons(
            PriorAuthJourneyResponse primary,
            PriorAuthJourneyResponse comparison
    ) {
        var max = Math.max(primary.workflowStages().size(), comparison.workflowStages().size());
        var comparisons = new java.util.ArrayList<SyntheticLabCompareResponse.TimelineComparison>();
        for (int index = 0; index < max; index++) {
            var primaryStage = index < primary.workflowStages().size() ? primary.workflowStages().get(index) : null;
            var comparisonStage = index < comparison.workflowStages().size() ? comparison.workflowStages().get(index) : null;
            comparisons.add(new SyntheticLabCompareResponse.TimelineComparison(
                    "timeline_" + index,
                    primaryStage == null ? "n/a" : primaryStage.title(),
                    comparisonStage == null ? "n/a" : comparisonStage.title(),
                    primaryStage == null || comparisonStage == null
                            ? "One workflow has more stages than the other."
                            : primaryStage.stageId().equals(comparisonStage.stageId())
                            ? "These runs share a similar stage position."
                            : "These runs diverge at this stage and should be interpreted differently."
            ));
        }
        return comparisons;
    }

    private Template requireTemplate(String templateId) {
        var template = templates.get(templateId);
        if (template == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown synthetic lab template");
        }
        return template;
    }

    private record Template(
            String templateId,
            String title,
            String description,
            String journeyType,
            String syntheticScenarioId,
            String expectedValidationStatus,
            String question,
            String projectContext,
            List<String> primaryActors,
            List<String> coverageTags
    ) {
    }
}
