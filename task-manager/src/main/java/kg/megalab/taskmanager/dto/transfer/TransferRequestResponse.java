package kg.megalab.taskmanager.dto.transfer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransferRequestResponse(
        UUID id,
        UUID taskId,
        String taskTitle,
        UUID departmentId,
        String departmentName,
        UUID initiatorId,
        String initiatorName,
        UUID approverId,
        String approverName,
        LocalDate currentDeadline,
        LocalDate proposedDeadline,
        String justification,
        String status,
        String decisionReason,
        Instant submittedAt,
        Instant decidedAt,
        Instant appliedAt,
        UUID appliedById,
        String appliedByName
) {
}
