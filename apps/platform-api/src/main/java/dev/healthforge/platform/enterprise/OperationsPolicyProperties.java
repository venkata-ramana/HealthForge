package dev.healthforge.platform.enterprise;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "healthforge.operations")
public class OperationsPolicyProperties {

    private String deploymentTier = "private_demo";
    private final List<EnvironmentPolicy> environments = new ArrayList<>();
    private final List<ConfigBoundary> configBoundaries = new ArrayList<>();
    private final List<SecretReference> secretReferences = new ArrayList<>();
    private final List<String> observabilityRunbooks = new ArrayList<>();
    private final List<String> retentionChecks = new ArrayList<>();
    private final List<String> continuityChecks = new ArrayList<>();
    private final List<QuotaPolicy> quotas = new ArrayList<>();
    private final List<String> expectedAttestations = new ArrayList<>();

    public String getDeploymentTier() {
        return deploymentTier;
    }

    public void setDeploymentTier(String deploymentTier) {
        this.deploymentTier = deploymentTier;
    }

    public List<EnvironmentPolicy> getEnvironments() {
        return environments;
    }

    public List<ConfigBoundary> getConfigBoundaries() {
        return configBoundaries;
    }

    public List<SecretReference> getSecretReferences() {
        return secretReferences;
    }

    public List<String> getObservabilityRunbooks() {
        return observabilityRunbooks;
    }

    public List<String> getRetentionChecks() {
        return retentionChecks;
    }

    public List<String> getContinuityChecks() {
        return continuityChecks;
    }

    public List<QuotaPolicy> getQuotas() {
        return quotas;
    }

    public List<String> getExpectedAttestations() {
        return expectedAttestations;
    }

    public static class EnvironmentPolicy {
        private String environmentName = "";
        private String promotionGate = "";
        private String secretBoundary = "";
        private String changeWindow = "";
        private String dataBoundary = "";

        public String getEnvironmentName() {
            return environmentName;
        }

        public void setEnvironmentName(String environmentName) {
            this.environmentName = environmentName;
        }

        public String getPromotionGate() {
            return promotionGate;
        }

        public void setPromotionGate(String promotionGate) {
            this.promotionGate = promotionGate;
        }

        public String getSecretBoundary() {
            return secretBoundary;
        }

        public void setSecretBoundary(String secretBoundary) {
            this.secretBoundary = secretBoundary;
        }

        public String getChangeWindow() {
            return changeWindow;
        }

        public void setChangeWindow(String changeWindow) {
            this.changeWindow = changeWindow;
        }

        public String getDataBoundary() {
            return dataBoundary;
        }

        public void setDataBoundary(String dataBoundary) {
            this.dataBoundary = dataBoundary;
        }
    }

    public static class ConfigBoundary {
        private String key = "";
        private String classification = "";
        private String source = "";
        private String exposurePolicy = "";
        private String rationale = "";

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getClassification() {
            return classification;
        }

        public void setClassification(String classification) {
            this.classification = classification;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getExposurePolicy() {
            return exposurePolicy;
        }

        public void setExposurePolicy(String exposurePolicy) {
            this.exposurePolicy = exposurePolicy;
        }

        public String getRationale() {
            return rationale;
        }

        public void setRationale(String rationale) {
            this.rationale = rationale;
        }
    }

    public static class SecretReference {
        private String system = "";
        private String reference = "";
        private String rotationExpectation = "";
        private String usageBoundary = "";

        public String getSystem() {
            return system;
        }

        public void setSystem(String system) {
            this.system = system;
        }

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }

        public String getRotationExpectation() {
            return rotationExpectation;
        }

        public void setRotationExpectation(String rotationExpectation) {
            this.rotationExpectation = rotationExpectation;
        }

        public String getUsageBoundary() {
            return usageBoundary;
        }

        public void setUsageBoundary(String usageBoundary) {
            this.usageBoundary = usageBoundary;
        }
    }

    public static class QuotaPolicy {
        private String metric = "";
        private int softLimit = 0;
        private String window = "";
        private String rationale = "";

        public String getMetric() {
            return metric;
        }

        public void setMetric(String metric) {
            this.metric = metric;
        }

        public int getSoftLimit() {
            return softLimit;
        }

        public void setSoftLimit(int softLimit) {
            this.softLimit = softLimit;
        }

        public String getWindow() {
            return window;
        }

        public void setWindow(String window) {
            this.window = window;
        }

        public String getRationale() {
            return rationale;
        }

        public void setRationale(String rationale) {
            this.rationale = rationale;
        }
    }
}
