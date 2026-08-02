package dev.healthforge.platform.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TenantMemberInvitationRequest(
        @NotBlank String actorUserId,
        @NotBlank String displayName,
        @NotBlank String authSubject,
        @NotBlank String identityMode,
        @NotEmpty List<@NotBlank String> roles
) {
}
