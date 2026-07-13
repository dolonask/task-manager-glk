package kg.megalab.taskmanager.dto.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String message,
        String entityType,
        UUID entityId,
        boolean read,
        Instant createdAt
) {
}
