package kg.megalab.taskmanager.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Поле \"username\" обязательно для заполнения") String username,
        @NotBlank(message = "Поле \"password\" обязательно для заполнения") String password
) {
}
