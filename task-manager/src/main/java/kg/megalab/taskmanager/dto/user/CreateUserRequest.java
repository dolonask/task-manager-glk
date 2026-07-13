package kg.megalab.taskmanager.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank(message = "Поле \"fullName\" обязательно для заполнения") String fullName,
        @NotBlank(message = "Поле \"login\" обязательно для заполнения") String login,
        @NotBlank(message = "Поле \"password\" обязательно для заполнения") String password,
        @NotNull(message = "Поле \"role\" обязательно для заполнения") String role,
        UUID departmentId
) {
}
