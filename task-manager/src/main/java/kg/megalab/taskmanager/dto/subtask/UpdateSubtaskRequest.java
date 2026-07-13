package kg.megalab.taskmanager.dto.subtask;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateSubtaskRequest(String title, UUID assigneeId, LocalDate deadline) {
}
