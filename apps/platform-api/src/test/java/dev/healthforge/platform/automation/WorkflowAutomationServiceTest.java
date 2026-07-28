package dev.healthforge.platform.automation;

import dev.healthforge.platform.auth.ActorRole;
import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.brief.BriefAuditEventService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowAutomationServiceTest {

    @Test
    void dispatchesWebhookEventsAgainstEnabledSubscriptions() {
        var jdbcTemplate = mock(JdbcTemplate.class);
        var auditEventService = mock(BriefAuditEventService.class);
        var service = new WorkflowAutomationService(jdbcTemplate, auditEventService);

        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(new WorkflowAutomationService.Subscription(
                        "review",
                        "decision_recorded",
                        "private_demo",
                        "slack-review-queue",
                        "governed_send"
                )));

        var response = service.dispatch(new WorkflowAutomationDispatchRequest(
                "brief-1",
                "review",
                "decision_recorded",
                "Review decision recorded.",
                "private_demo",
                true,
                null
        ), new AuthenticatedActor("admin.one", ActorRole.ADMINISTRATOR, "tenant.alpha", "local_header"));

        assertThat(response.eventFamily()).isEqualTo("review");
        assertThat(response.deliveries()).hasSize(1);
        assertThat(response.deliveries().getFirst().deliveryStatus()).isEqualTo("delivered");
    }
}
