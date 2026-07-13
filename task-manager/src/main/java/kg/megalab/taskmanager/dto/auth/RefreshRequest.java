package kg.megalab.taskmanager.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Поле \"refreshToken\" обязательно для заполнения") String refreshToken
) {
}
