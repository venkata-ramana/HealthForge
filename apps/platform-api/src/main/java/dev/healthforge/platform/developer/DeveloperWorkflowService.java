package dev.healthforge.platform.developer;

import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.brief.BriefService;
import dev.healthforge.platform.brief.BriefSummary;
import dev.healthforge.platform.brief.BriefWorkItemExportResponse;
import dev.healthforge.platform.implementation.ImplementationBundleService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeveloperWorkflowService {

    private final BriefService briefService;
    private final ImplementationBundleService implementationBundleService;
    private final Clock clock = Clock.systemUTC();

    public DeveloperWorkflowService(
            BriefService briefService,
            ImplementationBundleService implementationBundleService
    ) {
        this.briefService = briefService;
        this.implementationBundleService = implementationBundleService;
    }

    public DeveloperOverviewResponse overview(AuthenticatedActor actor) {
        var approvedBriefs = briefService.list(actor).stream()
                .filter(brief -> "approved".equals(brief.status()))
                .toList();

        return new DeveloperOverviewResponse(
                actor.organizationId(),
                Instant.now(clock),
                approvedBriefs,
                List.of(
                        new DeveloperOverviewResponse.WorkspaceSurface(
                                "vscode_extension",
                                "VS Code engineering companion",
                                "ide",
                                "Move from approved briefs into repo-aware implementation guidance, workspace insights, and synthetic lab runs without leaving the editor.",
                                List.of(
                                        "Create grounded brief drafts",
                                        "Browse approved briefs",
                                        "Generate repo-aware implementation guidance",
                                        "Inspect workspace overview",
                                        "Run synthetic interoperability labs"
                                )
                        ),
                        new DeveloperOverviewResponse.WorkspaceSurface(
                                "healthforge_cli",
                                "HealthForge CLI",
                                "cli",
                                "Run brief, workspace, lab, and repo-guidance workflows from local scripts or terminal-first developer habits.",
                                List.of(
                                        "List approved briefs",
                                        "Create brief drafts",
                                        "Inspect workspace and developer overview",
                                        "Generate repo guidance",
                                        "Execute synthetic lab runs"
                                )
                        ),
                        new DeveloperOverviewResponse.WorkspaceSurface(
                                "builder_sdk",
                                "Builder SDK",
                                "sdk",
                                "Embed supported HealthForge API workflows into external engineering tools, demos, and automation wrappers.",
                                List.of(
                                        "Call brief, workspace, and developer workflow APIs",
                                        "Reuse actor/header helpers",
                                        "Package external demo integrations safely"
                                )
                        )
                ),
                List.of(
                        new DeveloperOverviewResponse.AutomationRecipe(
                                "brief_quality_gate",
                                "Evaluation quality gate",
                                "./scripts/check-evaluation-gate.sh evals/reports/2026-07-24-expanded-web-core-v4.json",
                                "Verify evaluation baselines before presenting a new corpus snapshot or shipping a workflow update.",
                                List.of("pass/fail status", "quality gate summary")
                        ),
                        new DeveloperOverviewResponse.AutomationRecipe(
                                "developer_cli_overview",
                                "Developer surface smoke check",
                                "node apps/cli/bin/healthforge.js developer:overview",
                                "Confirm the local API exposes the builder-facing Phase 18 workflows.",
                                List.of("developer overview JSON", "approved brief list", "automation recipe list")
                        ),
                        new DeveloperOverviewResponse.AutomationRecipe(
                                "synthetic_regression",
                                "Synthetic lab workflow regression",
                                "node apps/cli/bin/healthforge.js labs:run provider_submission_baseline",
                                "Run a repeatable interoperability rehearsal before a demo or delivery checkpoint.",
                                List.of("lab assertions", "timeline summary", "bundle review highlights")
                        )
                ),
                List.of(
                        "Developer workflow surfaces remain local-first and demo-safe.",
                        "Repo-aware guidance stays grounded in approved briefs and bounded file inventory hints rather than autonomous code changes.",
                        "CLI, SDK, and IDE helpers do not remove required human review, approval, or architecture validation."
                )
        );
    }

    public DeveloperRepoGuidanceResponse repoGuidance(DeveloperRepoGuidanceRequest request, AuthenticatedActor actor) {
        var export = briefService.exportWorkItems(request.briefId(), actor);
        var implementationBundle = implementationBundleService.generate(request.briefId(), actor);
        var inventory = normalize(request.repositoryInventory());
        var changedFiles = normalize(request.changedFiles());
        var detectedTechnologySignals = detectTechnologySignals(inventory, changedFiles);
        var topWorkItems = export.workItems().stream().limit(4).toList();
        var fileSuggestions = buildFileSuggestions(topWorkItems, inventory, changedFiles);

        return new DeveloperRepoGuidanceResponse(
                request.briefId(),
                request.repositoryName().trim(),
                request.workspaceRoot().trim(),
                Instant.now(clock),
                "This repo guidance package ties the approved brief to likely implementation touchpoints in the local repository so teams can move from review to bounded engineering execution faster.",
                new DeveloperRepoGuidanceResponse.RepoContext(
                        inventory.size(),
                        changedFiles.size(),
                        detectedTechnologySignals,
                        changedFiles
                ),
                topWorkItems.stream().map(this::toFocus).toList(),
                fileSuggestions,
                automationSteps(request, topWorkItems, implementationBundle),
                traceabilityNotes(topWorkItems, implementationBundle),
                List.of(
                        "Treat repo suggestions as implementation guidance, not autonomous code authority.",
                        "Regenerated starter artifacts and repo suggestions should be re-reviewed if the approved brief or cited sources change.",
                        "Changed files without a work-item match still require human triage before assumptions are carried into implementation."
                )
        );
    }

    private DeveloperRepoGuidanceResponse.ImplementationFocus toFocus(BriefWorkItemExportResponse.WorkItem workItem) {
        return new DeveloperRepoGuidanceResponse.ImplementationFocus(
                workItem.workItemId(),
                workItem.title(),
                workItem.workflowStage(),
                workItem.affectedCapability(),
                workItem.rationale(),
                workItem.dependencies(),
                workItem.standardsTouchpoints(),
                workItem.validationNotes()
        );
    }

    private List<DeveloperRepoGuidanceResponse.FileSuggestion> buildFileSuggestions(
            List<BriefWorkItemExportResponse.WorkItem> workItems,
            List<String> inventory,
            List<String> changedFiles
    ) {
        var suggestions = new ArrayList<DeveloperRepoGuidanceResponse.FileSuggestion>();
        var prioritizedFiles = new ArrayList<String>();
        prioritizedFiles.addAll(changedFiles);
        prioritizedFiles.addAll(inventory);

        for (var workItem : workItems) {
            var matches = prioritizedFiles.stream()
                    .filter(path -> fileMatchesWorkItem(path, workItem))
                    .distinct()
                    .limit(4)
                    .toList();
            if (matches.isEmpty()) {
                suggestions.add(new DeveloperRepoGuidanceResponse.FileSuggestion(
                        inferFallbackPath(workItem),
                        "no direct repo match found",
                        "Create or extend a module that handles " + workItem.workflowStage() + " and preserves approved traceability for " + workItem.affectedCapability() + ".",
                        List.of(workItem.workItemId())
                ));
                continue;
            }
            for (var match : matches) {
                suggestions.add(new DeveloperRepoGuidanceResponse.FileSuggestion(
                        match,
                        changedFiles.contains(match) ? "recently changed file aligns with approved work item" : "existing repo file appears aligned with approved work item",
                        "Review this file against the approved work item rationale, validation notes, and standards touchpoints before implementation.",
                        List.of(workItem.workItemId())
                ));
            }
        }

        return suggestions.stream()
                .collect(Collectors.toMap(
                        DeveloperRepoGuidanceResponse.FileSuggestion::path,
                        suggestion -> suggestion,
                        (left, right) -> new DeveloperRepoGuidanceResponse.FileSuggestion(
                                left.path(),
                                left.matchReason(),
                                left.recommendation(),
                                mergeIds(left.relatedWorkItemIds(), right.relatedWorkItemIds())
                        )
                ))
                .values()
                .stream()
                .toList();
    }

    private List<String> mergeIds(List<String> left, List<String> right) {
        var merged = new LinkedHashSet<String>();
        merged.addAll(left);
        merged.addAll(right);
        return merged.stream().toList();
    }

    private boolean fileMatchesWorkItem(String path, BriefWorkItemExportResponse.WorkItem workItem) {
        var normalized = path.toLowerCase(Locale.ROOT);
        var keywords = new LinkedHashSet<String>();
        keywords.addAll(tokenize(workItem.title()));
        keywords.addAll(tokenize(workItem.workflowStage()));
        keywords.addAll(tokenize(workItem.affectedCapability()));
        keywords.addAll(workItem.dependencies().stream().flatMap(item -> tokenize(item).stream()).toList());
        return keywords.stream().anyMatch(normalized::contains);
    }

    private String inferFallbackPath(BriefWorkItemExportResponse.WorkItem workItem) {
        if (workItem.affectedCapability().toLowerCase(Locale.ROOT).contains("fhir")
                || workItem.workflowStage().toLowerCase(Locale.ROOT).contains("bundle")) {
            return "apps/platform-api/src/main/java/dev/healthforge/platform/fhir/";
        }
        if (workItem.workflowStage().toLowerCase(Locale.ROOT).contains("review")
                || workItem.workflowStage().toLowerCase(Locale.ROOT).contains("approval")) {
            return "apps/platform-api/src/main/java/dev/healthforge/platform/brief/";
        }
        return "apps/platform-api/src/main/java/dev/healthforge/platform/implementation/";
    }

    private List<DeveloperRepoGuidanceResponse.AutomationStep> automationSteps(
            DeveloperRepoGuidanceRequest request,
            List<BriefWorkItemExportResponse.WorkItem> topWorkItems,
            dev.healthforge.platform.implementation.ImplementationBundleResponse implementationBundle
    ) {
        var workItemIds = topWorkItems.stream().map(BriefWorkItemExportResponse.WorkItem::workItemId).toList();
        return List.of(
                new DeveloperRepoGuidanceResponse.AutomationStep(
                        "inspect_bundle",
                        "Inspect the approved implementation bundle",
                        "curl http://localhost:8080/v1/implementation/briefs/" + request.briefId() + "/bundle -H 'X-HealthForge-Actor: local.approver' -H 'X-HealthForge-Role: approver' -H 'X-HealthForge-Organization: tenant.alpha'",
                        "See starter artifacts, architecture patterns, and validation scenarios tied to the approved brief."
                ),
                new DeveloperRepoGuidanceResponse.AutomationStep(
                        "generate_repo_guidance",
                        "Re-run repo guidance from the CLI",
                        "node apps/cli/bin/healthforge.js repo:guide --brief-id " + request.briefId() + " --repo-name " + request.repositoryName() + " --workspace-root " + request.workspaceRoot(),
                        "Refresh file-touchpoint guidance after the repo changes."
                ),
                new DeveloperRepoGuidanceResponse.AutomationStep(
                        "preserve_traceability",
                        "Keep traceability visible in implementation work",
                        "Focus work items: " + String.join(", ", workItemIds),
                        "Carry these approved work-item IDs into commits, PRs, and test notes so planning-to-repo linkage remains reviewable."
                ),
                new DeveloperRepoGuidanceResponse.AutomationStep(
                        "validate_changed_workflow",
                        "Validate the workflow after implementation",
                        "node apps/cli/bin/healthforge.js labs:run provider_submission_baseline",
                        "Confirm the changed implementation story still aligns with the synthetic interoperability lab baseline."
                )
        );
    }

    private List<String> traceabilityNotes(
            List<BriefWorkItemExportResponse.WorkItem> topWorkItems,
            dev.healthforge.platform.implementation.ImplementationBundleResponse implementationBundle
    ) {
        var notes = new ArrayList<String>();
        notes.add("Approved work items in focus: " + topWorkItems.stream().map(BriefWorkItemExportResponse.WorkItem::workItemId).collect(Collectors.joining(", ")));
        notes.add("Implementation tracks available: " + implementationBundle.handoffBundle().workItemExport().implementationTracks().size());
        notes.add("Validation scenarios available: " + implementationBundle.testPlan().validationScenarios().size());
        notes.add("Architecture patterns available: " + implementationBundle.architecturePatterns().size());
        return notes;
    }

    private List<String> detectTechnologySignals(List<String> inventory, List<String> changedFiles) {
        var signals = new LinkedHashSet<String>();
        var combined = new ArrayList<String>();
        combined.addAll(inventory);
        combined.addAll(changedFiles);
        for (var path : combined) {
            var normalized = path.toLowerCase(Locale.ROOT);
            if (normalized.endsWith(".java")) {
                signals.add("java");
            }
            if (normalized.endsWith(".ts") || normalized.endsWith(".tsx")) {
                signals.add("typescript");
            }
            if (normalized.endsWith(".js")) {
                signals.add("javascript");
            }
            if (normalized.contains("docker")) {
                signals.add("docker");
            }
            if (normalized.contains("terraform")) {
                signals.add("terraform");
            }
            if (normalized.contains("fhir")) {
                signals.add("fhir");
            }
            if (normalized.contains("workflow") || normalized.contains("brief")) {
                signals.add("workflow");
            }
        }
        if (signals.isEmpty()) {
            signals.add("repo_inventory_not_supplied");
        }
        return signals.stream().toList();
    }

    private List<String> normalize(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private Set<String> tokenize(String input) {
        if (input == null || input.isBlank()) {
            return Set.of();
        }
        return List.of(input.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")).stream()
                .filter(token -> token.length() >= 4)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
