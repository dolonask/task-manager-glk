package kg.megalab.taskmanager.dto.subtask;

import java.time.LocalDate;
import java.util.UUID;

public record SubtaskResponse(
        UUID id,
        UUID taskId,
        String title,
        UUID assigneeId,
        String assigneeName,
        LocalDate deadline,
        boolean done
) {
}
