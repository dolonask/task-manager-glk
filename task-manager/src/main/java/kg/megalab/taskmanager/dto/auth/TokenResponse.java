package kg.megalab.taskmanager.dto.auth;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
