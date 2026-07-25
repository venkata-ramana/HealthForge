package dev.healthforge.platform.fhirassistant;

import dev.healthforge.platform.fhir.FhirValidationCatalogProperties;
import dev.healthforge.platform.standards.StandardsArtifactProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FhirKnowledgeAssistantService {

    private final StandardsArtifactProperties standardsProperties;
    private final FhirValidationCatalogProperties catalogProperties;
    private final Clock clock = Clock.systemUTC();

    public FhirKnowledgeAssistantService(
            StandardsArtifactProperties standardsProperties,
            FhirValidationCatalogProperties catalogProperties
    ) {
        this.standardsProperties = standardsProperties;
        this.catalogProperties = catalogProperties;
    }

    public FhirKnowledgeAssistantResponse assist(FhirKnowledgeAssistantRequest request) {
        var normalizedQuery = request.query().trim().toLowerCase(Locale.ROOT);
        var requestedArtifactType = request.artifactType() == null ? null : request.artifactType().trim().toLowerCase(Locale.ROOT);
        var packageFilter = request.packageId() == null ? null : request.packageId().trim().toLowerCase(Locale.ROOT);
        var limit = request.limit() == null ? 5 : Math.max(1, Math.min(request.limit(), 10));

        var packageById = catalogProperties.getPackages().stream()
                .collect(Collectors.toMap(pkg -> pkg.getPackageId().toLowerCase(Locale.ROOT), pkg -> pkg));

        var artifactMatches = standardsProperties.getArtifacts().stream()
                .filter(artifact -> packageFilter == null || artifact.getPackageId().toLowerCase(Locale.ROOT).contains(packageFilter))
                .filter(artifact -> requestedArtifactType == null || artifact.getArtifactType().equalsIgnoreCase(requestedArtifactType))
                .map(artifact -> toMatch(artifact, normalizedQuery, packageById))
                .filter(match -> !match.matchedKeywords().isEmpty() || normalizedQuery.isBlank())
                .sorted(Comparator.comparingInt(ArtifactMatch::score).reversed()
                        .thenComparing(match -> match.artifact().getArtifactType())
                        .thenComparing(match -> match.artifact().getTitle()))
                .limit(limit)
                .map(match -> toArtifactInsight(match, packageById))
                .toList();

        var packageMatches = catalogProperties.getPackages().stream()
                .filter(pkg -> packageFilter == null || pkg.getPackageId().toLowerCase(Locale.ROOT).contains(packageFilter))
                .filter(pkg -> matchesPackage(pkg, normalizedQuery) || artifactMatches.stream().anyMatch(artifact -> artifact.packageId().equals(pkg.getPackageId())))
                .sorted(Comparator.comparing(FhirValidationCatalogProperties.FhirPackage::getPackageKind)
                        .thenComparing(FhirValidationCatalogProperties.FhirPackage::getPackageTitle))
                .limit(limit)
                .map(this::toPackageInsight)
                .toList();

        var unsupportedRequests = unsupportedRequests(normalizedQuery, requestedArtifactType, artifactMatches);
        var status = artifactMatches.isEmpty() && packageMatches.isEmpty() ? "no_match" : "grounded";

        return new FhirKnowledgeAssistantResponse(
                "fhir_assist_" + UUID.randomUUID(),
                status,
                Instant.now(clock),
                request.query(),
                summarize(status, artifactMatches, packageMatches, normalizedQuery),
                artifactMatches,
                packageMatches,
                List.of(
                        "This assistant only inspects curated standards metadata and the pinned local validation catalog.",
                        "Artifact presence does not claim payer-specific support or production conformance.",
                        "Human review is still required before implementation decisions, package pinning, or external interoperability claims."
                ),
                unsupportedRequests,
                "This local assistant is standards-aware but non-authoritative. Treat it as developer guidance over curated metadata, not as a certification or conformance decision."
        );
    }

    private String summarize(
            String status,
            List<FhirKnowledgeAssistantResponse.ArtifactInsight> artifactMatches,
            List<FhirKnowledgeAssistantResponse.PackageInsight> packageMatches,
            String normalizedQuery
    ) {
        if (!"grounded".equals(status)) {
            return "No curated standards artifact or validation package matched the request closely enough. Refine the query by resource name, guide acronym, profile, or workflow term such as PAS, CRD, or DTR.";
        }
        if (normalizedQuery.contains("operation") || normalizedQuery.contains("$submit") || normalizedQuery.contains("hook")) {
            return "The assistant found curated operation- or workflow-adjacent artifacts and packaged them with support boundaries so reviewers can inspect likely standards touchpoints without making conformance claims.";
        }
        if (!artifactMatches.isEmpty() && !packageMatches.isEmpty()) {
            return "The assistant matched both curated standards artifacts and pinned validation packages so a developer can inspect the likely resource, profile, and guide surfaces together.";
        }
        if (!artifactMatches.isEmpty()) {
            return "The assistant matched curated standards artifacts for the request and surfaced their evidence links, boundaries, and package relationships.";
        }
        return "The assistant matched validation packages and profiles, but the current curated artifact registry did not yield a stronger artifact-level hit for the request.";
    }

    private List<String> unsupportedRequests(
            String normalizedQuery,
            String requestedArtifactType,
            List<FhirKnowledgeAssistantResponse.ArtifactInsight> artifactMatches
    ) {
        var unsupported = new ArrayList<String>();
        var asksForOperation = "operation".equals(requestedArtifactType)
                || normalizedQuery.contains("$submit")
                || normalizedQuery.contains("operation")
                || normalizedQuery.contains("hook");
        if (asksForOperation && artifactMatches.stream().noneMatch(match -> "operation".equals(match.artifactType()))) {
            unsupported.add("No curated operation artifact matched the request. The assistant can still surface nearby PAS, CRD, or DTR workflow artifacts, but operation applicability remains a reviewer task.");
        }
        if (normalizedQuery.contains("certified") || normalizedQuery.contains("guarantee") || normalizedQuery.contains("compliant")) {
            unsupported.add("The assistant does not certify compliance, conformance, or production-readiness from metadata lookup alone.");
        }
        return unsupported;
    }

    private boolean matchesPackage(FhirValidationCatalogProperties.FhirPackage pkg, String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            return true;
        }
        if (pkg.getPackageId().toLowerCase(Locale.ROOT).contains(normalizedQuery)) return true;
        if (pkg.getPackageTitle().toLowerCase(Locale.ROOT).contains(normalizedQuery)) return true;
        return pkg.getProfiles().stream().anyMatch(profile ->
                profile.getProfileTitle().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || profile.getProfileUrl().toLowerCase(Locale.ROOT).contains(normalizedQuery));
    }

    private ArtifactMatch toMatch(
            StandardsArtifactProperties.Artifact artifact,
            String normalizedQuery,
            Map<String, FhirValidationCatalogProperties.FhirPackage> packageById
    ) {
        var matchedKeywords = new ArrayList<String>();
        var score = 0;
        if (normalizedQuery.isBlank()) {
            score = 1;
        }
        if (artifact.getTitle().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
            matchedKeywords.add("title");
            score += 10;
        }
        if (artifact.getArtifactId().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
            matchedKeywords.add("artifact_id");
            score += 9;
        }
        if (artifact.getCanonicalUrl().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
            matchedKeywords.add("canonical_url");
            score += 7;
        }
        for (var keyword : artifact.getKeywords()) {
            if (normalizedQuery.contains(keyword.toLowerCase(Locale.ROOT)) || keyword.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                matchedKeywords.add(keyword);
                score += 6;
            }
        }
        var pkg = packageById.get(artifact.getPackageId().toLowerCase(Locale.ROOT));
        if (pkg != null && pkg.getPackageTitle().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
            matchedKeywords.add("package_title");
            score += 5;
        }
        if (normalizedQuery.contains(artifact.getArtifactType().toLowerCase(Locale.ROOT))) {
            matchedKeywords.add("artifact_type");
            score += 4;
        }
        return new ArtifactMatch(artifact, score, matchedKeywords.stream().distinct().toList());
    }

    private FhirKnowledgeAssistantResponse.ArtifactInsight toArtifactInsight(
            ArtifactMatch match,
            Map<String, FhirValidationCatalogProperties.FhirPackage> packageById
    ) {
        var pkg = packageById.get(match.artifact().getPackageId().toLowerCase(Locale.ROOT));
        var supportStatus = pkg == null ? "curated_reference" : pkg.getSupportStatus();
        return new FhirKnowledgeAssistantResponse.ArtifactInsight(
                match.artifact().getArtifactId(),
                match.artifact().getTitle(),
                match.artifact().getArtifactType(),
                match.artifact().getCanonicalUrl(),
                match.artifact().getVersion(),
                match.artifact().getPackageId(),
                match.artifact().getSourceId(),
                match.artifact().getSourceVersion(),
                supportStatus,
                explanation(match.artifact(), supportStatus),
                match.artifact().getSupportBoundary(),
                match.artifact().getEvidenceLinks(),
                match.matchedKeywords()
        );
    }

    private String explanation(StandardsArtifactProperties.Artifact artifact, String supportStatus) {
        return switch (artifact.getArtifactType()) {
            case "resource" -> "Curated base or guide-level resource reference for workflow modeling. Review implementation-guide and counterparty applicability before use. Current support status: " + supportStatus + ".";
            case "profile" -> "Curated profile reference that can guide field expectations, but deployment-specific support still requires human review. Current support status: " + supportStatus + ".";
            case "operation" -> "Curated workflow or operation touchpoint reference. Treat it as discovery guidance, not as a guarantee that a trading partner supports the operation. Current support status: " + supportStatus + ".";
            case "implementation_guide" -> "Curated implementation-guide reference that can frame likely standards expectations. Human review is still required before claiming applicability. Current support status: " + supportStatus + ".";
            default -> "Curated standards artifact matched the request. Human review remains required before implementation use.";
        };
    }

    private FhirKnowledgeAssistantResponse.PackageInsight toPackageInsight(FhirValidationCatalogProperties.FhirPackage pkg) {
        return new FhirKnowledgeAssistantResponse.PackageInsight(
                pkg.getPackageId(),
                pkg.getPackageVersion(),
                pkg.getPackageTitle(),
                pkg.getPackageKind(),
                pkg.getSupportStatus(),
                pkg.getValidationBoundary(),
                pkg.getPackageEvidenceLink(),
                pkg.getProfiles().stream()
                        .map(profile -> new FhirKnowledgeAssistantResponse.ProfileInsight(
                                profile.getProfileUrl(),
                                profile.getProfileTitle(),
                                profile.getSupportStatus(),
                                profile.getValidationScope(),
                                profile.getProfileEvidenceLink()
                        ))
                        .toList()
        );
    }

    private record ArtifactMatch(
            StandardsArtifactProperties.Artifact artifact,
            int score,
            List<String> matchedKeywords
    ) {
    }
}
