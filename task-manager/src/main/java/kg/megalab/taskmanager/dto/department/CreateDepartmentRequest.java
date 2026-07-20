package kg.megalab.taskmanager.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDepartmentRequest(
        @NotBlank(message = "Поле \"name\" обязательно для заполнения") String name,
        @NotNull(message = "Поле \"curatorId\" обязательно для заполнения") UUID curatorId,
        UUID headId
) {
}
