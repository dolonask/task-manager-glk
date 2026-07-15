package kg.megalab.taskmanager.dto.subtask;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record CreateSubtaskRequest(
        @NotBlank(message = "Поле \"title\" обязательно для заполнения") String title,
        String description,
        UUID assigneeId,
        LocalDate deadline
) {
}
