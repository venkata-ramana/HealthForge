package dev.healthforge.platform.enterprise;

import dev.healthforge.platform.auth.AuthenticatedActor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeploymentPromotionGuideService {

    public DeploymentPromotionGuideResponse guide(AuthenticatedActor actor) {
        return new DeploymentPromotionGuideResponse(
                actor.organizationId(),
                "private_local_or_self_hosted",
                List.of(
                        new DeploymentPromotionGuideResponse.EnvironmentStage(
                                "demo",
                                "Fast showcase environment for synthetic scenarios and guided walkthroughs.",
                                List.of("docker compose overrides", "local actor headers", "sample corpus snapshots"),
                                List.of("confirm sample data is synthetic", "confirm webhook subscriptions are preview-only where needed")
                        ),
                        new DeploymentPromotionGuideResponse.EnvironmentStage(
                                "staging",
                                "Internal validation environment for governed connectors and operator rehearsal.",
                                List.of("environment-specific .env files", "terraform variable sets", "connector target labels"),
                                List.of("verify approval gates", "verify retention windows", "verify rollback tag is captured")
                        ),
                        new DeploymentPromotionGuideResponse.EnvironmentStage(
                                "private_prod",
                                "Customer-hosted or enterprise-private deployment with explicit environment controls.",
                                List.of("secret manager inputs", "SSO-ready auth config", "environment-scoped webhook/documentation targets"),
                                List.of("review change ticket", "review rollback plan", "confirm export and delivery policies")
                        )
                ),
                List.of(
                        "Promote the same application artifact forward and change only environment-scoped configuration.",
                        "Enable governed webhook, collaboration, and documentation targets per environment rather than in source control.",
                        "Capture operator sign-off before turning preview-only integrations into explicit send/publish modes.",
                        "Validate rollback and retention settings before each promotion."
                ),
                List.of(
                        "Keep the previous image tag and environment variables available for immediate rollback.",
                        "Disable webhook subscriptions first if an external automation path behaves unexpectedly.",
                        "Revert collaboration/documentation targets to preview-only mode before wider rollback if downstream systems were not impacted.",
                        "Preserve audit, export, and delivery telemetry for post-incident review."
                ),
                List.of(
                        "Production PHI handling is still out of scope for this phase.",
                        "Connector credentials remain environment-governed and never belong in repo history.",
                        "Promotion should preserve organization boundaries, approval traces, and retention policy defaults."
                )
        );
    }
}
