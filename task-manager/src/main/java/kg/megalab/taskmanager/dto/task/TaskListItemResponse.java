package kg.megalab.taskmanager.dto.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskListItemResponse(
        UUID id,
        String title,
        String description,
        UUID creatorId,
        String creatorName,
        UUID departmentId,
        String departmentName,
        UUID assigneeId,
        String assigneeName,
        String priority,
        LocalDate initialDeadline,
        LocalDate currentDeadline,
        String status,
        double progress,
        Instant createdAt,
        Instant closedAt
) {
}
