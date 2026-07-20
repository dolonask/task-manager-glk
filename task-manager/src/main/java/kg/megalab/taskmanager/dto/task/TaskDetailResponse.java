package kg.megalab.taskmanager.dto.task;

import kg.megalab.taskmanager.dto.subtask.SubtaskResponse;
import kg.megalab.taskmanager.dto.transfer.TransferRequestResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskDetailResponse(
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
        Instant closedAt,
        String closeComment,
        List<SubtaskResponse> subtasks,
        List<TransferRequestResponse> transferRequests
) {
}
