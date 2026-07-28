package dev.healthforge.platform.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

@Component
@ConditionalOnProperty(name = "healthforge.auth.mode", havingValue = "trusted_proxy")
public class TrustedProxyAuthenticatedActorProvider implements AuthenticatedActorProvider {

    private final AuthProperties properties;

    public TrustedProxyAuthenticatedActorProvider(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public AuthenticatedActor resolveRequiredActor(HttpServletRequest request) {
        var subject = requiredHeader(request, properties.getTrustedSubjectHeader());
        var role = resolveRole(request);
        var actorId = optionalHeader(request, LocalHeaderAuthenticatedActorProvider.ACTOR_ID_HEADER);
        var displayName = optionalHeader(request, properties.getTrustedDisplayNameHeader());
        var organizationId = optionalHeader(request, LocalHeaderAuthenticatedActorProvider.ACTOR_ORG_HEADER);
        return new AuthenticatedActor(
                actorId != null ? actorId : (displayName != null ? displayName : subject),
                role,
                organizationId == null ? properties.getDefaultOrganizationId() : organizationId,
                authenticationMode()
        );
    }

    @Override
    public AuthenticatedActor resolveOptionalActor(HttpServletRequest request) {
        var subject = optionalHeader(request, properties.getTrustedSubjectHeader());
        if (subject == null) {
            return null;
        }
        return resolveRequiredActor(request);
    }

    @Override
    public String authenticationMode() {
        return "trusted_proxy";
    }

    private ActorRole resolveRole(HttpServletRequest request) {
        var groupsHeader = requiredHeader(request, properties.getTrustedGroupsHeader());
        return Arrays.stream(groupsHeader.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::toMappedRole)
                .filter(role -> role != null)
                .max(Comparator.comparingInt(this::rank))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Trusted proxy identity did not include any mapped groups for reviewer, approver, auditor, or administrator access."
                ));
    }

    private ActorRole toMappedRole(String groupName) {
        return properties.getGroupRoleMappings().stream()
                .filter(mapping -> mapping.getGroup() != null && mapping.getGroup().equalsIgnoreCase(groupName))
                .findFirst()
                .map(mapping -> ActorRole.parse(mapping.getRole()))
                .orElse(null);
    }

    private int rank(ActorRole role) {
        return switch (role) {
            case REVIEWER -> 1;
            case APPROVER -> 2;
            case AUDITOR -> 3;
            case ADMINISTRATOR -> 4;
        };
    }

    private String requiredHeader(HttpServletRequest request, String name) {
        var value = optionalHeader(request, name);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Configured trusted-proxy authentication requires " + name + " and mapped groups.");
        }
        return value;
    }

    private String optionalHeader(HttpServletRequest request, String name) {
        var value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
