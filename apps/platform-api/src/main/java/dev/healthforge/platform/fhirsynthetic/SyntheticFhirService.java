package dev.healthforge.platform.fhirsynthetic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.healthforge.platform.ingestion.WorkspaceProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SyntheticFhirService {

    private final ObjectMapper objectMapper;
    private final WorkspaceProperties workspaceProperties;
    private final Map<String, ScenarioDefinition> scenarios = new LinkedHashMap<>();

    public SyntheticFhirService(ObjectMapper objectMapper, WorkspaceProperties workspaceProperties) {
        this.objectMapper = objectMapper;
        this.workspaceProperties = workspaceProperties;
        scenarios.put("prior_auth_claim_valid", new ScenarioDefinition(
                "prior_auth_claim_valid",
                "Prior authorization claim (valid)",
                "Synthetic Claim example for a prior-authorization planning workflow.",
                "knowledge/fixtures/fhir-validation/prior-auth-claim-valid.request.json",
                "valid"
        ));
        scenarios.put("prior_auth_claim_invalid_missing_status", new ScenarioDefinition(
                "prior_auth_claim_invalid_missing_status",
                "Prior authorization claim (invalid: missing status)",
                "Synthetic Claim example intentionally missing status to demonstrate validation findings.",
                "knowledge/fixtures/fhir-validation/prior-auth-claim-invalid-missing-status.request.json",
                "invalid"
        ));
        scenarios.put("prior_auth_coverage_valid", new ScenarioDefinition(
                "prior_auth_coverage_valid",
                "Coverage example (valid)",
                "Synthetic Coverage example for eligibility and authorization workflow demos.",
                "knowledge/fixtures/fhir-validation/prior-auth-coverage-valid.request.json",
                "valid"
        ));
        scenarios.put("prior_auth_bundle_valid", new ScenarioDefinition(
                "prior_auth_bundle_valid",
                "Prior authorization bundle (valid)",
                "Synthetic multi-resource prior-authorization bundle for workflow and validation demos.",
                "knowledge/fixtures/fhir-validation/prior-auth-bundle-valid.request.json",
                "valid"
        ));
        scenarios.put("prior_auth_bundle_invalid_missing_type", new ScenarioDefinition(
                "prior_auth_bundle_invalid_missing_type",
                "Prior authorization bundle (invalid: missing type)",
                "Synthetic Bundle example intentionally missing bundle type for validation demos.",
                "knowledge/fixtures/fhir-validation/prior-auth-bundle-invalid-missing-type.request.json",
                "invalid"
        ));
    }

    public SyntheticFhirCatalogResponse catalog() {
        return new SyntheticFhirCatalogResponse(
                scenarios.values().stream().map(definition -> new SyntheticFhirCatalogResponse.SyntheticScenario(
                        definition.scenarioId(),
                        definition.title(),
                        definition.description(),
                        definition.profileUrl(),
                        definition.packageId(),
                        definition.packageVersion(),
                        definition.expectedStatus()
                )).toList()
        );
    }

    public SyntheticFhirGenerateResponse generate(SyntheticFhirGenerateRequest request) {
        var definition = scenarios.get(request.scenarioId());
        if (definition == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown synthetic FHIR scenario");
        }
        var validationRequest = loadFixture(definition.relativePath());
        var resource = validationRequest.get("resource");
        return new SyntheticFhirGenerateResponse(
                definition.scenarioId(),
                definition.title(),
                definition.description(),
                definition.expectedStatus(),
                validationRequest.path("data_classification").asText("synthetic"),
                new SyntheticFhirGenerateResponse.ValidationRecommendation(
                        validationRequest.path("package_id").asText(),
                        validationRequest.path("package_version").asText(),
                        validationRequest.path("profile_url").asText(),
                        "/v1/fhir-validation/validate"
                ),
                validationRequest,
                resource,
                List.of(
                        "This generator emits only synthetic, non-PHI examples stored in the repository fixture set.",
                        "The returned payload can be sent directly to the FHIR validation endpoint for deterministic local validation."
                )
        );
    }

    private JsonNode loadFixture(String relativePath) {
        var root = workspaceProperties.rootDirectory();
        if (root == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Workspace root is not configured");
        }
        var path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root.normalize()) || !Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Synthetic FHIR fixture is missing");
        }
        try {
            return objectMapper.readTree(path.toFile());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Synthetic FHIR fixture could not be parsed");
        }
    }

    private record ScenarioDefinition(
            String scenarioId,
            String title,
            String description,
            String relativePath,
            String expectedStatus
    ) {
        String packageId() {
            return loadString("package_id");
        }

        String packageVersion() {
            return loadString("package_version");
        }

        String profileUrl() {
            return loadString("profile_url");
        }

        private String loadString(String field) {
            return switch (scenarioId) {
                case "prior_auth_claim_valid" -> switch (field) {
                    case "package_id" -> "hl7.fhir.r4.core";
                    case "package_version" -> "4.0.1";
                    case "profile_url" -> "http://hl7.org/fhir/StructureDefinition/Claim";
                    default -> "";
                };
                case "prior_auth_claim_invalid_missing_status" -> switch (field) {
                    case "package_id" -> "hl7.fhir.r4.core";
                    case "package_version" -> "4.0.1";
                    case "profile_url" -> "http://hl7.org/fhir/StructureDefinition/Claim";
                    default -> "";
                };
                case "prior_auth_coverage_valid" -> switch (field) {
                    case "package_id" -> "hl7.fhir.r4.core";
                    case "package_version" -> "4.0.1";
                    case "profile_url" -> "http://hl7.org/fhir/StructureDefinition/Coverage";
                    default -> "";
                };
                case "prior_auth_bundle_valid" -> switch (field) {
                    case "package_id" -> "hl7.fhir.r4.core";
                    case "package_version" -> "4.0.1";
                    case "profile_url" -> "http://hl7.org/fhir/StructureDefinition/Bundle";
                    default -> "";
                };
                case "prior_auth_bundle_invalid_missing_type" -> switch (field) {
                    case "package_id" -> "hl7.fhir.r4.core";
                    case "package_version" -> "4.0.1";
                    case "profile_url" -> "http://hl7.org/fhir/StructureDefinition/Bundle";
                    default -> "";
                };
                default -> "";
            };
        }
    }
}
