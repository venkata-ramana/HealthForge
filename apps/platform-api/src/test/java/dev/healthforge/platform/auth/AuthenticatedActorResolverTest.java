package dev.healthforge.platform.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AuthenticatedActorResolverTest {

    private final AuthenticatedActor reviewer = new AuthenticatedActor("local.reviewer", ActorRole.REVIEWER, "tenant.alpha", "local_header");
    private final AuthenticatedActor administrator = new AuthenticatedActor("local.admin", ActorRole.ADMINISTRATOR, "tenant.alpha", "local_header");
    private final AuthenticatedActor approver = new AuthenticatedActor("local.approver", ActorRole.APPROVER, "tenant.alpha", "local_header");
    private final AuthenticatedActor auditor = new AuthenticatedActor("local.auditor", ActorRole.AUDITOR, "tenant.alpha", "local_header");

    @Test
    void delegatesRequiredResolutionToConfiguredProvider() {
        var resolver = new AuthenticatedActorResolver(new StubActorProvider(reviewer, null), mock(AuthenticatedActorRegistry.class));

        var actor = resolver.requireWriteActor(new MockHttpServletRequest());

        assertThat(actor).isEqualTo(reviewer);
    }

    @Test
    void resolvesOptionalActorThroughProvider() {
        var resolver = new AuthenticatedActorResolver(new StubActorProvider(reviewer, approver), mock(AuthenticatedActorRegistry.class));

        var actor = resolver.resolveOptionalActor(new MockHttpServletRequest());

        assertThat(actor).isEqualTo(approver);
    }

    @Test
    void rejectsAdministratorOnlyActionForReviewer() {
        var resolver = new AuthenticatedActorResolver(new StubActorProvider(reviewer, null), mock(AuthenticatedActorRegistry.class));

        assertThatThrownBy(() -> resolver.requireAdministrator(new MockHttpServletRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("administrator role");
    }

    @Test
    void allowsApproverForApprovalActions() {
        var resolver = new AuthenticatedActorResolver(new StubActorProvider(approver, null), mock(AuthenticatedActorRegistry.class));

        var actor = resolver.requireApproverOrAdministrator(new MockHttpServletRequest());

        assertThat(actor).isEqualTo(approver);
    }

    @Test
    void allowsAuditorForAuditActions() {
        var resolver = new AuthenticatedActorResolver(new StubActorProvider(auditor, null), mock(AuthenticatedActorRegistry.class));

        var actor = resolver.requireAuditorOrAdministrator(new MockHttpServletRequest());

        assertThat(actor).isEqualTo(auditor);
    }

    @Test
    void allowsAdministratorForAdministrativeActions() {
        var resolver = new AuthenticatedActorResolver(new StubActorProvider(administrator, null), mock(AuthenticatedActorRegistry.class));

        var actor = resolver.requireAdministrator(new MockHttpServletRequest());

        assertThat(actor).isEqualTo(administrator);
    }

    private record StubActorProvider(AuthenticatedActor requiredActor, AuthenticatedActor optionalActor) implements AuthenticatedActorProvider {

        @Override
        public AuthenticatedActor resolveRequiredActor(jakarta.servlet.http.HttpServletRequest request) {
            return requiredActor;
        }

        @Override
        public AuthenticatedActor resolveOptionalActor(jakarta.servlet.http.HttpServletRequest request) {
            return optionalActor;
        }

        @Override
        public String authenticationMode() {
            return "stub";
        }
    }
}
