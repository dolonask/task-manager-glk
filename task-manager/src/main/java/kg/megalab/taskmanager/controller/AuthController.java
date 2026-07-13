package kg.megalab.taskmanager.controller;

import jakarta.validation.Valid;
import kg.megalab.taskmanager.dto.auth.LoginRequest;
import kg.megalab.taskmanager.dto.auth.LoginResponse;
import kg.megalab.taskmanager.dto.auth.RefreshRequest;
import kg.megalab.taskmanager.dto.auth.TokenResponse;
import kg.megalab.taskmanager.dto.auth.UserSummaryResponse;
import kg.megalab.taskmanager.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    public UserSummaryResponse me() {
        return authService.me();
    }
}
