package dev.healthforge.platform.regulatory;

import java.time.Instant;
import java.util.List;

public record RegulatedReadinessResponse(
        String organizationId,
        String actorId,
        String actorRole,
        Instant generatedAt,
        SecurityPosture securityPosture,
        ComplianceEvidencePack complianceEvidencePack,
        DeploymentArchitecturePack deploymentArchitecturePack,
        ReleaseGovernancePack releaseGovernancePack,
        ResilienceReadinessPack resilienceReadinessPack,
        List<String> regulatedDeploymentNarratives
) {
    public record SecurityPosture(
            String summary,
            List<DependencyEvidence> dependencyEvidence,
            List<String> supplyChainControls,
            List<String> operatorSecurityWorkflows
    ) {}

    public record DependencyEvidence(
            String component,
            String evidenceType,
            String currentState,
            String enterpriseNarrative
    ) {}

    public record ComplianceEvidencePack(
            String summary,
            List<ControlMapping> controlMappings,
            List<String> auditFacingArtifacts,
            List<String> automationBoundaries
    ) {}

    public record ControlMapping(
            String controlId,
            String title,
            String mappedSurface,
            String evidenceArtifact,
            String currentCoverage
    ) {}

    public record DeploymentArchitecturePack(
            String deploymentTier,
            List<ArchitectureView> architectureViews,
            List<String> secretAndNetworkControls,
            List<String> environmentGuardrails
    ) {}

    public record ArchitectureView(
            String title,
            String deploymentTarget,
            String summary,
            List<String> designNotes
    ) {}

    public record ReleaseGovernancePack(
            String summary,
            List<ReleaseControl> releaseControls,
            List<String> changeManagementArtifacts,
            List<String> retentionAutomationNarratives
    ) {}

    public record ReleaseControl(
            String controlId,
            String title,
            String changeStage,
            String evidenceRequirement
    ) {}

    public record ResilienceReadinessPack(
            String summary,
            List<RecoveryArtifact> recoveryArtifacts,
            List<String> continuityNarratives,
            List<String> futureRoadmapSignals
    ) {}

    public record RecoveryArtifact(
            String artifactId,
            String title,
            String resilienceArea,
            String status,
            String summary
    ) {}
}
