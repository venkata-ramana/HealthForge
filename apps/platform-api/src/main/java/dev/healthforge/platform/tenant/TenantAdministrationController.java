package dev.healthforge.platform.tenant;

import dev.healthforge.platform.auth.AuthenticatedActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/tenants")
public class TenantAdministrationController {

    private final TenantAdministrationService service;
    private final AuthenticatedActorResolver actorResolver;

    public TenantAdministrationController(
            TenantAdministrationService service,
            AuthenticatedActorResolver actorResolver
    ) {
        this.service = service;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/overview")
    public TenantAdministrationOverviewResponse overview(HttpServletRequest request) {
        return service.overview(actorResolver.requireAdministrator(request));
    }

    @GetMapping("/analytics")
    public TenantAnalyticsResponse analytics(HttpServletRequest request) {
        return service.analytics(actorResolver.requireAdministrator(request));
    }

    @PostMapping("/provisioning-requests")
    public TenantProvisioningResponse createProvisioningRequest(
            @Valid @RequestBody TenantProvisioningRequest provisioningRequest,
            HttpServletRequest request
    ) {
        return service.createProvisioningRequest(provisioningRequest, actorResolver.requireAdministrator(request));
    }

    @GetMapping("/provisioning-requests")
    public List<TenantProvisioningResponse> listProvisioningRequests(HttpServletRequest request) {
        return service.listProvisioningRequests(actorResolver.requireAdministrator(request));
    }

    @GetMapping("/members")
    public List<TenantMemberResponse> members(HttpServletRequest request) {
        return service.members(actorResolver.requireAdministrator(request));
    }

    @PostMapping("/member-invitations")
    public TenantMemberResponse inviteMember(
            @Valid @RequestBody TenantMemberInvitationRequest invitation,
            HttpServletRequest request
    ) {
        return service.inviteMember(invitation, actorResolver.requireAdministrator(request));
    }
}
