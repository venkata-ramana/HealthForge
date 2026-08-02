package dev.healthforge.platform.tenant;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TenantAdministrationService {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock = Clock.systemUTC();

    public TenantAdministrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public TenantAdministrationOverviewResponse overview(AuthenticatedActor actor) {
        ensureDemoTenantLandscape(actor);
        return new TenantAdministrationOverviewResponse(
                actor.organizationId(),
                Instant.now(clock),
                new TenantAdministrationOverviewResponse.HostedProductPosture(
                        "private_demo_with_hosted_foundation_artifacts",
                        "Organizations, workspaces, and workflow artifacts stay organization-scoped while product administration can inspect the broader tenant landscape.",
                        "Tenant-aware role delegation builds on the existing organization membership and role-assignment model.",
                        "Provisioning requests now make customer-space setup, delegated admin ownership, and deployment shape easier to explain."
                ),
                customerTenants(),
                List.of(
                        new TenantAdministrationOverviewResponse.IsolationBoundary(
                                "artifact_boundary",
                                "Artifact and workflow isolation",
                                "Briefs, approvals, workspace projects, exports, and telemetry remain scoped to a single organization_id.",
                                List.of("organization_id columns", "org-scoped role checks", "workspace and export filters")
                        ),
                        new TenantAdministrationOverviewResponse.IsolationBoundary(
                                "identity_boundary",
                                "Tenant-aware identity boundary",
                                "Users and roles are modeled as memberships plus role assignments per organization instead of a single global role surface.",
                                List.of("actor_organization_membership", "actor_role_assignment", "trusted proxy mappings")
                        ),
                        new TenantAdministrationOverviewResponse.IsolationBoundary(
                                "environment_boundary",
                                "Environment and deployment isolation",
                                "Private customer spaces can carry different deployment models, environment shapes, and delegated admin owners without implying shared runtime state.",
                                List.of("provisioning requests", "deployment guide posture", "operations policy views")
                        )
                ),
                roleDelegations(),
                provisioningRequests(actor.organizationId()),
                List.of(
                        new TenantAdministrationOverviewResponse.HostedPackagingArtifact(
                                "private_customer_space",
                                "Private customer space",
                                "Enterprise buyer / platform team",
                                "Dedicated private deployment story for customers that need tighter deployment and environment control.",
                                List.of("team workspace", "governed integrations", "private ops", "pilot readiness")
                        ),
                        new TenantAdministrationOverviewResponse.HostedPackagingArtifact(
                                "hosted_evaluator_workspace",
                                "Hosted evaluator workspace",
                                "Evaluator / innovation team",
                                "Lighter hosted-style packaging for tenant-scoped evaluation, trust, and workflow rehearsal conversations.",
                                List.of("brief workflow", "evaluation", "synthetic labs", "developer workflows")
                        ),
                        new TenantAdministrationOverviewResponse.HostedPackagingArtifact(
                                "builder_companion",
                                "Builder companion package",
                                "Technical builders",
                                "Tenant-scoped API, CLI, SDK, and VS Code surfaces for approved-brief-to-implementation workflows.",
                                List.of("developer overview", "repo guidance", "CLI", "VS Code companion")
                        )
                ),
                List.of(
                        "This remains a tenant-aware product foundation, not a production SaaS control plane.",
                        "Cross-tenant administration in this phase is an operator visibility surface, not a shared data plane for customer artifacts.",
                        "Provisioning workflows describe setup intent and delegation boundaries without claiming automated infrastructure provisioning."
                )
        );
    }

    public TenantProvisioningResponse createProvisioningRequest(TenantProvisioningRequest request, AuthenticatedActor actor) {
        ensureDemoTenantLandscape(actor);
        var now = Instant.now(clock);
        var provisioningRequestId = "tenant_provisioning_" + UUID.randomUUID();
        var capabilities = normalizedCapabilities(request.requestedCapabilities());
        jdbcTemplate.update("""
                insert into tenant_provisioning_request (
                    provisioning_request_id, organization_id, tenant_key, tenant_name, deployment_model,
                    environment_shape, status, requested_by, delegated_admin, requested_capabilities,
                    onboarding_summary, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                provisioningRequestId,
                actor.organizationId(),
                request.tenantKey().trim(),
                request.tenantName().trim(),
                request.deploymentModel().trim(),
                request.environmentShape().trim(),
                "requested",
                actor.actorId(),
                request.delegatedAdmin().trim(),
                String.join("|", capabilities),
                request.onboardingSummary().trim(),
                Timestamp.from(now),
                Timestamp.from(now)
        );
        return provisioningRequest(provisioningRequestId);
    }

    public List<TenantProvisioningResponse> listProvisioningRequests(AuthenticatedActor actor) {
        ensureDemoTenantLandscape(actor);
        return jdbcTemplate.query("""
                select provisioning_request_id, organization_id, tenant_key, tenant_name, deployment_model,
                       environment_shape, status, requested_by, delegated_admin, requested_capabilities,
                       onboarding_summary, updated_at
                from tenant_provisioning_request
                where organization_id = ?
                order by updated_at desc
                """, (rs, row) -> new TenantProvisioningResponse(
                rs.getString("provisioning_request_id"),
                rs.getString("organization_id"),
                rs.getString("tenant_key"),
                rs.getString("tenant_name"),
                rs.getString("deployment_model"),
                rs.getString("environment_shape"),
                rs.getString("status"),
                rs.getString("requested_by"),
                rs.getString("delegated_admin"),
                splitPipe(rs.getString("requested_capabilities")),
                setupChecklist(rs.getString("deployment_model"), rs.getString("environment_shape")),
                rs.getString("onboarding_summary"),
                rs.getTimestamp("updated_at").toInstant()
        ), actor.organizationId());
    }

    public TenantAnalyticsResponse analytics(AuthenticatedActor actor) {
        ensureDemoTenantLandscape(actor);
        var tenants = customerTenants();
        var activeTenants = (int) tenants.stream().filter(item -> "active".equals(item.status())).count();
        var privateTenants = (int) tenants.stream().filter(item -> "private_customer_space".equals(item.deploymentModel())).count();
        var hostedTenants = (int) tenants.stream().filter(item -> "hosted_evaluator_workspace".equals(item.deploymentModel())).count();

        return new TenantAnalyticsResponse(
                actor.organizationId(),
                Instant.now(clock),
                new TenantAnalyticsResponse.UsageSummary(
                        tenants.size(),
                        activeTenants,
                        privateTenants,
                        hostedTenants
                ),
                tenants.stream().map(tenant -> new TenantAnalyticsResponse.TenantUsage(
                        tenant.organizationId(),
                        tenant.displayName(),
                        tenant.userCount(),
                        briefsLast30Days(tenant.organizationId()),
                        tenant.projectCount(),
                        approvalsLast30Days(tenant.organizationId()),
                        engagementSignal(tenant.organizationId()),
                        packagingFit(tenant.deploymentModel(), tenant.tenantTier())
                )).toList(),
                List.of(
                        new TenantAnalyticsResponse.ProductPackagingView(
                                "enterprise_private_space",
                                "Enterprise private space",
                                "private deployment",
                                "Best fit for customers that want stronger environment separation, delegated administration, and operator posture workflows.",
                                List.of("tenant admin", "private ops", "identity delegation", "governed connectors")
                        ),
                        new TenantAnalyticsResponse.ProductPackagingView(
                                "hosted_evaluation_pack",
                                "Hosted evaluation pack",
                                "hosted evaluation",
                                "Best fit for evaluator teams that want multi-tenant-safe analysis, labs, and trust workflows without full private deployment.",
                                List.of("brief workflow", "evaluation", "synthetic labs", "developer workflows")
                        )
                ),
                List.of(
                        "Tenant analytics remain activity summaries rather than billing or production telemetry.",
                        "These packaging views help future hosted-product conversations stay grounded in currently modeled capabilities.",
                        "The strongest engagement signals today are approved-brief volume, workspace activity, and workflow operations posture per tenant."
                )
        );
    }

    public List<TenantMemberResponse> members(AuthenticatedActor actor) {
        ensureDemoTenantLandscape(actor);
        return jdbcTemplate.query("""
                select u.actor_user_id, u.display_name, u.auth_subject, u.identity_mode,
                       m.organization_id, m.status, m.joined_at, m.last_seen_at,
                       coalesce(string_agg(r.actor_role, '|' order by r.actor_role), '') as roles
                from actor_user u
                join actor_organization_membership m on m.actor_user_id = u.actor_user_id
                left join actor_role_assignment r
                  on r.actor_user_id = m.actor_user_id
                 and r.organization_id = m.organization_id
                where m.organization_id = ?
                group by u.actor_user_id, u.display_name, u.auth_subject, u.identity_mode,
                         m.organization_id, m.status, m.joined_at, m.last_seen_at
                order by u.display_name, u.actor_user_id
                """, (rs, row) -> new TenantMemberResponse(
                rs.getString("actor_user_id"),
                rs.getString("display_name"),
                rs.getString("auth_subject"),
                rs.getString("identity_mode"),
                rs.getString("organization_id"),
                rs.getString("status"),
                splitPipe(rs.getString("roles")),
                rs.getTimestamp("joined_at").toInstant(),
                rs.getTimestamp("last_seen_at").toInstant()
        ), actor.organizationId());
    }

    public TenantMemberResponse inviteMember(TenantMemberInvitationRequest request, AuthenticatedActor actor) {
        ensureDemoTenantLandscape(actor);
        var now = Timestamp.from(Instant.now(clock));
        var actorUserId = request.actorUserId().trim();
        var organizationId = actor.organizationId();
        jdbcTemplate.update("""
                insert into actor_user (actor_user_id, display_name, auth_subject, identity_mode, created_at, last_seen_at)
                values (?, ?, ?, ?, ?, ?)
                on conflict (actor_user_id) do update
                set display_name = excluded.display_name,
                    auth_subject = excluded.auth_subject,
                    identity_mode = excluded.identity_mode,
                    last_seen_at = excluded.last_seen_at
                """, actorUserId, request.displayName().trim(), request.authSubject().trim(),
                request.identityMode().trim(), now, now);
        jdbcTemplate.update("""
                insert into actor_organization_membership
                    (membership_id, actor_user_id, organization_id, status, joined_at, last_seen_at)
                values (?, ?, ?, 'invited', ?, ?)
                on conflict (actor_user_id, organization_id) do update
                set status = 'invited', last_seen_at = excluded.last_seen_at
                """, "membership_" + UUID.randomUUID(), actorUserId, organizationId, now, now);
        for (var role : normalizedRoles(request.roles())) {
            jdbcTemplate.update("""
                    insert into actor_role_assignment
                        (role_assignment_id, actor_user_id, organization_id, actor_role, granted_by, granted_at, last_seen_at)
                    values (?, ?, ?, ?, ?, ?, ?)
                    on conflict (actor_user_id, organization_id, actor_role) do update
                    set granted_by = excluded.granted_by, last_seen_at = excluded.last_seen_at
                    """, "role_" + UUID.randomUUID(), actorUserId, organizationId, role,
                    actor.actorId(), now, now);
        }
        return members(actor).stream()
                .filter(member -> member.actorUserId().equals(actorUserId))
                .findFirst()
                .orElseThrow();
    }

    private TenantProvisioningResponse provisioningRequest(String provisioningRequestId) {
        return jdbcTemplate.query("""
                select provisioning_request_id, organization_id, tenant_key, tenant_name, deployment_model,
                       environment_shape, status, requested_by, delegated_admin, requested_capabilities,
                       onboarding_summary, updated_at
                from tenant_provisioning_request
                where provisioning_request_id = ?
                """, rs -> rs.next() ? new TenantProvisioningResponse(
                rs.getString("provisioning_request_id"),
                rs.getString("organization_id"),
                rs.getString("tenant_key"),
                rs.getString("tenant_name"),
                rs.getString("deployment_model"),
                rs.getString("environment_shape"),
                rs.getString("status"),
                rs.getString("requested_by"),
                rs.getString("delegated_admin"),
                splitPipe(rs.getString("requested_capabilities")),
                setupChecklist(rs.getString("deployment_model"), rs.getString("environment_shape")),
                rs.getString("onboarding_summary"),
                rs.getTimestamp("updated_at").toInstant()
        ) : null, provisioningRequestId);
    }

    private List<TenantAdministrationOverviewResponse.CustomerTenantSummary> customerTenants() {
        return jdbcTemplate.query("""
                select o.organization_id,
                       o.display_name,
                       o.status,
                       o.last_seen_at,
                       count(distinct m.actor_user_id) as user_count,
                       count(distinct p.project_id) as project_count,
                       count(distinct b.brief_id) as brief_count
                from actor_organization o
                left join actor_organization_membership m on m.organization_id = o.organization_id
                left join workspace_project p on p.organization_id = o.organization_id
                left join engineering_brief b on b.organization_id = o.organization_id
                group by o.organization_id, o.display_name, o.status, o.last_seen_at
                order by o.last_seen_at desc
                """, (rs, row) -> new TenantAdministrationOverviewResponse.CustomerTenantSummary(
                rs.getString("organization_id"),
                rs.getString("display_name"),
                rs.getString("status"),
                tenantTier(rs.getString("organization_id")),
                deploymentModel(rs.getString("organization_id")),
                rs.getInt("user_count"),
                rs.getInt("project_count"),
                rs.getInt("brief_count"),
                rs.getTimestamp("last_seen_at").toInstant()
        ));
    }

    private List<TenantAdministrationOverviewResponse.RoleDelegation> roleDelegations() {
        return jdbcTemplate.query("""
                select organization_id, actor_user_id, string_agg(actor_role, '|' order by actor_role) as roles
                from actor_role_assignment
                group by organization_id, actor_user_id
                order by organization_id, actor_user_id
                """, (rs, row) -> new TenantAdministrationOverviewResponse.RoleDelegation(
                rs.getString("organization_id"),
                rs.getString("actor_user_id"),
                splitPipe(rs.getString("roles")),
                delegationSummary(splitPipe(rs.getString("roles")))
        ));
    }

    private List<TenantAdministrationOverviewResponse.ProvisioningRequestSummary> provisioningRequests(String organizationId) {
        return jdbcTemplate.query("""
                select provisioning_request_id, tenant_key, tenant_name, deployment_model, environment_shape,
                       status, delegated_admin, updated_at
                from tenant_provisioning_request
                where organization_id = ?
                order by updated_at desc
                """, (rs, row) -> new TenantAdministrationOverviewResponse.ProvisioningRequestSummary(
                rs.getString("provisioning_request_id"),
                rs.getString("tenant_key"),
                rs.getString("tenant_name"),
                rs.getString("deployment_model"),
                rs.getString("environment_shape"),
                rs.getString("status"),
                rs.getString("delegated_admin"),
                rs.getTimestamp("updated_at").toInstant()
        ), organizationId);
    }

    private void ensureDemoTenantLandscape(AuthenticatedActor actor) {
        seedOrganization(actor.organizationId(), "HealthForge Primary Demo Tenant", "active");
        seedOrganization("tenant.northstar-health", "Northstar Health Plan", "active");
        seedOrganization("tenant.river-ehr", "River EHR Platform", "active");

        seedUserAndMembership("tenant-admin.primary", "Primary Tenant Admin", actor.organizationId(), "administrator", "reviewer");
        seedUserAndMembership("northstar.admin", "Northstar Admin", "tenant.northstar-health", "administrator", "auditor");
        seedUserAndMembership("river.builder", "River Builder Lead", "tenant.river-ehr", "administrator", "reviewer");

        seedProject(actor.organizationId(), "program-hosted-foundation", "Hosted product foundation", "workspace", "phase19,tenant,hosted");
        seedProject("tenant.northstar-health", "program-payer-ops", "Payer operations workspace", "program", "payer,operations");
        seedProject("tenant.river-ehr", "program-builder-companion", "Builder companion workspace", "workspace", "builder,developer");

        seedProvisioningRequest(actor.organizationId(), "tenant_northstar_private", "Northstar private space", "private_customer_space", "single-tenant-private", "northstar.admin");
        seedProvisioningRequest(actor.organizationId(), "tenant_river_hosted_eval", "River hosted evaluation", "hosted_evaluator_workspace", "hosted-shared-control", "river.builder");
    }

    private void seedOrganization(String organizationId, String displayName, String status) {
        var now = Timestamp.from(Instant.now(clock).minus(2, ChronoUnit.DAYS));
        jdbcTemplate.update("""
                insert into actor_organization (organization_id, display_name, status, created_at, last_seen_at)
                values (?, ?, ?, ?, ?)
                on conflict (organization_id) do update
                set display_name = excluded.display_name,
                    status = excluded.status,
                    last_seen_at = greatest(actor_organization.last_seen_at, excluded.last_seen_at)
                """, organizationId, displayName, status, now, Timestamp.from(Instant.now(clock)));
    }

    private void seedUserAndMembership(String userId, String displayName, String organizationId, String... roles) {
        var now = Timestamp.from(Instant.now(clock));
        jdbcTemplate.update("""
                insert into actor_user (actor_user_id, display_name, auth_subject, identity_mode, created_at, last_seen_at)
                values (?, ?, ?, ?, ?, ?)
                on conflict (actor_user_id) do update
                set display_name = excluded.display_name,
                    last_seen_at = greatest(actor_user.last_seen_at, excluded.last_seen_at)
                """, userId, displayName, userId + "@demo.healthforge", "local_header", now, now);
        jdbcTemplate.update("""
                insert into actor_organization_membership (membership_id, actor_user_id, organization_id, status, joined_at, last_seen_at)
                values (?, ?, ?, ?, ?, ?)
                on conflict (actor_user_id, organization_id) do nothing
                """, "membership_" + organizationId + "_" + userId, userId, organizationId, "active", now, now);
        for (var role : roles) {
            jdbcTemplate.update("""
                    insert into actor_role_assignment (role_assignment_id, actor_user_id, organization_id, actor_role, granted_by, granted_at, last_seen_at)
                    values (?, ?, ?, ?, ?, ?, ?)
                    on conflict (actor_user_id, organization_id, actor_role) do nothing
                    """, "role_" + organizationId + "_" + userId + "_" + role, userId, organizationId, role, "phase19.seed", now, now);
        }
    }

    private void seedProject(String organizationId, String projectIdSuffix, String name, String kind, String tags) {
        var now = Timestamp.from(Instant.now(clock));
        jdbcTemplate.update("""
                insert into workspace_project (project_id, organization_id, name, kind, description, owner_actor_id, tags, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (project_id) do nothing
                """,
                organizationId + "." + projectIdSuffix,
                organizationId,
                name,
                kind,
                "Phase 19 seeded workspace for tenant-aware product administration storytelling.",
                "phase19.seed",
                tags,
                now,
                now
        );
    }

    private void seedProvisioningRequest(String organizationId, String tenantKey, String tenantName, String deploymentModel, String environmentShape, String delegatedAdmin) {
        var now = Timestamp.from(Instant.now(clock));
        jdbcTemplate.update("""
                insert into tenant_provisioning_request (
                    provisioning_request_id, organization_id, tenant_key, tenant_name, deployment_model,
                    environment_shape, status, requested_by, delegated_admin, requested_capabilities,
                    onboarding_summary, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (provisioning_request_id) do nothing
                """,
                "seed_" + tenantKey,
                organizationId,
                tenantKey,
                tenantName,
                deploymentModel,
                environmentShape,
                "ready_for_review",
                "phase19.seed",
                delegatedAdmin,
                "team_workspace|private_ops|identity_delegation",
                "Seeded provisioning request for tenant-aware hosted/private packaging walkthroughs.",
                now,
                now
        );
    }

    private int briefsLast30Days(String organizationId) {
        var value = jdbcTemplate.queryForObject("""
                select count(*) from engineering_brief
                where organization_id = ? and created_at >= ?
                """, Integer.class, organizationId, Timestamp.from(Instant.now(clock).minus(30, ChronoUnit.DAYS)));
        return value == null ? 0 : value;
    }

    private int approvalsLast30Days(String organizationId) {
        var value = jdbcTemplate.queryForObject("""
                select count(*) from brief_approval
                where organization_id = ? and approved_at >= ?
                """, Integer.class, organizationId, Timestamp.from(Instant.now(clock).minus(30, ChronoUnit.DAYS)));
        return value == null ? 0 : value;
    }

    private String engagementSignal(String organizationId) {
        var briefs = briefsLast30Days(organizationId);
        if (briefs >= 5) {
            return "high_workflow_adoption";
        }
        if (briefs >= 1) {
            return "early_builder_engagement";
        }
        return "admin_and_packaging_ready";
    }

    private String packagingFit(String deploymentModel, String tenantTier) {
        if ("private_customer_space".equals(deploymentModel)) {
            return "private_operator_and_governance_fit";
        }
        if ("enterprise".equals(tenantTier)) {
            return "hosted_evaluator_plus_builder_fit";
        }
        return "builder_companion_fit";
    }

    private String tenantTier(String organizationId) {
        if (organizationId.contains("northstar")) {
            return "enterprise";
        }
        if (organizationId.contains("river")) {
            return "builder_partner";
        }
        return "platform_owner";
    }

    private String deploymentModel(String organizationId) {
        if (organizationId.contains("northstar")) {
            return "private_customer_space";
        }
        if (organizationId.contains("river")) {
            return "hosted_evaluator_workspace";
        }
        return "platform_owner_workspace";
    }

    private String delegationSummary(List<String> roles) {
        var lowered = roles.stream().map(role -> role.toLowerCase(Locale.ROOT)).toList();
        if (lowered.contains("administrator") && lowered.contains("auditor")) {
            return "Delegated for customer administration plus oversight review.";
        }
        if (lowered.contains("administrator")) {
            return "Delegated as the primary customer-space administrator.";
        }
        return "Delegated for scoped workflow participation within the tenant.";
    }

    private List<String> normalizedCapabilities(List<String> requestedCapabilities) {
        if (requestedCapabilities == null || requestedCapabilities.isEmpty()) {
            return List.of("team_workspace", "developer_workflows", "synthetic_labs");
        }
        var values = new LinkedHashSet<String>();
        for (var capability : requestedCapabilities) {
            if (capability != null && !capability.isBlank()) {
                values.add(capability.trim());
            }
        }
        return values.stream().toList();
    }

    private List<String> normalizedRoles(List<String> roles) {
        var values = new LinkedHashSet<String>();
        for (var role : roles) {
            if (role == null || role.isBlank()) {
                continue;
            }
            var normalized = role.trim().toLowerCase(Locale.ROOT);
            try {
                dev.healthforge.platform.auth.ActorRole.parse(normalized);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unsupported tenant member role: " + normalized);
            }
            values.add(normalized);
        }
        return values.stream().toList();
    }

    private List<String> splitPipe(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\\|")).stream().filter(item -> !item.isBlank()).toList();
    }

    private List<String> setupChecklist(String deploymentModel, String environmentShape) {
        return List.of(
                "Confirm delegated admin ownership for " + deploymentModel + ".",
                "Prepare environment baseline for " + environmentShape + ".",
                "Verify identity mappings, organization setup, and role delegation before user onboarding.",
                "Review workspace, integration, and operations boundaries before opening the tenant space."
        );
    }
}
