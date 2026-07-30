package dev.healthforge.platform.implementation;

import dev.healthforge.platform.architecture.ArchitectureReviewRequest;
import dev.healthforge.platform.architecture.ArchitectureReviewResponse;
import dev.healthforge.platform.architecture.ArchitectureReviewService;
import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.brief.BriefResponse;
import dev.healthforge.platform.brief.BriefService;
import dev.healthforge.platform.brief.BriefWorkItemExportResponse;
import dev.healthforge.platform.codegen.StarterCodeGenerationRequest;
import dev.healthforge.platform.codegen.StarterCodeGenerationResponse;
import dev.healthforge.platform.codegen.StarterCodeGenerationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ImplementationBundleService {

    private final BriefService briefService;
    private final StarterCodeGenerationService starterCodeGenerationService;
    private final ArchitectureReviewService architectureReviewService;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public ImplementationBundleService(
            BriefService briefService,
            StarterCodeGenerationService starterCodeGenerationService,
            ArchitectureReviewService architectureReviewService,
            JdbcTemplate jdbcTemplate
    ) {
        this.briefService = briefService;
        this.starterCodeGenerationService = starterCodeGenerationService;
        this.architectureReviewService = architectureReviewService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ImplementationBundleResponse generate(String briefId, AuthenticatedActor actor) {
        var brief = briefService.get(briefId, actor);
        var export = briefService.exportWorkItems(briefId, actor);
        var architectureReview = architectureReviewService.review(new ArchitectureReviewRequest(
                brief.input().corpusId(),
                brief.input().corpusVersion(),
                brief.input().question(),
                brief.input().projectContext(),
                List.of()
        ));
        var starterArtifacts = starterArtifacts(export, actor);

        return new ImplementationBundleResponse(
                export.briefId(),
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                new ImplementationBundleResponse.HandoffSummary(
                        brief.input().question(),
                        "approved_brief_ready_for_implementation_handoff",
                        export.workItems().size(),
                        export.implementationTracks().size(),
                        "This pack turns an approved Brief into implementation-ready starter artifacts, architecture patterns, test planning, and change-impact guidance while preserving reviewed traceability."
                ),
                architecturePatterns(export, architectureReview),
                testPlan(export),
                changeImpact(brief),
                new ImplementationBundleResponse.HandoffBundle(
                        export,
                        architectureReview,
                        implementationSummary(export, architectureReview),
                        List.of(
                                "approved_brief_context",
                                "implementation_tracks",
                                "starter_code_artifacts",
                                "acceptance_criteria_and_validation_cases",
                                "change_impact_summary"
                        )
                ),
                releaseBundle(brief, export, architectureReview),
                starterArtifacts,
                List.of(
                        "Starter artifacts are examples only and remain subject to human engineering review.",
                        "Implementation guidance stays grounded in approved findings and evidence-bearing work items only.",
                        "Change-impact summaries identify likely maintenance pressure but do not replace source review when upstream regulations or standards change."
                )
        );
    }

    private List<StarterCodeGenerationResponse> starterArtifacts(BriefWorkItemExportResponse export, AuthenticatedActor actor) {
        return export.workItems().stream()
                .limit(3)
                .flatMap(workItem -> List.of(
                        "spring_boot_endpoint_stub",
                        "spring_service_stub",
                        "fhir_client_stub",
                        "workflow_adapter_stub"
                ).stream().map(artifactType -> starterCodeGenerationService.generate(
                        new StarterCodeGenerationRequest(export.briefId(), workItem.workItemId(), artifactType),
                        actor
                )))
                .toList();
    }

    private List<ImplementationBundleResponse.ArchitecturePattern> architecturePatterns(
            BriefWorkItemExportResponse export,
            ArchitectureReviewResponse architectureReview
    ) {
        var patterns = new ArrayList<ImplementationBundleResponse.ArchitecturePattern>();
        for (var track : export.implementationTracks()) {
            patterns.add(new ImplementationBundleResponse.ArchitecturePattern(
                    track.title(),
                    track.actorFocus(),
                    track.summary(),
                    List.of(
                            "Focus workflow stages: " + String.join(", ", track.workflowStages()),
                            "Key dependencies: " + String.join(", ", track.dependencies()),
                            "Standards touchpoints: " + String.join(", ", track.standardsTouchpoints())
                    ),
                    List.of(
                            "Keep transport, documentation capture, and status handling decoupled where counterparty behavior can vary.",
                            "Preserve review and approval traceability when turning planning outputs into implementation tasks."
                    )
            ));
        }
        architectureReview.components().stream().limit(3).forEach(component ->
                patterns.add(new ImplementationBundleResponse.ArchitecturePattern(
                        component.name(),
                        "shared",
                        component.rationale(),
                        List.of(component.role()),
                        List.of("Validate deployment applicability before implementation.", "Review security, auth, and workflow ownership boundaries.")
                )));
        return patterns;
    }

    private ImplementationBundleResponse.TestPlan testPlan(BriefWorkItemExportResponse export) {
        var acceptanceCriteria = export.workItems().stream()
                .map(item -> new ImplementationBundleResponse.AcceptanceCriterion(
                        "criterion_" + item.workItemId(),
                        item.title(),
                        "Implementation satisfies the approved rationale, preserves cited workflow intent, and passes bounded validation review for " + item.affectedCapability() + ".",
                        item.primaryTrack()
                ))
                .toList();
        var validationScenarios = export.workItems().stream()
                .map(item -> new ImplementationBundleResponse.ValidationScenario(
                        "validation_" + item.workItemId(),
                        "Validate " + item.workflowStage() + " for " + item.title(),
                        "positive_path",
                        "Confirm the implementation covers " + item.workflowStage() + ", preserves traceability, and aligns with " + String.join(", ", item.standardsTouchpoints()) + "."
                ))
                .toList();
        var negativeCases = export.workItems().stream()
                .map(item -> new ImplementationBundleResponse.ValidationScenario(
                        "negative_" + item.workItemId(),
                        "Negative case for " + item.title(),
                        "negative_path",
                        "Confirm unsupported inputs, missing prerequisites, or counterparty mismatches fail safely without implying unsupported automation."
                ))
                .toList();
        var traceabilityLinks = export.workItems().stream()
                .flatMap(item -> item.relatedFindingIds().stream().map(findingId -> new ImplementationBundleResponse.TraceabilityLink(
                        findingId,
                        item.workItemId(),
                        item.workflowStage() + " + " + item.affectedCapability()
                )))
                .toList();
        return new ImplementationBundleResponse.TestPlan(acceptanceCriteria, validationScenarios, negativeCases, traceabilityLinks);
    }

    private ImplementationBundleResponse.ChangeImpact changeImpact(BriefResponse brief) {
        var signals = brief.sources().stream()
                .map(source -> {
                    var latestVersion = jdbcTemplate.query("""
                                    select source_version
                                    from source_version
                                    where manifest_source_id = ?
                                    order by retrieved_at desc
                                    limit 1
                                    """,
                            rs -> rs.next() ? rs.getString("source_version") : source.sourceVersion(),
                            source.sourceId()
                    );
                    var changed = latestVersion != null && !latestVersion.equals(source.sourceVersion());
                    return new ImplementationBundleResponse.SourceChangeSignal(
                            source.sourceId(),
                            source.sourceVersion(),
                            latestVersion == null ? source.sourceVersion() : latestVersion,
                            changed ? "newer_source_available" : "no_known_change",
                            changed
                                    ? "Review the newer source version and decide whether the approved implementation artifacts require re-review."
                                    : "Current brief source version appears current within the indexed knowledge base."
                    );
                })
                .toList();
        var changedSources = signals.stream().filter(signal -> "newer_source_available".equals(signal.changeStatus())).count();
        return new ImplementationBundleResponse.ChangeImpact(
                signals,
                changedSources > 0
                        ? List.of(
                        "Re-open the approved Brief and confirm whether newer sources change implementation assumptions.",
                        "Regenerate starter artifacts and test plans after review if source applicability changes."
                )
                        : List.of(
                        "No indexed newer source version was detected for the cited sources used in this Brief.",
                        "Re-run change impact after future corpus refreshes or standards updates."
                ),
                changedSources > 0
                        ? "One or more cited sources may have newer indexed versions, so downstream implementation work should pause for a bounded re-review."
                        : "The indexed knowledge base does not currently show newer versions for the sources cited by this Brief, so this implementation pack can be used as the working baseline."
        );
    }

    private List<String> implementationSummary(
            BriefWorkItemExportResponse export,
            ArchitectureReviewResponse architectureReview
    ) {
        var summary = new LinkedHashSet<String>();
        summary.add("Approved work items: " + export.workItems().size());
        summary.add("Implementation tracks: " + export.implementationTracks().size());
        summary.add("Architecture components highlighted: " + architectureReview.components().size());
        summary.add("Review checkpoints to preserve: " + architectureReview.reviewCheckpoints().size());
        export.implementationTracks().forEach(track ->
                summary.add(track.title() + " covers " + String.join(", ", track.workflowStages())));
        return summary.stream().toList();
    }

    private ImplementationBundleResponse.ReleaseBundle releaseBundle(
            BriefResponse brief,
            BriefWorkItemExportResponse export,
            ArchitectureReviewResponse architectureReview
    ) {
        return new ImplementationBundleResponse.ReleaseBundle(
                "grouped_for_downstream_handoff",
                List.of(
                        new ImplementationBundleResponse.ArtifactGroup(
                                "Reviewed planning artifacts",
                                "reviewer_and_approver",
                                List.of("approved Brief context", "accepted work items", "approval trace", "audit events")
                        ),
                        new ImplementationBundleResponse.ArtifactGroup(
                                "Engineering kickoff pack",
                                "implementation_team",
                                List.of("implementation tracks", "starter artifacts", "architecture patterns", "acceptance criteria")
                        ),
                        new ImplementationBundleResponse.ArtifactGroup(
                                "Operational delivery pack",
                                "platform_operator",
                                List.of("change impact signals", "negative validation cases", "release handoff summary")
                        )
                ),
                List.of(
                        new ImplementationBundleResponse.DownstreamPackage(
                                "pkg_reviewed_brief_" + export.briefId(),
                                "implementation_team",
                                "json",
                                List.of("work_item_export", "traceability_links", "approval_trace"),
                                "Preserve approved upstream reasoning when downstream teams create tracked work."
                        ),
                        new ImplementationBundleResponse.DownstreamPackage(
                                "pkg_architecture_" + export.briefId(),
                                "architecture_and_delivery",
                                "json",
                                List.of("architecture_review", "implementation_summary", "change_impact"),
                                "Group architecture, release, and maintenance signals into one handoff-ready package."
                        )
                ),
                List.of(
                        "Brief " + brief.briefId() + " approved with " + brief.approvals().size() + " approval record(s).",
                        "Release packaging includes " + export.workItems().size() + " approved work item(s) and " + architectureReview.components().size() + " architecture component(s).",
                        "Downstream bundles stay bounded to approved findings, cited evidence, and human-reviewed artifacts."
                ),
                "Use this release bundle to hand approved planning outputs to engineering and operations without losing approval, evidence, or change-impact context."
        );
    }
}
