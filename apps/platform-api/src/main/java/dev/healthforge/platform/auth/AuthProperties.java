package dev.healthforge.platform.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "healthforge.auth")
public class AuthProperties {

    private String mode = "local_header";
    private String defaultOrganizationId = "local.default";
    private String trustedSubjectHeader = "X-HealthForge-Subject";
    private String trustedDisplayNameHeader = "X-HealthForge-Display-Name";
    private String trustedGroupsHeader = "X-HealthForge-Groups";
    private boolean enforceMembership = false;
    private List<GroupRoleMapping> groupRoleMappings = new ArrayList<>();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDefaultOrganizationId() {
        return defaultOrganizationId;
    }

    public void setDefaultOrganizationId(String defaultOrganizationId) {
        this.defaultOrganizationId = defaultOrganizationId;
    }

    public String getTrustedSubjectHeader() {
        return trustedSubjectHeader;
    }

    public void setTrustedSubjectHeader(String trustedSubjectHeader) {
        this.trustedSubjectHeader = trustedSubjectHeader;
    }

    public String getTrustedDisplayNameHeader() {
        return trustedDisplayNameHeader;
    }

    public void setTrustedDisplayNameHeader(String trustedDisplayNameHeader) {
        this.trustedDisplayNameHeader = trustedDisplayNameHeader;
    }

    public String getTrustedGroupsHeader() {
        return trustedGroupsHeader;
    }

    public void setTrustedGroupsHeader(String trustedGroupsHeader) {
        this.trustedGroupsHeader = trustedGroupsHeader;
    }

    public boolean isEnforceMembership() {
        return enforceMembership;
    }

    public void setEnforceMembership(boolean enforceMembership) {
        this.enforceMembership = enforceMembership;
    }

    public List<GroupRoleMapping> getGroupRoleMappings() {
        return groupRoleMappings;
    }

    public void setGroupRoleMappings(List<GroupRoleMapping> groupRoleMappings) {
        this.groupRoleMappings = groupRoleMappings;
    }

    public static class GroupRoleMapping {
        private String group = "";
        private String role = "";
        private String scope = "";

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }
}
