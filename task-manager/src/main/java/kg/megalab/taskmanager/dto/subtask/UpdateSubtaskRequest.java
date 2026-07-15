package kg.megalab.taskmanager.dto.subtask;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateSubtaskRequest(String title, String description, UUID assigneeId, LocalDate deadline) {
}
