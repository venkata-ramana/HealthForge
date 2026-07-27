package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnterprisePostureService {

    private final AuditPolicyProperties auditPolicyProperties;

    public EnterprisePostureService(AuditPolicyProperties auditPolicyProperties) {
        this.auditPolicyProperties = auditPolicyProperties;
    }

    public EnterprisePostureResponse posture(AuthenticatedActor actor) {
        return new EnterprisePostureResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                "private_local_or_self_hosted",
                actor.identityMode(),
                List.of("reviewer", "approver", "auditor", "administrator"),
                List.of(
                        "Organization-scoped persistence for briefs, approvals, validation telemetry, export telemetry, and audit history",
                        "Role-gated approval and audit export actions",
                        "Preview-only tracker export workflow with explicit retention-until metadata",
                        "Synthetic/non-sensitive validation boundary",
                        "Private deployment scaffolding through Docker Compose and Terraform starter infrastructure"
                ),
                new EnterprisePostureResponse.RetentionPolicy(
                        auditPolicyProperties.trackedExportRetentionDays(),
                        auditPolicyProperties.validationTelemetryRetention(),
                        auditPolicyProperties.auditEvidenceRetention()
                ),
                new EnterprisePostureResponse.AuditPolicy(
                        auditPolicyProperties.policyVersion(),
                        auditPolicyProperties.accessReviewCadence(),
                        auditPolicyProperties.roleReviewExpectation(),
                        auditPolicyProperties.approvalRequiredForExports()
                ),
                List.of(
                        "This remains a human-review-first platform and is not production PHI infrastructure.",
                        "Identity is still header-backed in the local/private deployment path and is ready for future SSO replacement.",
                        "External tracker writeback is intentionally disabled in this phase.",
                        "Access review and audit policy reporting are enterprise-demo safe and organization scoped."
                ),
                List.of(
                        "Replace local header identity with SSO + group mapping.",
                        "Add automated retention jobs and configurable policy tiers.",
                        "Add tenant-aware policy routing and production secret management.",
                        "Add policy attestation workflows and operator sign-off history."
                )
        );
    }
}
