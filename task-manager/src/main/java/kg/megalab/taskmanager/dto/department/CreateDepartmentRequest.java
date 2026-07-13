package kg.megalab.taskmanager.dto.department;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateDepartmentRequest(
        @NotBlank(message = "Поле \"name\" обязательно для заполнения") String name,
        UUID curatorId,
        UUID headId
) {
}
