package kg.megalab.taskmanager.dto.task;

import jakarta.validation.constraints.NotBlank;

public record CloseTaskRequest(
        @NotBlank(message = "Поле \"comment\" обязательно для заполнения") String comment
) {
}
