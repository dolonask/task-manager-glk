package kg.megalab.taskmanager.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank(message = "Поле \"title\" обязательно для заполнения") String title,
        String description,
        @NotNull(message = "Поле \"departmentId\" обязательно для заполнения") UUID departmentId,
        UUID assigneeId,
        @NotNull(message = "Поле \"priority\" обязательно для заполнения") String priority,
        @NotNull(message = "Поле \"initialDeadline\" обязательно для заполнения") LocalDate initialDeadline
) {
}
