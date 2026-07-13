package kg.megalab.taskmanager.dto.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserSummaryResponse user
) {
}
