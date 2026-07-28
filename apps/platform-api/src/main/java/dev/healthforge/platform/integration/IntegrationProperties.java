package dev.healthforge.platform.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "healthforge.integrations")
public class IntegrationProperties {

    private final Connector github = new Connector();
    private final Connector jira = new Connector();
    private final Connector notion = new Connector();
    private final Connector sharepoint = new Connector();
    private final Connector confluence = new Connector();

    public Connector getGithub() { return github; }
    public Connector getJira() { return jira; }
    public Connector getNotion() { return notion; }
    public Connector getSharepoint() { return sharepoint; }
    public Connector getConfluence() { return confluence; }

    public Connector connector(String name) {
        return switch (name) {
            case "github" -> github;
            case "jira" -> jira;
            case "notion" -> notion;
            case "sharepoint" -> sharepoint;
            case "confluence" -> confluence;
            default -> null;
        };
    }

    public Map<String, Connector> all() {
        var connectors = new LinkedHashMap<String, Connector>();
        connectors.put("github", github);
        connectors.put("jira", jira);
        connectors.put("notion", notion);
        connectors.put("sharepoint", sharepoint);
        connectors.put("confluence", confluence);
        return connectors;
    }

    public static class Connector {
        private boolean enabled = false;
        private String executionMode = "simulated";
        private boolean allowLiveCalls = false;
        private String baseUrl = "";
        private String credentialReference = "";
        private String projectKey = "";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getExecutionMode() { return executionMode; }
        public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
        public boolean isAllowLiveCalls() { return allowLiveCalls; }
        public void setAllowLiveCalls(boolean allowLiveCalls) { this.allowLiveCalls = allowLiveCalls; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getCredentialReference() { return credentialReference; }
        public void setCredentialReference(String credentialReference) { this.credentialReference = credentialReference; }
        public String getProjectKey() { return projectKey; }
        public void setProjectKey(String projectKey) { this.projectKey = projectKey; }
    }
}
