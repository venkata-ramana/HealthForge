# Phase 36 — SaaS tenant and identity hardening

Phase 36 strengthens HealthForge's tenant boundary so the platform can support hosted customer workspaces while preserving the local and private-deployment experience.

## What shipped

- Added an explicit `healthforge.auth.enforce-membership` setting, configurable with `HEALTHFORGE_AUTH_ENFORCE_MEMBERSHIP`.
- Added active organization-membership and role-assignment checks to authenticated write and optional-actor flows when enforcement is enabled.
- Preserved local-header demo mode by keeping enforcement disabled by default for local development.
- Added administrator tenant member listing at `GET /v1/admin/tenants/members`.
- Added administrator member invitations at `POST /v1/admin/tenants/member-invitations`.
- Invitations are recorded as `invited` memberships and carry organization-scoped role assignments for later identity-provider activation.
- Added unit and integration coverage for membership enforcement and tenant-scoped member administration.

## Example

Enable the stronger boundary in a trusted identity deployment:

```bash
export HEALTHFORGE_AUTH_MODE=trusted_proxy
export HEALTHFORGE_AUTH_ENFORCE_MEMBERSHIP=true
```

An administrator can create an invitation without granting active access:

```http
POST /v1/admin/tenants/member-invitations
X-HealthForge-Actor: tenant.admin
X-HealthForge-Role: administrator
X-HealthForge-Organization: org.example
Content-Type: application/json

{
  "actor_user_id": "reviewer.alex",
  "display_name": "Alex Reviewer",
  "auth_subject": "alex@example.test",
  "identity_mode": "trusted_proxy",
  "roles": ["reviewer"]
}
```

The invitation is visible to tenant administrators, but the actor cannot use protected workflows until an identity/provisioning process activates the membership.

## Boundary and limitations

This phase provides an application-level tenant boundary. It does not claim to provide a complete SaaS control plane, automated infrastructure provisioning, billing, SSO, or compliance certification. Those remain follow-on work for enterprise identity, provisioning, commercial packaging, and production launch gates.
