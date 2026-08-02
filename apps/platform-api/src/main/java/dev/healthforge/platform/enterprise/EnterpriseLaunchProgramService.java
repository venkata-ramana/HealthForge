package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class EnterpriseLaunchProgramService {

    private final Clock clock = Clock.systemUTC();

    public EnterpriseLaunchProgramResponse program(AuthenticatedActor actor) {
        return new EnterpriseLaunchProgramResponse(
                actor.organizationId(), actor.actorId(), actor.role().name().toLowerCase(), Instant.now(clock),
                "bounded_enterprise_pilot_ready",
                List.of(
                        new EnterpriseLaunchProgramResponse.Phase(
                                "phase_41", "Enterprise SSO and identity lifecycle", "contract_ready",
                                "Trusted-proxy identity, tenant membership enforcement, role mapping, and access-review surfaces are ready for an enterprise identity provider.",
                                List.of("trusted_proxy auth mode", "group-to-role mapping", "active membership checks", "identity directory"),
                                List.of("Configure the target OIDC/SAML provider", "Use signed assertions", "Test joiner, mover, and leaver events")
                        ),
                        new EnterpriseLaunchProgramResponse.Phase(
                                "phase_42", "Automated tenant provisioning and environments", "operator_ready",
                                "Tenant provisioning requests, delegated ownership, invitations, deployment shapes, and reconciliation-oriented checklists are available to operators.",
                                List.of("tenant provisioning API", "tenant member invitations", "deployment model and environment shape", "operator setup checklist"),
                                List.of("Connect infrastructure automation", "Add activation callbacks", "Reconcile environment health")
                        ),
                        new EnterpriseLaunchProgramResponse.Phase(
                                "phase_43", "Usage metering, quotas, and commercial readiness", "advisory_ready",
                                "Tenant analytics, usage summaries, soft quotas, packaging views, and pilot outcome signals support capacity and commercial conversations.",
                                List.of("enterprise usage API", "tenant analytics", "soft quota policy", "pilot analytics"),
                                List.of("Choose a commercial model", "Add durable metering export", "Integrate billing only after packaging approval")
                        ),
                        new EnterpriseLaunchProgramResponse.Phase(
                                "phase_44", "Security, privacy, and compliance evidence", "evidence_ready",
                                "Access review, audit policy, retention, operations, controlled rollout, and readiness surfaces provide a reviewable evidence foundation.",
                                List.of("access review reports", "audit policy", "retention guidance", "controlled rollout evidence", "production readiness scorecard"),
                                List.of("Complete threat model review", "Map target customer controls", "Assign remediation owners and review cadence")
                        ),
                        new EnterpriseLaunchProgramResponse.Phase(
                                "phase_45", "Production SaaS launch and customer operations", "pilot_gate_ready",
                                "A bounded private-pilot launch path is defined with ownership, rollback, continuity, support, and go/no-go evidence requirements.",
                                List.of("SaaS readiness scorecard", "controlled rollout registry", "operations continuity", "pilot success analytics"),
                                List.of("Name the service owner", "Rehearse restore and rollback", "Approve the target tenant cohort and launch decision")
                        )
                ),
                List.of(
                        "Identity provider and tenant membership evidence is verified.",
                        "Tenant provisioning and rollback ownership is explicit.",
                        "Usage, retention, security, and incident contacts are documented.",
                        "Restore and rollback rehearsals pass in the target environment.",
                        "A named decision maker records the go/no-go outcome."
                ),
                List.of(
                        "This is an enterprise pilot operating program, not a production SaaS certification.",
                        "PHI, clinical decision support, billing, and regulatory attestations remain outside the current bounded platform claim.",
                        "Identity providers, infrastructure automation, and commercial integrations must be selected per deployment."
                ),
                "Phases 41–45 complete the enterprise launch program for a bounded pilot; production expansion remains evidence- and environment-dependent."
        );
    }
}
