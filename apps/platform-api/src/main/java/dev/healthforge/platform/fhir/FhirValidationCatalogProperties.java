package dev.healthforge.platform.fhir;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "healthforge.fhir.validation")
public class FhirValidationCatalogProperties {

    private List<FhirPackage> packages = new ArrayList<>();

    public List<FhirPackage> getPackages() {
        return packages;
    }

    public void setPackages(List<FhirPackage> packages) {
        this.packages = packages;
    }

    public static class FhirPackage {
        private String packageId;
        private String packageVersion;
        private String packageTitle;
        private String packageKind;
        private String supportStatus;
        private String validationBoundary;
        private String packageEvidenceLink;
        private List<FhirProfile> profiles = new ArrayList<>();

        public String getPackageId() { return packageId; }
        public void setPackageId(String packageId) { this.packageId = packageId; }
        public String getPackageVersion() { return packageVersion; }
        public void setPackageVersion(String packageVersion) { this.packageVersion = packageVersion; }
        public String getPackageTitle() { return packageTitle; }
        public void setPackageTitle(String packageTitle) { this.packageTitle = packageTitle; }
        public String getPackageKind() { return packageKind; }
        public void setPackageKind(String packageKind) { this.packageKind = packageKind; }
        public String getSupportStatus() { return supportStatus; }
        public void setSupportStatus(String supportStatus) { this.supportStatus = supportStatus; }
        public String getValidationBoundary() { return validationBoundary; }
        public void setValidationBoundary(String validationBoundary) { this.validationBoundary = validationBoundary; }
        public String getPackageEvidenceLink() { return packageEvidenceLink; }
        public void setPackageEvidenceLink(String packageEvidenceLink) { this.packageEvidenceLink = packageEvidenceLink; }
        public List<FhirProfile> getProfiles() { return profiles; }
        public void setProfiles(List<FhirProfile> profiles) { this.profiles = profiles; }
    }

    public static class FhirProfile {
        private String profileUrl;
        private String profileTitle;
        private String supportStatus;
        private String validationScope;
        private String profileEvidenceLink;

        public String getProfileUrl() { return profileUrl; }
        public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
        public String getProfileTitle() { return profileTitle; }
        public void setProfileTitle(String profileTitle) { this.profileTitle = profileTitle; }
        public String getSupportStatus() { return supportStatus; }
        public void setSupportStatus(String supportStatus) { this.supportStatus = supportStatus; }
        public String getValidationScope() { return validationScope; }
        public void setValidationScope(String validationScope) { this.validationScope = validationScope; }
        public String getProfileEvidenceLink() { return profileEvidenceLink; }
        public void setProfileEvidenceLink(String profileEvidenceLink) { this.profileEvidenceLink = profileEvidenceLink; }
    }
}
