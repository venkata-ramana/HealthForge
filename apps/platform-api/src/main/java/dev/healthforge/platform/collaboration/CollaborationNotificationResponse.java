package dev.healthforge.platform.collaboration;

import java.time.Instant;
import java.util.List;

public record CollaborationNotificationResponse(
        String notificationEventId,
        String briefId,
        String targetSystem,
        String notificationType,
        String deliveryMode,
        Instant createdAt,
        String handoffRole,
        String deliveryStatus,
        String messageSummary,
        List<String> safeMessageBullets,
        String externalReference,
        String reviewNotice
) {
}
