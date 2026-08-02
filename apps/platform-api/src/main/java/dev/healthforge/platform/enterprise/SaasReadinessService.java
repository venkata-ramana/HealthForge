package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class SaasReadinessService {

    private final Clock clock = Clock.systemUTC();

    public SaasReadinessResponse readiness(AuthenticatedActor actor) {
        return new SaasReadinessResponse(
                actor.organizationId(),
                actor.actorId(),
                actor.role().name().toLowerCase(),
                Instant.now(clock),
                "saas_ready_private_pilot_foundation",
                List.of(
                        new SaasReadinessResponse.PhaseReadiness(
                                "phase_37",
                                "Enterprise identity integration",
                                "foundation_complete",
                                "Tenant-scoped identity supports local headers and trusted-proxy group mapping with configurable active-membership enforcement.",
                                List.of("AuthenticatedActorProvider", "trusted_proxy mode", "HEALTHFORGE_AUTH_ENFORCE_MEMBERSHIP", "tenant membership and role checks"),
                                List.of("Connect a production identity provider", "Replace trusted headers with signed identity assertions", "Exercise joiner/mover/leaver lifecycle")
                        ),
                        new SaasReadinessResponse.PhaseReadiness(
                                "phase_38",
                                "Tenant provisioning lifecycle",
                                "operator_guided_complete",
                                "Administrators can inspect tenant boundaries, request provisioning, list members, and create organization-scoped invitations.",
                                List.of("tenant administration overview", "tenant provisioning requests", "member listing", "member invitations"),
                                List.of("Automate infrastructure provisioning", "Add invitation delivery and activation callbacks", "Add environment health reconciliation")
                        ),
                        new SaasReadinessResponse.PhaseReadiness(
                                "phase_39",
                                "Usage and commercial controls",
                                "advisory_controls_complete",
                                "Tenant usage, soft quotas, packaging views, and pilot analytics provide explainable adoption and capacity signals.",
                                List.of("enterprise usage endpoint", "tenant analytics", "soft quota configuration", "pilot funnel and outcome analytics"),
                                List.of("Add hard enforcement where commercially required", "Introduce metering retention policy", "Connect billing or contract systems")
                        ),
                        new SaasReadinessResponse.PhaseReadiness(
                                "phase_40",
                                "Security and SaaS launch gates",
                                "private_pilot_gate_complete",
                                "Production-readiness and controlled-rollout scorecards make security, resilience, ownership, and evidence gaps visible before expansion.",
                                List.of("production-readiness scorecard", "controlled-rollout evidence registry", "access review reports", "operations continuity and attestation views"),
                                List.of("Complete formal security review", "Rehearse restore and incident runbooks", "Approve an environment-specific launch decision")
                        )
                ),
                List.of(
                        "A production identity provider and signed identity assertions are configured.",
                        "Tenant isolation and access-review evidence are verified in the target environment.",
                        "Restore, rollback, retention, and incident contacts are rehearsed.",
                        "A named owner approves the private-pilot or hosted launch decision."
                ),
                List.of(
                        "This is an evidence-oriented readiness view, not a SaaS certification.",
                        "HealthForge remains bounded to synthetic and non-sensitive interoperability work.",
                        "Automated infrastructure provisioning, billing, PHI handling, and compliance attestations remain deployment-specific follow-on work."
                ),
                "Phases 37–40 complete the SaaS-ready operating foundation; production launch still requires environment-specific identity, security, resilience, and ownership evidence."
        );
    }
}
