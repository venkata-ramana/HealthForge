package dev.healthforge.platform.workspace;

import dev.healthforge.platform.auth.ActorRole;
import dev.healthforge.platform.auth.AuthProperties;
import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final JdbcTemplate jdbcTemplate;
    private final AuthProperties authProperties;
    private final Clock clock = Clock.systemUTC();

    public WorkspaceService(JdbcTemplate jdbcTemplate, AuthProperties authProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.authProperties = authProperties;
    }

    public WorkspaceOverviewResponse overview(AuthenticatedActor actor) {
        ensureSeedData(actor);
        return new WorkspaceOverviewResponse(
                actor.organizationId(),
                Instant.now(clock),
                authFoundation(actor.organizationId()),
                projects(actor.organizationId()),
                queues(actor.organizationId()),
                assignments(actor.organizationId()),
                workflowConfigurations(actor.organizationId()),
                savedViews(actor.organizationId()),
                evidenceCollections(actor.organizationId()),
                researchPacks(actor.organizationId()),
                questionPacks(actor.organizationId()),
                scenarioTemplates(actor.organizationId()),
                personaPresets(),
                precedentComparisons(actor.organizationId()),
                decisionPatterns(actor.organizationId()),
                themeClusters(actor.organizationId()),
                researchNotebooks(actor.organizationId()),
                topicBrowser(actor.organizationId()),
                reviewerOperations(actor.organizationId())
        );
    }

    public WorkspaceOverviewResponse.ProjectSummary createProject(WorkspaceProjectRequest request, AuthenticatedActor actor) {
        ensureSeedData(actor);
        var projectId = "project_" + UUID.randomUUID();
        var now = Timestamp.from(Instant.now(clock));
        jdbcTemplate.update("""
                insert into workspace_project (project_id, organization_id, name, kind, description, owner_actor_id, tags, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                projectId,
                actor.organizationId(),
                request.name().trim(),
                request.kind().trim(),
                request.description().trim(),
                actor.actorId(),
                normalizeTags(request.tags()),
                now,
                now
        );
        jdbcTemplate.update("""
                insert into evidence_collection (collection_id, organization_id, project_id, name, summary, source_count, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "collection_" + UUID.randomUUID(),
                actor.organizationId(),
                projectId,
                request.name().trim() + " evidence workspace",
                "Reusable evidence workspace for " + request.name().trim() + ".",
                0,
                now,
                now
        );
        return project(actor.organizationId(), projectId);
    }

    public WorkspaceOverviewResponse.ProjectSummary linkBrief(String projectId, WorkspaceProjectLinkRequest request, AuthenticatedActor actor) {
        ensureProject(projectId, actor.organizationId());
        var briefCount = jdbcTemplate.queryForObject("""
                select count(*) from engineering_brief where brief_id = ? and organization_id = ?
                """, Integer.class, request.briefId(), actor.organizationId());
        if (briefCount == null || briefCount == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Brief was not found in the current organization.");
        }
        jdbcTemplate.update("""
                insert into workspace_project_brief (project_id, brief_id, linked_at)
                values (?, ?, ?) on conflict do nothing
                """, projectId, request.briefId(), Timestamp.from(Instant.now(clock)));
        refreshEvidenceCollectionCounts(projectId, actor.organizationId());
        return project(actor.organizationId(), projectId);
    }

    public WorkspaceOverviewResponse.AssignmentSummary createAssignment(WorkspaceAssignmentRequest request, AuthenticatedActor actor) {
        var brief = jdbcTemplate.query("""
                select brief_id, question, status from engineering_brief where brief_id = ? and organization_id = ?
                """, (rs, row) -> Map.of(
                "brief_id", rs.getString("brief_id"),
                "question", rs.getString("question"),
                "status", rs.getString("status")
        ), request.briefId(), actor.organizationId());
        if (brief.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Brief was not found in the current organization.");
        }
        var now = Timestamp.from(Instant.now(clock));
        var assignmentId = "assignment_" + UUID.randomUUID();
        jdbcTemplate.update("""
                insert into workspace_assignment
                (assignment_id, brief_id, organization_id, assignee_actor_id, assignee_role, queue_name, status, handoff_summary, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                assignmentId,
                request.briefId(),
                actor.organizationId(),
                request.assigneeActorId().trim(),
                ActorRole.parse(request.assigneeRole()).name().toLowerCase(Locale.ROOT),
                request.queueName().trim(),
                "assigned",
                request.handoffSummary().trim(),
                now,
                now
        );
        return assignments(actor.organizationId()).stream()
                .filter(item -> item.assignmentId().equals(assignmentId))
                .findFirst()
                .orElseThrow();
    }

    public WorkspaceOverviewResponse.SavedViewSummary createSavedView(WorkspaceSavedViewRequest request, AuthenticatedActor actor) {
        if (request.projectId() != null && !request.projectId().isBlank()) {
            ensureProject(request.projectId(), actor.organizationId());
        }
        var viewId = "view_" + UUID.randomUUID();
        var now = Timestamp.from(Instant.now(clock));
        jdbcTemplate.update("""
                insert into workspace_saved_view
                (view_id, organization_id, project_id, view_type, name, query_text, summary, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                viewId,
                actor.organizationId(),
                blankToNull(request.projectId()),
                request.viewType().trim(),
                request.name().trim(),
                request.queryText().trim(),
                request.summary().trim(),
                now,
                now
        );
        return savedViews(actor.organizationId()).stream()
                .filter(item -> item.viewId().equals(viewId))
                .findFirst()
                .orElseThrow();
    }

    public WorkspaceOverviewResponse.ResearchPackSummary createResearchPack(WorkspaceResearchPackRequest request, AuthenticatedActor actor) {
        if (request.projectId() != null && !request.projectId().isBlank()) {
            ensureProject(request.projectId(), actor.organizationId());
        }
        var packId = "research_pack_" + UUID.randomUUID();
        var now = Timestamp.from(Instant.now(clock));
        var recurringQuestions = normalizeRecurringQuestions(request.recurringQuestions());
        jdbcTemplate.update("""
                insert into workspace_research_pack
                (research_pack_id, organization_id, project_id, name, summary, recurring_questions, question_count, next_review_at, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                packId,
                actor.organizationId(),
                blankToNull(request.projectId()),
                request.name().trim(),
                request.summary().trim(),
                recurringQuestions,
                parseRecurringQuestions(recurringQuestions).size(),
                parseInstantOrNull(request.nextReviewDate()),
                now,
                now
        );
        return researchPacks(actor.organizationId()).stream()
                .filter(item -> item.researchPackId().equals(packId))
                .findFirst()
                .orElseThrow();
    }

    public WorkspaceOverviewResponse.QuestionPackSummary createQuestionPack(WorkspaceQuestionPackRequest request, AuthenticatedActor actor) {
        if (request.projectId() != null && !request.projectId().isBlank()) {
            ensureProject(request.projectId(), actor.organizationId());
        }
        var packId = "question_pack_" + UUID.randomUUID();
        var now = Timestamp.from(Instant.now(clock));
        var prompts = normalizeLineList(request.questionPrompts());
        jdbcTemplate.update("""
                insert into workspace_question_pack
                (question_pack_id, organization_id, project_id, name, summary, persona, template_kind, starter_question, question_prompts, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                packId,
                actor.organizationId(),
                blankToNull(request.projectId()),
                request.name().trim(),
                request.summary().trim(),
                request.persona().trim(),
                request.templateKind().trim(),
                request.starterQuestion().trim(),
                prompts,
                now,
                now
        );
        return questionPacks(actor.organizationId()).stream()
                .filter(item -> item.questionPackId().equals(packId))
                .findFirst()
                .orElseThrow();
    }

    public WorkspaceOverviewResponse.ResearchNotebookSummary createResearchNotebook(WorkspaceResearchNotebookRequest request, AuthenticatedActor actor) {
        if (request.projectId() != null && !request.projectId().isBlank()) {
            ensureProject(request.projectId(), actor.organizationId());
        }
        if (request.briefId() != null && !request.briefId().isBlank()) {
            ensureBrief(request.briefId(), actor.organizationId());
        }
        var notebookId = "notebook_" + UUID.randomUUID();
        var now = Timestamp.from(Instant.now(clock));
        jdbcTemplate.update("""
                insert into workspace_research_notebook
                (notebook_id, organization_id, project_id, brief_id, title, summary, key_takeaways, evidence_bundle_name, handoff_summary, continuity_note, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                notebookId,
                actor.organizationId(),
                blankToNull(request.projectId()),
                blankToNull(request.briefId()),
                request.title().trim(),
                request.summary().trim(),
                normalizeLineList(request.keyTakeaways()),
                request.evidenceBundleName().trim(),
                request.handoffSummary().trim(),
                request.continuityNote().trim(),
                now,
                now
        );
        return researchNotebooks(actor.organizationId()).stream()
                .filter(item -> item.notebookId().equals(notebookId))
                .findFirst()
                .orElseThrow();
    }

    public WorkspaceOverviewResponse.EscalationSummary createReviewEscalation(WorkspaceReviewEscalationRequest request, AuthenticatedActor actor) {
        ensureBrief(request.briefId(), actor.organizationId());
        if (request.assignmentId() != null && !request.assignmentId().isBlank()) {
            ensureAssignment(request.assignmentId(), actor.organizationId());
        }
        var escalationId = "escalation_" + UUID.randomUUID();
        var now = Timestamp.from(Instant.now(clock));
        jdbcTemplate.update("""
                insert into workspace_review_escalation
                (escalation_id, organization_id, assignment_id, brief_id, escalation_reason, urgency, destination_queue, status, note, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                escalationId,
                actor.organizationId(),
                blankToNull(request.assignmentId()),
                request.briefId().trim(),
                request.escalationReason().trim(),
                request.urgency().trim(),
                request.destinationQueue().trim(),
                "open",
                request.note().trim(),
                now,
                now
        );
        return reviewerOperations(actor.organizationId()).escalations().stream()
                .filter(item -> item.escalationId().equals(escalationId))
                .findFirst()
                .orElseThrow();
    }

    public WorkspaceSearchResponse search(WorkspaceSearchRequest request, AuthenticatedActor actor) {
        ensureSeedData(actor);
        var normalizedQuery = request.query().trim().toLowerCase(Locale.ROOT);
        var facet = request.facet() == null ? "all" : request.facet().trim().toLowerCase(Locale.ROOT);
        var hits = new ArrayList<WorkspaceSearchResponse.SearchHitSummary>();
        if ("all".equals(facet) || "briefs".equals(facet)) {
            hits.addAll(searchBriefs(actor.organizationId(), normalizedQuery));
        }
        if ("all".equals(facet) || "findings".equals(facet)) {
            hits.addAll(searchFindings(actor.organizationId(), normalizedQuery));
        }
        if ("all".equals(facet) || "approvals".equals(facet)) {
            hits.addAll(searchApprovals(actor.organizationId(), normalizedQuery));
        }
        if ("all".equals(facet) || "evidence".equals(facet)) {
            hits.addAll(searchEvidence(actor.organizationId(), normalizedQuery));
        }
        if ("all".equals(facet) || "workspace".equals(facet)) {
            hits.addAll(searchWorkspaceArtifacts(actor.organizationId(), normalizedQuery));
        }
        var sorted = hits.stream()
                .sorted((left, right) -> left.title().compareToIgnoreCase(right.title()))
                .limit(12)
                .toList();
        return new WorkspaceSearchResponse(
                actor.organizationId(),
                Instant.now(clock),
                request.query().trim(),
                facet,
                sorted.size(),
                sorted
        );
    }

    private void ensureSeedData(AuthenticatedActor actor) {
        seedIdentityFoundation(actor.organizationId());
        seedProjects(actor);
        seedWorkflowConfigurations(actor);
        seedSavedViews(actor);
        seedAssignments(actor);
        seedResearchPacks(actor);
        seedQuestionPacks(actor);
        seedResearchNotebooks(actor);
        seedReviewEscalations(actor);
        refreshAllEvidenceCollections(actor.organizationId());
    }

    private void seedIdentityFoundation(String organizationId) {
        var now = Timestamp.from(Instant.now(clock));
        jdbcTemplate.update("""
                insert into workspace_identity_provider
                (provider_id, organization_id, provider_type, display_name, status, fallback_mode, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (provider_id) do nothing
                """,
                organizationId + ".local-header",
                organizationId,
                "local_header",
                "Local header demo session",
                "active",
                "sandbox_safe",
                now,
                now
        );
        jdbcTemplate.update("""
                insert into workspace_identity_provider
                (provider_id, organization_id, provider_type, display_name, status, fallback_mode, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (provider_id) do nothing
                """,
                organizationId + ".trusted-proxy",
                organizationId,
                "trusted_proxy",
                "Trusted SSO / reverse proxy bridge",
                "planned",
                "fall_back_to_local_header_for_demo",
                now,
                now
        );
        if (authProperties.getGroupRoleMappings().isEmpty()) {
            insertGroupMapping(organizationId, organizationId + ".trusted-proxy", "reviewers", "reviewer", "Maps enterprise review groups into HealthForge reviewer access.");
            insertGroupMapping(organizationId, organizationId + ".trusted-proxy", "approvers", "approver", "Maps enterprise approver groups into approval and export workflows.");
            insertGroupMapping(organizationId, organizationId + ".trusted-proxy", "auditors", "auditor", "Maps enterprise audit teams into oversight-only reporting views.");
            insertGroupMapping(organizationId, organizationId + ".trusted-proxy", "administrators", "administrator", "Maps enterprise platform operators into administration and access review functions.");
            return;
        }
        for (var mapping : authProperties.getGroupRoleMappings()) {
            insertGroupMapping(
                    organizationId,
                    organizationId + ".trusted-proxy",
                    mapping.getGroup(),
                    mapping.getRole(),
                    mapping.getScope() == null || mapping.getScope().isBlank()
                            ? "Configured group-to-role mapping for the trusted proxy identity path."
                            : mapping.getScope()
            );
        }
    }

    private void insertGroupMapping(String organizationId, String providerId, String groupName, String role, String scope) {
        jdbcTemplate.update("""
                insert into workspace_group_role_mapping
                (mapping_id, provider_id, organization_id, group_name, actor_role, scope_summary, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (provider_id, group_name, actor_role) do nothing
                """,
                "mapping_" + providerId + "." + groupName + "." + role,
                providerId,
                organizationId,
                groupName,
                role,
                scope,
                Timestamp.from(Instant.now(clock)),
                Timestamp.from(Instant.now(clock))
        );
    }

    private void seedProjects(AuthenticatedActor actor) {
        var existing = jdbcTemplate.queryForObject("""
                select count(*) from workspace_project where organization_id = ?
                """, Integer.class, actor.organizationId());
        if (existing != null && existing > 0) {
            return;
        }
        var now = Timestamp.from(Instant.now(clock));
        createSeedProject(actor.organizationId(), "program-cms-prior-auth", "CMS Prior Authorization Modernization", "program",
                "Group briefs, exports, and evidence for the CMS prior authorization initiative.", actor.actorId(),
                "cms,prior-auth,interoperability", now);
        createSeedProject(actor.organizationId(), "workspace-evaluator-demo", "Enterprise Evaluator Workspace", "workspace",
                "Reusable showcase workspace for enterprise review, audit, and demo flows.", actor.actorId(),
                "demo,evaluation,governance", now);
        var firstProjectId = actor.organizationId() + ".program-cms-prior-auth";
        jdbcTemplate.update("""
                insert into workspace_project_brief (project_id, brief_id, linked_at)
                select ?, brief_id, ?
                from engineering_brief
                where organization_id = ?
                on conflict do nothing
                """, firstProjectId, now, actor.organizationId());
        jdbcTemplate.update("""
                insert into evidence_collection (collection_id, organization_id, project_id, name, summary, source_count, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (collection_id) do nothing
                """,
                actor.organizationId() + ".collection.cms",
                actor.organizationId(),
                actor.organizationId() + ".program-cms-prior-auth",
                "CMS prior auth evidence collection",
                "Saved evidence workspace for CMS prior auth findings and linked artifacts.",
                0,
                now,
                now,
                actor.organizationId() + ".collection.demo",
                actor.organizationId(),
                actor.organizationId() + ".workspace-evaluator-demo",
                "Evaluator workspace collection",
                "Saved demo-safe evidence workspace for enterprise evaluators and operators.",
                0,
                now,
                now
        );
    }

    private void createSeedProject(String organizationId, String shortId, String name, String kind, String description, String ownerActorId, String tags, Timestamp now) {
        jdbcTemplate.update("""
                insert into workspace_project (project_id, organization_id, name, kind, description, owner_actor_id, tags, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (project_id) do nothing
                """,
                organizationId + "." + shortId,
                organizationId,
                name,
                kind,
                description,
                ownerActorId,
                tags,
                now,
                now
        );
    }

    private void seedWorkflowConfigurations(AuthenticatedActor actor) {
        var existing = jdbcTemplate.queryForObject("""
                select count(*) from workflow_configuration where organization_id = ?
                """, Integer.class, actor.organizationId());
        if (existing != null && existing > 0) {
            return;
        }
        var now = Timestamp.from(Instant.now(clock));
        insertWorkflowConfiguration(actor.organizationId(), "brief_flow", "CMS prior auth reviewer brief", "v1", "active",
                "Default prompt and retrieval posture for cited, human-reviewable engineering briefs.",
                "grounded-brief-v1", "expanded-web-core-v4", "brief-review-approval", now);
        insertWorkflowConfiguration(actor.organizationId(), "review_queue", "Approver handoff workflow", "v1", "active",
                "Named configuration for approval gating, audit export checks, and work-item readiness.",
                "approval-handoff-v1", "brief-source-citations", "approval-export-governance", now);
        insertWorkflowConfiguration(actor.organizationId(), "admin_console", "Enterprise evaluator walkthrough", "v1", "active",
                "Reusable operator flow for identity, access review, evaluation, and deployment posture walkthroughs.",
                "operator-narrative-v1", "compliance-eval-metrics", "admin-console-tour", now);
    }

    private void insertWorkflowConfiguration(String organizationId, String type, String name, String version, String status,
                                             String summary, String promptProfile, String retrievalProfile, String workflowProfile, Timestamp now) {
        jdbcTemplate.update("""
                insert into workflow_configuration
                (config_id, organization_id, config_type, name, version_label, status, summary, prompt_profile, retrieval_profile, workflow_profile, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (config_id) do nothing
                """,
                organizationId + "." + type + "." + version,
                organizationId,
                type,
                name,
                version,
                status,
                summary,
                promptProfile,
                retrievalProfile,
                workflowProfile,
                now,
                now
        );
    }

    private void seedSavedViews(AuthenticatedActor actor) {
        var existing = jdbcTemplate.queryForObject("""
                select count(*) from workspace_saved_view where organization_id = ?
                """, Integer.class, actor.organizationId());
        if (existing != null && existing > 0) {
            return;
        }
        var now = Timestamp.from(Instant.now(clock));
        insertSavedView(actor.organizationId(), actor.organizationId() + ".program-cms-prior-auth", "brief_queue", "Needs reviewer action",
                "status in (draft, changes_requested)", "Quickly reopen briefs that still need reviewer attention.", now);
        insertSavedView(actor.organizationId(), actor.organizationId() + ".workspace-evaluator-demo", "oversight", "Approved and export-ready",
                "status = approved", "Saved view for approved briefs that are ready for governance-aware export discussion.", now);
        insertSavedView(actor.organizationId(), null, "evidence", "Public regulatory evidence",
                "source_type in (governing_regulation, authoritative_implementation_guidance)", "Reusable evidence view for grounded public-source research.", now);
    }

    private void insertSavedView(String organizationId, String projectId, String viewType, String name, String queryText, String summary, Timestamp now) {
        jdbcTemplate.update("""
                insert into workspace_saved_view
                (view_id, organization_id, project_id, view_type, name, query_text, summary, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (view_id) do nothing
                """,
                organizationId + "." + name.toLowerCase(Locale.ROOT).replace(" ", "-"),
                organizationId,
                projectId,
                viewType,
                name,
                queryText,
                summary,
                now,
                now
        );
    }

    private void seedAssignments(AuthenticatedActor actor) {
        var briefs = jdbcTemplate.query("""
                select brief_id, status from engineering_brief where organization_id = ? order by created_at desc
                """, (rs, row) -> Map.of(
                "brief_id", rs.getString("brief_id"),
                "status", rs.getString("status")
        ), actor.organizationId());
        for (var brief : briefs) {
            var count = jdbcTemplate.queryForObject("""
                    select count(*) from workspace_assignment where brief_id = ? and organization_id = ?
                    """, Integer.class, brief.get("brief_id"), actor.organizationId());
            if (count != null && count > 0) {
                continue;
            }
            var status = String.valueOf(brief.get("status"));
            var assignmentRole = switch (status) {
                case "approved" -> "administrator";
                case "in_review" -> "approver";
                default -> "reviewer";
            };
            var queueName = switch (status) {
                case "approved" -> "approved-artifacts";
                case "in_review" -> "approver-queue";
                case "changes_requested" -> "changes-requested";
                default -> "reviewer-queue";
            };
            var assigneeActorId = actor.organizationId() + "." + assignmentRole;
            var note = switch (status) {
                case "approved" -> "Approved brief ready for export/audit discussion.";
                case "in_review" -> "Brief has accepted findings and is waiting for approver review.";
                case "changes_requested" -> "Brief needs reviewer follow-up before approval.";
                default -> "New or draft brief waiting for reviewer triage.";
            };
            createAssignment(new WorkspaceAssignmentRequest(String.valueOf(brief.get("brief_id")), assigneeActorId, assignmentRole, queueName, note), actor);
        }
    }

    private void seedResearchPacks(AuthenticatedActor actor) {
        var existing = jdbcTemplate.queryForObject("""
                select count(*) from workspace_research_pack where organization_id = ?
                """, Integer.class, actor.organizationId());
        if (existing != null && existing > 0) {
            return;
        }
        var now = Timestamp.from(Instant.now(clock));
        insertResearchPack(
                actor.organizationId(),
                actor.organizationId() + ".research.cms-prior-auth",
                actor.organizationId() + ".program-cms-prior-auth",
                "CMS prior auth evidence pack",
                "Reusable analyst pack for recurring prior-authorization planning and evidence review.",
                String.join("\n",
                        "What changes do we need for CMS prior authorization workflows?",
                        "How should a provider workflow handle documentation and status exchange for prior authorization?",
                        "What evidence-quality and approval signals should an enterprise evaluator inspect?"),
                now.toInstant().plus(14, ChronoUnit.DAYS),
                now
        );
        insertResearchPack(
                actor.organizationId(),
                actor.organizationId() + ".research.enterprise-eval",
                actor.organizationId() + ".workspace-evaluator-demo",
                "Enterprise evaluator readiness pack",
                "Saved questions for repeated readiness, trust, and operator walkthroughs.",
                String.join("\n",
                        "What trust signals should an enterprise evaluator inspect before approving planning outputs?",
                        "How do source freshness and evidence sufficiency affect readiness conversations?"),
                now.toInstant().plus(21, ChronoUnit.DAYS),
                now
        );
    }

    private void seedQuestionPacks(AuthenticatedActor actor) {
        var existing = jdbcTemplate.queryForObject("""
                select count(*) from workspace_question_pack where organization_id = ?
                """, Integer.class, actor.organizationId());
        if (existing != null && existing > 0) {
            return;
        }
        var now = Timestamp.from(Instant.now(clock));
        insertQuestionPack(
                actor.organizationId(),
                actor.organizationId() + ".question-pack.reviewer-prior-auth",
                actor.organizationId() + ".program-cms-prior-auth",
                "Reviewer prior-auth starter pack",
                "Reusable reviewer prompts for CMS prior authorization research and evidence evaluation.",
                "reviewer",
                "analyst_starter",
                "What changes do we need for CMS prior authorization workflows?",
                String.join("\n",
                        "What changes do we need for CMS prior authorization workflows?",
                        "Which passages directly affect documentation and status exchange?",
                        "Where is evidence still too weak for a Brief?"),
                now
        );
        insertQuestionPack(
                actor.organizationId(),
                actor.organizationId() + ".question-pack.evaluator-trust",
                actor.organizationId() + ".workspace-evaluator-demo",
                "Evaluator trust and readiness pack",
                "Operator-ready prompts for evidence sufficiency, governance, and oversight walkthroughs.",
                "auditor",
                "stakeholder_walkthrough",
                "What evidence-quality and approval signals should an enterprise evaluator inspect?",
                String.join("\n",
                        "What evidence-quality and approval signals should an enterprise evaluator inspect?",
                        "Which approved Briefs are strongest for pilot conversations?",
                        "Where are stale sources likely to change the story?"),
                now
        );
    }

    private void insertQuestionPack(
            String organizationId,
            String questionPackId,
            String projectId,
            String name,
            String summary,
            String persona,
            String templateKind,
            String starterQuestion,
            String questionPrompts,
            Timestamp now
    ) {
        jdbcTemplate.update("""
                insert into workspace_question_pack
                (question_pack_id, organization_id, project_id, name, summary, persona, template_kind, starter_question, question_prompts, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (question_pack_id) do nothing
                """,
                questionPackId,
                organizationId,
                projectId,
                name,
                summary,
                persona,
                templateKind,
                starterQuestion,
                questionPrompts,
                now,
                now
        );
    }

    private void seedResearchNotebooks(AuthenticatedActor actor) {
        var existing = jdbcTemplate.queryForObject("""
                select count(*) from workspace_research_notebook where organization_id = ?
                """, Integer.class, actor.organizationId());
        if (existing != null && existing > 0) {
            return;
        }
        var brief = latestBrief(actor.organizationId());
        var now = Timestamp.from(Instant.now(clock));
        insertResearchNotebook(
                actor.organizationId(),
                actor.organizationId() + ".notebook.prior-auth-handoff",
                actor.organizationId() + ".program-cms-prior-auth",
                brief == null ? null : brief.briefId(),
                "Prior auth reviewer notebook",
                "Bounded notebook for preserving evidence takeaways before approval.",
                String.join("\n",
                        "Capture the governing CMS requirement before interpreting workflow impact.",
                        "Separate provider workflow changes from payer decision/status assumptions.",
                        "Flag stale evidence before presenting the Brief to an approver."),
                "CMS prior auth evidence bundle",
                "Use this notebook to hand the reviewer interpretation and unresolved evidence questions to the approver.",
                "Refresh when CMS rule or implementation-guide evidence changes.",
                now
        );
    }

    private void insertResearchNotebook(
            String organizationId,
            String notebookId,
            String projectId,
            String briefId,
            String title,
            String summary,
            String keyTakeaways,
            String evidenceBundleName,
            String handoffSummary,
            String continuityNote,
            Timestamp now
    ) {
        jdbcTemplate.update("""
                insert into workspace_research_notebook
                (notebook_id, organization_id, project_id, brief_id, title, summary, key_takeaways, evidence_bundle_name, handoff_summary, continuity_note, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (notebook_id) do nothing
                """,
                notebookId,
                organizationId,
                projectId,
                briefId,
                title,
                summary,
                keyTakeaways,
                evidenceBundleName,
                handoffSummary,
                continuityNote,
                now,
                now
        );
    }

    private void seedReviewEscalations(AuthenticatedActor actor) {
        var existing = jdbcTemplate.queryForObject("""
                select count(*) from workspace_review_escalation where organization_id = ?
                """, Integer.class, actor.organizationId());
        if (existing != null && existing > 0) {
            return;
        }
        var assignments = assignments(actor.organizationId());
        if (assignments.isEmpty()) {
            return;
        }
        var candidate = assignments.stream()
                .filter(item -> "draft".equals(item.briefStatus()) || "changes_requested".equals(item.briefStatus()))
                .findFirst()
                .orElse(assignments.getFirst());
        var now = Timestamp.from(Instant.now(clock));
        jdbcTemplate.update("""
                insert into workspace_review_escalation
                (escalation_id, organization_id, assignment_id, brief_id, escalation_reason, urgency, destination_queue, status, note, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (escalation_id) do nothing
                """,
                actor.organizationId() + ".escalation.reviewer-follow-up",
                actor.organizationId(),
                candidate.assignmentId(),
                candidate.briefId(),
                "Evidence questions need reviewer follow-up before approver time is spent.",
                "medium",
                "reviewer-queue",
                "open",
                "Use escalation cues to surface aging research without turning this into a punitive ticketing queue.",
                now,
                now
        );
    }

    private void insertResearchPack(
            String organizationId,
            String researchPackId,
            String projectId,
            String name,
            String summary,
            String recurringQuestions,
            Instant nextReviewAt,
            Timestamp now
    ) {
        jdbcTemplate.update("""
                insert into workspace_research_pack
                (research_pack_id, organization_id, project_id, name, summary, recurring_questions, question_count, next_review_at, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (research_pack_id) do nothing
                """,
                researchPackId,
                organizationId,
                projectId,
                name,
                summary,
                recurringQuestions,
                parseRecurringQuestions(recurringQuestions).size(),
                nextReviewAt == null ? null : Timestamp.from(nextReviewAt),
                now,
                now
        );
    }

    private WorkspaceOverviewResponse.AuthFoundation authFoundation(String organizationId) {
        var providers = jdbcTemplate.query("""
                select provider_id, provider_type, display_name, status, fallback_mode
                from workspace_identity_provider
                where organization_id = ?
                order by provider_type
                """, (rs, row) -> new WorkspaceOverviewResponse.IdentityProviderSummary(
                rs.getString("provider_id"),
                rs.getString("provider_type"),
                rs.getString("display_name"),
                rs.getString("status"),
                rs.getString("fallback_mode")
        ), organizationId);
        var groupMappings = jdbcTemplate.query("""
                select mapping_id, provider_id, group_name, actor_role, scope_summary
                from workspace_group_role_mapping
                where organization_id = ?
                order by provider_id, actor_role, group_name
                """, (rs, row) -> new WorkspaceOverviewResponse.GroupRoleMappingSummary(
                rs.getString("mapping_id"),
                rs.getString("provider_id"),
                rs.getString("group_name"),
                rs.getString("actor_role"),
                rs.getString("scope_summary")
        ), organizationId);
        return new WorkspaceOverviewResponse.AuthFoundation(
                authProperties.getMode(),
                authProperties.getMode().equals("trusted_proxy")
                        ? "Enterprise SSO mode reads identity from trusted proxy headers and maps groups into HealthForge roles."
                        : "Sandbox/demo mode uses explicit local headers and stays separate from the future enterprise SSO path.",
                List.of("local_header", "trusted_proxy"),
                providers,
                groupMappings
        );
    }

    private List<WorkspaceOverviewResponse.ProjectSummary> projects(String organizationId) {
        var briefsByProject = new LinkedHashMap<String, List<String>>();
        jdbcTemplate.query("""
                select project_id, brief_id from workspace_project_brief
                where project_id in (select project_id from workspace_project where organization_id = ?)
                order by linked_at desc
                """, rs -> {
            briefsByProject.computeIfAbsent(rs.getString("project_id"), ignored -> new java.util.ArrayList<>())
                    .add(rs.getString("brief_id"));
        }, organizationId);
        return jdbcTemplate.query("""
                select project_id, name, kind, description, owner_actor_id, tags, updated_at
                from workspace_project
                where organization_id = ?
                order by updated_at desc, name
                """, (rs, row) -> new WorkspaceOverviewResponse.ProjectSummary(
                rs.getString("project_id"),
                rs.getString("name"),
                rs.getString("kind"),
                rs.getString("description"),
                rs.getString("owner_actor_id"),
                parseTags(rs.getString("tags")),
                briefsByProject.getOrDefault(rs.getString("project_id"), List.of()),
                briefsByProject.getOrDefault(rs.getString("project_id"), List.of()).size(),
                rs.getTimestamp("updated_at").toInstant()
        ), organizationId);
    }

    private WorkspaceOverviewResponse.ProjectSummary project(String organizationId, String projectId) {
        return projects(organizationId).stream()
                .filter(item -> item.projectId().equals(projectId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project was not found in the current organization."));
    }

    private List<WorkspaceOverviewResponse.QueueSummary> queues(String organizationId) {
        return jdbcTemplate.query("""
                select queue_name,
                       count(*) as total_assignments,
                       sum(case when b.status = 'draft' then 1 else 0 end) as draft_briefs,
                       sum(case when b.status = 'in_review' then 1 else 0 end) as in_review_briefs,
                       sum(case when b.status = 'changes_requested' then 1 else 0 end) as changes_requested_briefs,
                       sum(case when b.status = 'approved' then 1 else 0 end) as approved_briefs
                from workspace_assignment a
                join engineering_brief b on b.brief_id = a.brief_id and b.organization_id = a.organization_id
                where a.organization_id = ?
                group by queue_name
                order by queue_name
                """, (rs, row) -> new WorkspaceOverviewResponse.QueueSummary(
                rs.getString("queue_name"),
                rs.getInt("total_assignments"),
                rs.getInt("draft_briefs"),
                rs.getInt("in_review_briefs"),
                rs.getInt("changes_requested_briefs"),
                rs.getInt("approved_briefs")
        ), organizationId);
    }

    private List<WorkspaceOverviewResponse.AssignmentSummary> assignments(String organizationId) {
        return jdbcTemplate.query("""
                select a.assignment_id, a.brief_id, b.question, b.status as brief_status, a.assignee_actor_id, a.assignee_role, a.queue_name, a.status, a.handoff_summary, a.updated_at
                from workspace_assignment a
                join engineering_brief b on b.brief_id = a.brief_id and b.organization_id = a.organization_id
                where a.organization_id = ?
                order by a.updated_at desc, a.queue_name
                """, (rs, row) -> new WorkspaceOverviewResponse.AssignmentSummary(
                rs.getString("assignment_id"),
                rs.getString("brief_id"),
                rs.getString("question"),
                rs.getString("brief_status"),
                rs.getString("assignee_actor_id"),
                rs.getString("assignee_role"),
                rs.getString("queue_name"),
                rs.getString("status"),
                rs.getString("handoff_summary"),
                rs.getTimestamp("updated_at").toInstant()
        ), organizationId);
    }

    private List<WorkspaceOverviewResponse.WorkflowConfigurationSummary> workflowConfigurations(String organizationId) {
        return jdbcTemplate.query("""
                select config_id, config_type, name, version_label, status, summary, prompt_profile, retrieval_profile, workflow_profile, updated_at
                from workflow_configuration
                where organization_id = ?
                order by updated_at desc, name
                """, (rs, row) -> new WorkspaceOverviewResponse.WorkflowConfigurationSummary(
                rs.getString("config_id"),
                rs.getString("config_type"),
                rs.getString("name"),
                rs.getString("version_label"),
                rs.getString("status"),
                rs.getString("summary"),
                rs.getString("prompt_profile"),
                rs.getString("retrieval_profile"),
                rs.getString("workflow_profile"),
                rs.getTimestamp("updated_at").toInstant()
        ), organizationId);
    }

    private List<WorkspaceOverviewResponse.SavedViewSummary> savedViews(String organizationId) {
        return jdbcTemplate.query("""
                select v.view_id, v.project_id, p.name as project_name, v.view_type, v.name, v.query_text, v.summary, v.updated_at
                from workspace_saved_view v
                left join workspace_project p on p.project_id = v.project_id
                where v.organization_id = ?
                order by v.updated_at desc, v.name
                """, (rs, row) -> new WorkspaceOverviewResponse.SavedViewSummary(
                rs.getString("view_id"),
                rs.getString("project_id"),
                rs.getString("project_name"),
                rs.getString("view_type"),
                rs.getString("name"),
                rs.getString("query_text"),
                rs.getString("summary"),
                rs.getTimestamp("updated_at").toInstant()
        ), organizationId);
    }

    private List<WorkspaceOverviewResponse.EvidenceCollectionSummary> evidenceCollections(String organizationId) {
        return jdbcTemplate.query("""
                select c.collection_id, c.project_id, p.name as project_name, c.name, c.summary, c.source_count, c.updated_at
                from evidence_collection c
                left join workspace_project p on p.project_id = c.project_id
                where c.organization_id = ?
                order by c.updated_at desc, c.name
                """, (rs, row) -> new WorkspaceOverviewResponse.EvidenceCollectionSummary(
                rs.getString("collection_id"),
                rs.getString("project_id"),
                rs.getString("project_name"),
                rs.getString("name"),
                rs.getString("summary"),
                rs.getInt("source_count"),
                rs.getTimestamp("updated_at").toInstant()
        ), organizationId);
    }

    private List<WorkspaceOverviewResponse.ResearchPackSummary> researchPacks(String organizationId) {
        return jdbcTemplate.query("""
                select rp.research_pack_id, rp.project_id, p.name as project_name, rp.name, rp.summary,
                       rp.recurring_questions, rp.question_count, rp.next_review_at, rp.updated_at
                from workspace_research_pack rp
                left join workspace_project p on p.project_id = rp.project_id
                where rp.organization_id = ?
                order by rp.updated_at desc, rp.name
                """, (rs, row) -> new WorkspaceOverviewResponse.ResearchPackSummary(
                rs.getString("research_pack_id"),
                rs.getString("project_id"),
                rs.getString("project_name"),
                rs.getString("name"),
                rs.getString("summary"),
                rs.getInt("question_count"),
                parseRecurringQuestions(rs.getString("recurring_questions")),
                rs.getTimestamp("next_review_at") == null ? null : rs.getTimestamp("next_review_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ), organizationId);
    }

    private List<WorkspaceOverviewResponse.QuestionPackSummary> questionPacks(String organizationId) {
        return jdbcTemplate.query("""
                select qp.question_pack_id, qp.project_id, p.name as project_name, qp.name, qp.summary,
                       qp.persona, qp.template_kind, qp.starter_question, qp.question_prompts, qp.updated_at
                from workspace_question_pack qp
                left join workspace_project p on p.project_id = qp.project_id
                where qp.organization_id = ?
                order by qp.updated_at desc, qp.name
                """, (rs, row) -> new WorkspaceOverviewResponse.QuestionPackSummary(
                rs.getString("question_pack_id"),
                rs.getString("project_id"),
                rs.getString("project_name"),
                rs.getString("name"),
                rs.getString("summary"),
                rs.getString("persona"),
                rs.getString("template_kind"),
                rs.getString("starter_question"),
                parseLineList(rs.getString("question_prompts")),
                rs.getTimestamp("updated_at").toInstant()
        ), organizationId);
    }

    private List<WorkspaceOverviewResponse.ScenarioTemplateSummary> scenarioTemplates(String organizationId) {
        return questionPacks(organizationId).stream()
                .map(pack -> new WorkspaceOverviewResponse.ScenarioTemplateSummary(
                        "template_" + pack.questionPackId(),
                        pack.name(),
                        pack.persona(),
                        pack.templateKind(),
                        pack.summary(),
                        pack.questionPrompts().isEmpty()
                                ? List.of(pack.starterQuestion())
                                : pack.questionPrompts().stream().limit(3).toList()
                ))
                .toList();
    }

    private List<WorkspaceOverviewResponse.PersonaPresetSummary> personaPresets() {
        return List.of(
                new WorkspaceOverviewResponse.PersonaPresetSummary(
                        "preset_reviewer",
                        "reviewer",
                        "reviewer",
                        "brief_studio",
                        "Best for turning grounded evidence into a reviewable Brief with explicit findings and decisions.",
                        List.of("evidence sufficiency", "finding review", "research continuity")
                ),
                new WorkspaceOverviewResponse.PersonaPresetSummary(
                        "preset_approver",
                        "approver",
                        "approver",
                        "team_workspace",
                        "Best for comparing prior reviewed work, handoffs, and approval-ready evidence bundles.",
                        List.of("precedent comparison", "handoff summaries", "approval readiness")
                ),
                new WorkspaceOverviewResponse.PersonaPresetSummary(
                        "preset_auditor",
                        "auditor",
                        "auditor",
                        "admin_console",
                        "Best for inspecting evidence freshness, governance patterns, and pilot-readiness narratives.",
                        List.of("answer quality", "policy signals", "oversight reporting")
                )
        );
    }

    private List<WorkspaceOverviewResponse.PrecedentComparisonSummary> precedentComparisons(String organizationId) {
        var briefs = jdbcTemplate.query("""
                select brief_id, question, status
                from engineering_brief
                where organization_id = ?
                order by created_at desc
                limit 8
                """, (rs, row) -> new BriefLite(rs.getString("brief_id"), rs.getString("question"), rs.getString("status")), organizationId);
        var comparisons = new ArrayList<WorkspaceOverviewResponse.PrecedentComparisonSummary>();
        for (int i = 0; i < briefs.size(); i++) {
            for (int j = i + 1; j < briefs.size(); j++) {
                var left = briefs.get(i);
                var right = briefs.get(j);
                var sharedSignals = sharedSignals(left.question(), right.question());
                if (!sharedSignals.isEmpty()) {
                    comparisons.add(new WorkspaceOverviewResponse.PrecedentComparisonSummary(
                            "comparison_" + left.briefId() + "_" + right.briefId(),
                            left.briefId(),
                            left.question(),
                            right.briefId(),
                            right.question(),
                            dominantTheme(left.question() + " " + right.question()),
                            sharedSignals,
                            "Use this as advisory precedent only; the current Brief still needs its own citations and review decisions."
                    ));
                }
            }
        }
        if (comparisons.isEmpty() && !briefs.isEmpty()) {
            var first = briefs.getFirst();
            comparisons.add(new WorkspaceOverviewResponse.PrecedentComparisonSummary(
                    "comparison_seed_" + first.briefId(),
                    first.briefId(),
                    first.question(),
                    first.briefId(),
                    first.question(),
                    dominantTheme(first.question()),
                    List.of("No related Brief overlap is visible yet."),
                    "Create more approved Briefs in a shared theme to unlock stronger precedent comparisons."
            ));
        }
        return comparisons.stream().limit(4).toList();
    }

    private List<WorkspaceOverviewResponse.DecisionPatternSummary> decisionPatterns(String organizationId) {
        var counts = jdbcTemplate.query("""
                select decision, count(*) as total
                from brief_review_decision
                where organization_id = ?
                group by decision
                order by total desc, decision
                """, (rs, row) -> Map.entry(rs.getString("decision"), rs.getInt("total")), organizationId);
        if (counts.isEmpty()) {
            return List.of(new WorkspaceOverviewResponse.DecisionPatternSummary(
                    "pattern_seed",
                    "Review patterns will appear as decisions accumulate",
                    "seed",
                    "Capture more reviewer decisions to expose reusable approval and correction patterns.",
                    List.of("No review decisions recorded yet.")
            ));
        }
        return counts.stream().map(entry -> new WorkspaceOverviewResponse.DecisionPatternSummary(
                "pattern_" + entry.getKey(),
                entry.getKey().replace('_', ' '),
                "review_decision",
                switch (entry.getKey()) {
                    case "accept" -> "Accepted findings show where evidence is already strong enough for reuse.";
                    case "correct" -> "Corrections show where analyst interpretation needs more structure before approval.";
                    case "needs_information" -> "Needs-information decisions reveal recurring evidence gaps and review friction.";
                    default -> "Rejected findings help identify weak or misleading answer paths.";
                },
                List.of("decision count: " + entry.getValue())
        )).toList();
    }

    private List<WorkspaceOverviewResponse.ThemeClusterSummary> themeClusters(String organizationId) {
        var briefs = jdbcTemplate.query("""
                select brief_id, question, status
                from engineering_brief
                where organization_id = ?
                order by created_at desc
                """, (rs, row) -> new BriefLite(rs.getString("brief_id"), rs.getString("question"), rs.getString("status")), organizationId);
        var buckets = new LinkedHashMap<String, List<BriefLite>>();
        for (var brief : briefs) {
            buckets.computeIfAbsent(dominantTheme(brief.question()), ignored -> new ArrayList<>()).add(brief);
        }
        if (buckets.isEmpty()) {
            return List.of();
        }
        return buckets.entrySet().stream()
                .map(entry -> new WorkspaceOverviewResponse.ThemeClusterSummary(
                        "cluster_" + entry.getKey().replace(' ', '_'),
                        entry.getKey(),
                        entry.getValue().size(),
                        (int) entry.getValue().stream().filter(item -> "approved".equals(item.status())).count(),
                        entry.getValue().stream().map(BriefLite::question).limit(3).toList(),
                        "Cluster related work by theme so analysts can navigate by topic instead of isolated Briefs."
                ))
                .sorted((left, right) -> Integer.compare(right.briefCount(), left.briefCount()))
                .toList();
    }

    private List<WorkspaceOverviewResponse.ResearchNotebookSummary> researchNotebooks(String organizationId) {
        return jdbcTemplate.query("""
                select n.notebook_id, n.project_id, p.name as project_name, n.brief_id, b.question as brief_question,
                       n.title, n.summary, n.key_takeaways, n.evidence_bundle_name, n.handoff_summary, n.continuity_note, n.updated_at
                from workspace_research_notebook n
                left join workspace_project p on p.project_id = n.project_id
                left join engineering_brief b on b.brief_id = n.brief_id and b.organization_id = n.organization_id
                where n.organization_id = ?
                order by n.updated_at desc, n.title
                """, (rs, row) -> new WorkspaceOverviewResponse.ResearchNotebookSummary(
                rs.getString("notebook_id"),
                rs.getString("project_id"),
                rs.getString("project_name"),
                rs.getString("brief_id"),
                rs.getString("brief_question"),
                rs.getString("title"),
                rs.getString("summary"),
                parseLineList(rs.getString("key_takeaways")),
                rs.getString("evidence_bundle_name"),
                rs.getString("handoff_summary"),
                rs.getString("continuity_note"),
                rs.getTimestamp("updated_at").toInstant()
        ), organizationId);
    }

    private List<WorkspaceOverviewResponse.TopicSummary> topicBrowser(String organizationId) {
        return themeClusters(organizationId).stream()
                .map(cluster -> new WorkspaceOverviewResponse.TopicSummary(
                        "topic_" + cluster.clusterId(),
                        cluster.theme(),
                        cluster.briefCount(),
                        relatedFindingCount(organizationId, cluster.theme()),
                        cluster.approvedCount(),
                        "Browse " + cluster.theme() + " work across questions, findings, and approvals."
                ))
                .toList();
    }

    private WorkspaceOverviewResponse.ReviewerOperationsSummary reviewerOperations(String organizationId) {
        var assignments = assignments(organizationId);
        var escalations = escalations(organizationId);
        var slaCues = assignments.stream()
                .map(item -> {
                    var ageDays = Math.max(0, ChronoUnit.DAYS.between(item.updatedAt(), Instant.now(clock)));
                    var urgency = ageDays >= 5 ? "high" : ageDays >= 3 ? "medium" : "low";
                    var recommendation = ageDays >= 5
                            ? "Escalate or reassign this Brief so reviewer work does not stall."
                            : ageDays >= 3
                            ? "Review soon to avoid a stale queue."
                            : "Within target reviewer flow.";
                    return new WorkspaceOverviewResponse.SlaCueSummary(
                            item.assignmentId(),
                            item.briefId(),
                            item.briefQuestion(),
                            item.queueName(),
                            item.assigneeActorId(),
                            ageDays,
                            urgency,
                            recommendation
                    );
                })
                .sorted((left, right) -> Long.compare(right.ageDays(), left.ageDays()))
                .limit(6)
                .toList();
        return new WorkspaceOverviewResponse.ReviewerOperationsSummary(
                assignments.size(),
                (int) assignments.stream().filter(item -> ChronoUnit.DAYS.between(item.updatedAt(), Instant.now(clock)) >= 3).count(),
                (int) assignments.stream().filter(item -> ChronoUnit.DAYS.between(item.updatedAt(), Instant.now(clock)) >= 5).count(),
                escalations.size(),
                slaCues,
                escalations
        );
    }

    private List<WorkspaceOverviewResponse.EscalationSummary> escalations(String organizationId) {
        return jdbcTemplate.query("""
                select e.escalation_id, e.assignment_id, e.brief_id, b.question as brief_question,
                       e.escalation_reason, e.urgency, e.destination_queue, e.status, e.note, e.updated_at
                from workspace_review_escalation e
                join engineering_brief b on b.brief_id = e.brief_id and b.organization_id = e.organization_id
                where e.organization_id = ?
                order by e.updated_at desc
                """, (rs, row) -> new WorkspaceOverviewResponse.EscalationSummary(
                rs.getString("escalation_id"),
                rs.getString("assignment_id"),
                rs.getString("brief_id"),
                rs.getString("brief_question"),
                rs.getString("escalation_reason"),
                rs.getString("urgency"),
                rs.getString("destination_queue"),
                rs.getString("status"),
                rs.getString("note"),
                rs.getTimestamp("updated_at").toInstant()
        ), organizationId);
    }

    private void refreshAllEvidenceCollections(String organizationId) {
        var projectIds = jdbcTemplate.query("""
                select project_id from workspace_project where organization_id = ?
                """, (rs, row) -> rs.getString("project_id"), organizationId);
        projectIds.forEach(projectId -> refreshEvidenceCollectionCounts(projectId, organizationId));
    }

    private void refreshEvidenceCollectionCounts(String projectId, String organizationId) {
        var sourceCount = jdbcTemplate.queryForObject("""
                select count(distinct concat(s.source_id, '|', s.source_version))
                from workspace_project_brief pb
                join engineering_brief b on b.brief_id = pb.brief_id and b.organization_id = ?
                join brief_source s on s.brief_id = b.brief_id
                where pb.project_id = ?
                """, Integer.class, organizationId, projectId);
        jdbcTemplate.update("""
                update evidence_collection
                set source_count = ?, updated_at = ?
                where project_id = ? and organization_id = ?
                """,
                sourceCount == null ? 0 : sourceCount,
                Timestamp.from(Instant.now(clock)),
                projectId,
                organizationId
        );
    }

    private void ensureProject(String projectId, String organizationId) {
        var count = jdbcTemplate.queryForObject("""
                select count(*) from workspace_project where project_id = ? and organization_id = ?
                """, Integer.class, projectId, organizationId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project was not found in the current organization.");
        }
    }

    private void ensureBrief(String briefId, String organizationId) {
        var count = jdbcTemplate.queryForObject("""
                select count(*) from engineering_brief where brief_id = ? and organization_id = ?
                """, Integer.class, briefId, organizationId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Brief was not found in the current organization.");
        }
    }

    private void ensureAssignment(String assignmentId, String organizationId) {
        var count = jdbcTemplate.queryForObject("""
                select count(*) from workspace_assignment where assignment_id = ? and organization_id = ?
                """, Integer.class, assignmentId, organizationId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment was not found in the current organization.");
        }
    }

    private String normalizeTags(String tags) {
        return Arrays.stream((tags == null ? "" : tags).split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String normalizeRecurringQuestions(String recurringQuestions) {
        return Arrays.stream((recurringQuestions == null ? "" : recurringQuestions).split("\\r?\\n"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private List<String> parseRecurringQuestions(String recurringQuestions) {
        if (recurringQuestions == null || recurringQuestions.isBlank()) {
            return List.of();
        }
        return Arrays.stream(recurringQuestions.split("\\r?\\n"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String normalizeLineList(String value) {
        return Arrays.stream((value == null ? "" : value).split("\\r?\\n"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private List<String> parseLineList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\r?\\n"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private BriefLite latestBrief(String organizationId) {
        var briefs = jdbcTemplate.query("""
                select brief_id, question, status
                from engineering_brief
                where organization_id = ?
                order by created_at desc
                limit 1
                """, (rs, row) -> new BriefLite(rs.getString("brief_id"), rs.getString("question"), rs.getString("status")), organizationId);
        return briefs.isEmpty() ? null : briefs.getFirst();
    }

    private List<String> sharedSignals(String left, String right) {
        var leftWords = significantTokens(left);
        var rightWords = significantTokens(right);
        return leftWords.stream()
                .filter(rightWords::contains)
                .limit(3)
                .toList();
    }

    private String dominantTheme(String text) {
        var normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (normalized.contains("prior authorization") || normalized.contains("prior auth")) return "prior authorization";
        if (normalized.contains("review") || normalized.contains("approval")) return "review governance";
        if (normalized.contains("evidence") || normalized.contains("citation")) return "evidence quality";
        if (normalized.contains("deployment") || normalized.contains("operator")) return "operations";
        if (normalized.contains("fhir") || normalized.contains("pas")) return "fhir interoperability";
        return "workspace research";
    }

    private LinkedHashSet<String> significantTokens(String text) {
        return Arrays.stream((text == null ? "" : text).toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .map(String::trim)
                .filter(item -> item.length() > 3)
                .filter(item -> !List.of("what", "changes", "need", "with", "from", "this", "that", "should", "workflow").contains(item))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int relatedFindingCount(String organizationId, String theme) {
        var match = switch (theme) {
            case "prior authorization" -> "%prior%";
            case "review governance" -> "%review%";
            case "evidence quality" -> "%evidence%";
            case "operations" -> "%deploy%";
            case "fhir interoperability" -> "%fhir%";
            default -> "%";
        };
        var count = jdbcTemplate.queryForObject("""
                select count(*)
                from brief_finding f
                join engineering_brief b on b.brief_id = f.brief_id
                where b.organization_id = ?
                  and (lower(f.statement) like ? or lower(b.question) like ?)
                """, Integer.class, organizationId, match, match);
        return count == null ? 0 : count;
    }

    private List<WorkspaceSearchResponse.SearchHitSummary> searchBriefs(String organizationId, String query) {
        return jdbcTemplate.query("""
                select brief_id, question, status
                from engineering_brief
                where organization_id = ?
                  and lower(question) like ?
                order by created_at desc
                limit 6
                """, (rs, row) -> new WorkspaceSearchResponse.SearchHitSummary(
                "brief",
                rs.getString("brief_id"),
                rs.getString("question"),
                "Brief question and workflow status",
                dominantTheme(rs.getString("question")),
                rs.getString("status")
        ), organizationId, "%" + query + "%");
    }

    private List<WorkspaceSearchResponse.SearchHitSummary> searchFindings(String organizationId, String query) {
        return jdbcTemplate.query("""
                select f.finding_id, f.statement, b.status, b.question
                from brief_finding f
                join engineering_brief b on b.brief_id = f.brief_id
                where b.organization_id = ?
                  and lower(f.statement) like ?
                order by b.created_at desc
                limit 6
                """, (rs, row) -> new WorkspaceSearchResponse.SearchHitSummary(
                "finding",
                rs.getString("finding_id"),
                rs.getString("question"),
                rs.getString("statement"),
                dominantTheme(rs.getString("question")),
                rs.getString("status")
        ), organizationId, "%" + query + "%");
    }

    private List<WorkspaceSearchResponse.SearchHitSummary> searchApprovals(String organizationId, String query) {
        return jdbcTemplate.query("""
                select a.approval_id, a.approver_role, a.rationale, b.question, b.status
                from brief_approval a
                join engineering_brief b on b.brief_id = a.brief_id
                where a.organization_id = ?
                  and (lower(coalesce(a.rationale, '')) like ? or lower(b.question) like ?)
                order by a.approved_at desc
                limit 6
                """, (rs, row) -> new WorkspaceSearchResponse.SearchHitSummary(
                "approval",
                rs.getString("approval_id"),
                rs.getString("question"),
                rs.getString("approver_role") + ": " + Objects.toString(rs.getString("rationale"), "Approval recorded"),
                dominantTheme(rs.getString("question")),
                rs.getString("status")
        ), organizationId, "%" + query + "%", "%" + query + "%");
    }

    private List<WorkspaceSearchResponse.SearchHitSummary> searchEvidence(String organizationId, String query) {
        return jdbcTemplate.query("""
                select s.source_id, s.title, s.source_type, s.canonical_url, b.question, b.status
                from brief_source s
                join engineering_brief b on b.brief_id = s.brief_id
                where b.organization_id = ?
                  and (lower(s.title) like ? or lower(coalesce(s.canonical_url, '')) like ?)
                order by b.created_at desc
                limit 6
                """, (rs, row) -> new WorkspaceSearchResponse.SearchHitSummary(
                "evidence",
                rs.getString("source_id"),
                rs.getString("title"),
                rs.getString("source_type") + " · " + Objects.toString(rs.getString("canonical_url"), "no canonical URL"),
                dominantTheme(rs.getString("question")),
                rs.getString("status")
        ), organizationId, "%" + query + "%", "%" + query + "%");
    }

    private List<WorkspaceSearchResponse.SearchHitSummary> searchWorkspaceArtifacts(String organizationId, String query) {
        var hits = new ArrayList<WorkspaceSearchResponse.SearchHitSummary>();
        hits.addAll(jdbcTemplate.query("""
                select question_pack_id, name, summary, persona
                from workspace_question_pack
                where organization_id = ?
                  and (lower(name) like ? or lower(summary) like ? or lower(starter_question) like ? or lower(question_prompts) like ?)
                order by updated_at desc
                limit 4
                """, (rs, row) -> new WorkspaceSearchResponse.SearchHitSummary(
                "question_pack",
                rs.getString("question_pack_id"),
                rs.getString("name"),
                rs.getString("summary"),
                rs.getString("persona"),
                "ready"
        ), organizationId, "%" + query + "%", "%" + query + "%", "%" + query + "%", "%" + query + "%"));
        hits.addAll(jdbcTemplate.query("""
                select research_pack_id, name, summary
                from workspace_research_pack
                where organization_id = ?
                  and (lower(name) like ? or lower(summary) like ? or lower(recurring_questions) like ?)
                order by updated_at desc
                limit 4
                """, (rs, row) -> new WorkspaceSearchResponse.SearchHitSummary(
                "research_pack",
                rs.getString("research_pack_id"),
                rs.getString("name"),
                rs.getString("summary"),
                "research continuity",
                "ready"
        ), organizationId, "%" + query + "%", "%" + query + "%", "%" + query + "%"));
        return hits;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Timestamp parseInstantOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Timestamp.from(Instant.parse(value.trim() + "T00:00:00Z"));
    }

    private record BriefLite(String briefId, String question, String status) {
    }
}
