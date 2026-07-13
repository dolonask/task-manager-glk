package kg.megalab.taskmanager.service;

import kg.megalab.taskmanager.domain.Department;
import kg.megalab.taskmanager.domain.RefreshToken;
import kg.megalab.taskmanager.domain.User;
import kg.megalab.taskmanager.dto.auth.LoginRequest;
import kg.megalab.taskmanager.dto.auth.LoginResponse;
import kg.megalab.taskmanager.dto.auth.TokenResponse;
import kg.megalab.taskmanager.dto.auth.UserSummaryResponse;
import kg.megalab.taskmanager.exception.ApiException;
import kg.megalab.taskmanager.exception.ErrorCode;
import kg.megalab.taskmanager.exception.ForbiddenException;
import kg.megalab.taskmanager.repository.DepartmentRepository;
import kg.megalab.taskmanager.repository.RefreshTokenRepository;
import kg.megalab.taskmanager.repository.UserRepository;
import kg.megalab.taskmanager.security.JwtService;
import kg.megalab.taskmanager.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Login is stubbed against the local `User` table with a bcrypt password hash rather than
 * a real LDAP bind to the glk.kg domain controller (API spec §2) — see CLAUDE.md. The token
 * shape (JWT access token + server-side opaque refresh token) matches the spec exactly, so
 * swapping in real LDAP later only touches this class.
 */
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, DepartmentRepository departmentRepository,
                        RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLogin(request.username())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, "Неверный логин или пароль"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Неверный логин или пароль");
        }
        if (!user.isActive()) {
            throw new ForbiddenException("Учётная запись деактивирована");
        }

        Set<UUID> curatedIds = curatedDepartmentIds(user);
        String accessToken = jwtService.generateAccessToken(user, curatedIds);
        RefreshToken refreshToken = issueRefreshToken(user);

        return new LoginResponse(accessToken, refreshToken.getToken(), jwtService.getAccessTokenTtlSeconds(),
                toSummary(user, curatedIds));
    }

    public TokenResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, "Недействительный refresh-токен"));
        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Недействительный refresh-токен");
        }
        User user = refreshToken.getUser();
        Set<UUID> curatedIds = curatedDepartmentIds(user);
        String accessToken = jwtService.generateAccessToken(user, curatedIds);
        return new TokenResponse(accessToken, refreshToken.getToken(), jwtService.getAccessTokenTtlSeconds());
    }

    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse me() {
        UUID id = SecurityUtils.currentUser().id();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, "Требуется аутентификация"));
        return toSummary(user, curatedDepartmentIds(user));
    }

    private RefreshToken issueRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(jwtService.generateOpaqueRefreshToken());
        refreshToken.setExpiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenTtlSeconds()));
        return refreshTokenRepository.save(refreshToken);
    }

    private Set<UUID> curatedDepartmentIds(User user) {
        return departmentRepository.findByCurator(user).stream().map(Department::getId).collect(Collectors.toSet());
    }

    private UserSummaryResponse toSummary(User user, Set<UUID> curatedIds) {
        return new UserSummaryResponse(
                user.getId(), user.getFullName(), user.getLogin(), user.getRole().getValue(),
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                List.copyOf(curatedIds)
        );
    }
}
