package dev.healthforge.platform.collaboration;

import jakarta.validation.constraints.NotBlank;

public record CollaborationNotificationRequest(
        @NotBlank String briefId,
        @NotBlank String targetSystem,
        @NotBlank String notificationType,
        String handoffRole,
        boolean approvalAcknowledgement,
        boolean sendRequested,
        String approvalId,
        String targetLocator,
        String deliveryReason
) {
}
