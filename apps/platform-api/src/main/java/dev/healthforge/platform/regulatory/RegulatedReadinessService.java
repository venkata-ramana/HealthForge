package dev.healthforge.platform.regulatory;

import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.enterprise.AuditPolicyProperties;
import dev.healthforge.platform.enterprise.OperationsPolicyProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class RegulatedReadinessService {

    private final JdbcTemplate jdbcTemplate;
    private final AuditPolicyProperties auditPolicyProperties;
    private final OperationsPolicyProperties operationsPolicyProperties;
    private final Clock clock = Clock.systemUTC();

    public RegulatedReadinessService(
            JdbcTemplate jdbcTemplate,
            AuditPolicyProperties auditPolicyProperties,
            OperationsPolicyProperties operationsPolicyProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditPolicyProperties = auditPolicyProperties;
        this.operationsPolicyProperties = operationsPolicyProperties;
    }

    public RegulatedReadinessResponse overview(AuthenticatedActor actor) {
        return new RegulatedReadinessResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                securityPosture(),
                complianceEvidencePack(),
                deploymentArchitecturePack(),
                releaseGovernancePack(actor.organizationId()),
                resilienceReadinessPack(actor.organizationId()),
                List.of(
                        "HealthForge now has concrete regulated-enterprise narrative assets, but it still does not claim production certification or compliance attestation.",
                        "Security, release, and resilience packs are evidence-oriented planning artifacts that make enterprise conversations more concrete without overstating current guarantees.",
                        "The regulated deployment path is now easier to explain across operators, auditors, enterprise architects, and security reviewers."
                )
        );
    }

    private RegulatedReadinessResponse.SecurityPosture securityPosture() {
        return new RegulatedReadinessResponse.SecurityPosture(
                "Security posture is explained through dependency evidence, supply-chain controls, and operator workflow packaging rather than broad certification claims.",
                List.of(
                        new RegulatedReadinessResponse.DependencyEvidence(
                                "Spring Boot + Java runtime",
                                "dependency posture",
                                "Version-pinned in Maven build and validated through CI platform-api tests.",
                                "Shows that runtime dependencies are explicit, test-backed, and ready for stronger secure-delivery conversations."
                        ),
                        new RegulatedReadinessResponse.DependencyEvidence(
                                "HAPI FHIR validation stack",
                                "standards/runtime dependency",
                                "Pinned for deterministic synthetic validation workflows with explicit package boundaries.",
                                "Demonstrates controlled standards-runtime usage rather than dynamic third-party dependency drift."
                        ),
                        new RegulatedReadinessResponse.DependencyEvidence(
                                "GitHub Actions builder workflow",
                                "supply-chain workflow evidence",
                                "CI now provisions its dependent services explicitly and validates developer + platform surfaces together.",
                                "Supports the secure release narrative by making delivery dependencies visible instead of implicit."
                        )
                ),
                List.of(
                        "Pinned dependency versions in Maven and local JS package manifests.",
                        "Explicit CI validation for platform API and builder-facing surfaces.",
                        "Secret references modeled as environment-managed references rather than embedded credentials.",
                        "Governed connector posture remains visible and environment-bounded."
                ),
                List.of(
                        "Review dependency posture during release preparation and attach the current workflow/test evidence.",
                        "Treat connector-mode changes, auth-mode changes, and deployment-tier changes as security-significant operator workflows.",
                        "Use the regulated readiness pack plus operations configuration surfaces together when discussing secure enterprise deployment."
                )
        );
    }

    private RegulatedReadinessResponse.ComplianceEvidencePack complianceEvidencePack() {
        return new RegulatedReadinessResponse.ComplianceEvidencePack(
                "Compliance evidence is now packaged as reusable mappings between current HealthForge surfaces and the artifacts enterprise reviewers expect to see.",
                List.of(
                        new RegulatedReadinessResponse.ControlMapping(
                                "HF-AUDIT-01",
                                "Review and approval traceability",
                                "Brief workflow, approvals, and audit export",
                                "Brief audit export + tracked work-item export",
                                "implemented_for_private_demo_and_enterprise_storytelling"
                        ),
                        new RegulatedReadinessResponse.ControlMapping(
                                "HF-OPS-02",
                                "Environment and configuration governance",
                                "Operations configuration + deployment promotion guide",
                                "Config boundary view + deployment guide",
                                "implemented_as_operator-facing regulated deployment pack"
                        ),
                        new RegulatedReadinessResponse.ControlMapping(
                                "HF-REL-03",
                                "Release and change evidence retention",
                                "Operations attestations + workflow automation receipts",
                                "Attestation history + delivery receipts + retention summaries",
                                "implemented_as_audit-ready_release_governance_artifact"
                        ),
                        new RegulatedReadinessResponse.ControlMapping(
                                "HF-RES-04",
                                "Continuity and recovery readiness",
                                "Continuity view + resilience pack",
                                "Backup/restore checklists + resilience roadmap artifacts",
                                "implemented_as_current_and_future_state_packaging"
                        )
                ),
                List.of(
                        "Brief audit exports",
                        "Tracked export and governed delivery receipts",
                        "Policy/safety and evaluation dashboards",
                        "Operations attestation history",
                        "Tenant administration and provisioning records"
                ),
                List.of(
                        "Control mappings package current evidence; they do not assert external audit completion.",
                        "Evidence automation packages artifacts already produced by the platform rather than inventing synthetic compliance status.",
                        "Future control automation can expand from these reusable evidence bundles without changing the current trust boundaries."
                )
        );
    }

    private RegulatedReadinessResponse.DeploymentArchitecturePack deploymentArchitecturePack() {
        return new RegulatedReadinessResponse.DeploymentArchitecturePack(
                operationsPolicyProperties.getDeploymentTier(),
                List.of(
                        new RegulatedReadinessResponse.ArchitectureView(
                                "Private deployment control plane",
                                "private_customer_space",
                                "Enterprise-private deployment with organization-scoped workflows, environment-separated controls, and externally managed secrets.",
                                List.of(
                                        "Keep connector credentials outside the application runtime config.",
                                        "Separate operator, reviewer, and auditor usage paths with role-aware workflow surfaces.",
                                        "Use environment-specific policy and attestation packs before enabling governed delivery."
                                )
                        ),
                        new RegulatedReadinessResponse.ArchitectureView(
                                "Hosted evaluator pattern",
                                "hosted_evaluator_workspace",
                                "Lighter hosted deployment posture for trusted evaluation, labs, and workflow rehearsal without implying full production SaaS maturity.",
                                List.of(
                                        "Retain tenant isolation and packaging boundaries.",
                                        "Constrain the hosted story to bounded synthetic/demo-safe workflows.",
                                        "Use tenant analytics and packaging views to keep hosted conversations honest."
                                )
                        )
                ),
                List.of(
                        "Credential references are environment-scoped and rotatable.",
                        "Network and environment controls are described per deployment stage and not embedded as application logic.",
                        "Secret, connector, and retention expectations are documented as operator-visible controls."
                ),
                List.of(
                        "Promotion between environments should be evidence-backed and operator-approved.",
                        "Networking, secrets, and environment boundaries remain deployment concerns, not mutable end-user settings.",
                        "Current deployment packs clarify target-state direction without claiming a finished regulated hosting platform."
                )
        );
    }

    private RegulatedReadinessResponse.ReleaseGovernancePack releaseGovernancePack(String organizationId) {
        var attestationCount = count("select count(*) from operations_attestation where organization_id = ?", organizationId);
        return new RegulatedReadinessResponse.ReleaseGovernancePack(
                "Release governance is now packaged as audit-ready change controls, evidence retention narratives, and operator sign-off expectations.",
                List.of(
                        new RegulatedReadinessResponse.ReleaseControl(
                                "REL-GATE-01",
                                "Platform validation gate",
                                "pre-release",
                                "Platform API tests, synthetic lab coverage, and evaluation-quality evidence are reviewed before release."
                        ),
                        new RegulatedReadinessResponse.ReleaseControl(
                                "REL-GATE-02",
                                "Operator sign-off gate",
                                "promotion",
                                "Operations attestation recorded for environment, connector, or policy changes."
                        ),
                        new RegulatedReadinessResponse.ReleaseControl(
                                "REL-GATE-03",
                                "Evidence retention gate",
                                "post-release",
                                "Retention timelines and release evidence are preserved with the current audit policy."
                        )
                ),
                List.of(
                        "Attestation events recorded: " + attestationCount,
                        "Deployment promotion guide",
                        "Operations usage + observability summaries",
                        "Compliance dashboard and policy/safety report"
                ),
                List.of(
                        "Retention automation is currently narrative and artifact-packaging oriented rather than full automatic archival enforcement.",
                        "Change-management evidence can already be packaged from existing audit, attestation, and workflow-event surfaces.",
                        "This gives enterprise reviewers a concrete release-control story without claiming ITSM or external GRC integration completeness."
                )
        );
    }

    private RegulatedReadinessResponse.ResilienceReadinessPack resilienceReadinessPack(String organizationId) {
        return new RegulatedReadinessResponse.ResilienceReadinessPack(
                "Resilience readiness extends the existing continuity story into disaster recovery, business continuity, and enterprise recovery-roadmap artifacts.",
                List.of(
                        new RegulatedReadinessResponse.RecoveryArtifact(
                                "resilience_backup_restore",
                                "Backup and restore readiness pack",
                                "backup_restore",
                                "implemented",
                                "Builds on continuity inventory, backup guidance, restore checks, and migration validation."
                        ),
                        new RegulatedReadinessResponse.RecoveryArtifact(
                                "resilience_operator_rehearsal",
                                "Operator recovery rehearsal narrative",
                                "incident_recovery",
                                "implemented",
                                "Packages current recovery rehearsal guidance into a regulated-deployment conversation artifact."
                        ),
                        new RegulatedReadinessResponse.RecoveryArtifact(
                                "resilience_dr_roadmap",
                                "Disaster recovery roadmap view",
                                "future_state_dr",
                                "roadmap",
                                "Clarifies what would need to mature next for stronger regulated enterprise resilience conversations."
                        )
                ),
                List.of(
                        "Business continuity is currently documented as operator guidance plus rehearsable checklists.",
                        "Recovery readiness remains bounded by private deployment and demo-safe tenant expectations today.",
                        "Resilience posture is explained as current-state artifacts plus future-state roadmap rather than overstated DR certification."
                ),
                List.of(
                        "Introduce environment-specific recovery objectives and evidence packaging per deployment tier.",
                        "Expand attestation history into explicit resilience rehearsal evidence.",
                        "Connect tenant packaging, release governance, and continuity packs into a fuller regulated deployment readiness storyline."
                )
        );
    }

    private int count(String sql, Object... args) {
        var value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
