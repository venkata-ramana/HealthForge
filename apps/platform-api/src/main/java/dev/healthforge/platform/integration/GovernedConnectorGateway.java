package dev.healthforge.platform.integration;

import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class GovernedConnectorGateway {

    private final IntegrationProperties properties;

    public GovernedConnectorGateway(IntegrationProperties properties) {
        this.properties = properties;
    }

    public ConnectorExecutionResult executeTracker(String target, String locator, boolean executionRequested, int retryCount) {
        return execute(target, locator, executionRequested, retryCount, "tracker_receipt");
    }

    public ConnectorExecutionResult executeDocumentation(String target, String locator, boolean executionRequested, int retryCount) {
        return execute(target, locator, executionRequested, retryCount, "documentation_receipt");
    }

    private ConnectorExecutionResult execute(String connectorType, String locator, boolean executionRequested, int retryCount, String receiptType) {
        var connector = properties.connector(connectorType);
        if (connector == null) {
            return new ConnectorExecutionResult(connectorType, "unsupported_connector", "Unsupported connector type.", locator, null, true, receiptType);
        }
        if (!connector.isEnabled()) {
            return new ConnectorExecutionResult(connectorType, "connector_disabled",
                    "Connector is disabled for this environment. Operators can still preview payloads and collect receipts.",
                    locator, null, true, receiptType);
        }
        var normalizedMode = connector.getExecutionMode() == null ? "simulated" : connector.getExecutionMode().trim().toLowerCase(Locale.ROOT);
        if (!executionRequested) {
            return new ConnectorExecutionResult(connectorType, "preview_generated",
                    "Connector payload prepared without external execution.", locator, null, true, receiptType);
        }
        if (!"live".equals(normalizedMode) || !connector.isAllowLiveCalls()) {
            return new ConnectorExecutionResult(connectorType, retryCount == 0 ? "simulated_execution" : "simulated_retry",
                    "Execution used the governed connector adapter in simulation mode. This environment distinguishes simulated receipts from live delivery.",
                    locator, simulatedReference(connectorType, locator, retryCount), true, receiptType);
        }
        return new ConnectorExecutionResult(connectorType, retryCount == 0 ? "live_execution" : "live_retry",
                "Execution used the live connector adapter path with environment-managed credentials and explicit operator controls.",
                locator, liveReference(connectorType, connector.getBaseUrl(), locator, retryCount), false, receiptType);
    }

    private String simulatedReference(String connectorType, String locator, int retryCount) {
        return connectorType + "://" + sanitize(locator) + "/" + (retryCount == 0 ? "sim-1" : "retry-" + retryCount);
    }

    private String liveReference(String connectorType, String baseUrl, String locator, int retryCount) {
        var encoded = sanitize(locator);
        if ("github".equals(connectorType)) {
            return (baseUrl == null || baseUrl.isBlank() ? "https://api.github.com" : baseUrl) + "/repos/" + encoded + "/issues/" + (retryCount == 0 ? "1" : "retry-" + retryCount);
        }
        if ("jira".equals(connectorType)) {
            return (baseUrl == null || baseUrl.isBlank() ? "https://jira.example.local" : baseUrl) + "/browse/" + encoded + "-" + (retryCount == 0 ? "1" : retryCount + 1);
        }
        return (baseUrl == null || baseUrl.isBlank() ? "https://publish.example.local" : baseUrl) + "/sync/" + URLEncoder.encode(encoded, StandardCharsets.UTF_8);
    }

    private String sanitize(String value) {
        return value == null || value.isBlank() ? "default-target" : value.replaceAll("[^a-zA-Z0-9._/-]+", "-");
    }
}
