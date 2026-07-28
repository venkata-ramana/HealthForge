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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
                evidenceCollections(actor.organizationId())
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

    private void ensureSeedData(AuthenticatedActor actor) {
        seedIdentityFoundation(actor.organizationId());
        seedProjects(actor);
        seedWorkflowConfigurations(actor);
        seedSavedViews(actor);
        seedAssignments(actor);
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
