package kg.megalab.taskmanager.dto.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogEntryResponse(
        UUID id,
        Instant ts,
        UUID actorId,
        String actorName,
        String entityType,
        UUID entityId,
        String action,
        Object before,
        Object after
) {
}
