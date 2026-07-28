package dev.healthforge.platform.collaboration;

import dev.healthforge.platform.auth.ActorRole;
import dev.healthforge.platform.auth.AuthenticatedActor;
import dev.healthforge.platform.automation.WorkflowAutomationService;
import dev.healthforge.platform.brief.BriefResponse;
import dev.healthforge.platform.brief.BriefService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CollaborationNotificationServiceTest {

    @Test
    void blocksGovernedSendWithoutApproval() {
        var briefService = mock(BriefService.class);
        var jdbcTemplate = mock(JdbcTemplate.class);
        var automationService = mock(WorkflowAutomationService.class);
        var service = new CollaborationNotificationService(briefService, jdbcTemplate, automationService);

        when(briefService.get(eq("brief-1"), any())).thenReturn(new BriefResponse(
                "brief-1",
                "in_review",
                Instant.parse("2026-07-28T10:00:00Z"),
                new BriefResponse.Input("What changes do we need?", "Synthetic planning", "corpus", "v1"),
                List.of(),
                List.of(),
                "Summary",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                true
        ));

        var response = service.notify(new CollaborationNotificationRequest(
                "brief-1",
                "slack",
                "approval_needed",
                null,
                true,
                true,
                null,
                "#pa-approvals",
                "Alert the approvers."
        ), new AuthenticatedActor("approver.one", ActorRole.APPROVER, "tenant.alpha", "local_header"));

        assertThat(response.deliveryStatus()).isEqualTo("notification_blocked");
        assertThat(response.reviewNotice()).contains("blocked");
    }
}
